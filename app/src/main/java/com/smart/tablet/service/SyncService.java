package com.smart.tablet.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.util.Log;
import android.widget.Toast;

import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.smart.tablet.Constants;
import com.smart.tablet.R;
import com.smart.tablet.entities.Arrival;
import com.smart.tablet.entities.Category;
import com.smart.tablet.entities.Device;
import com.smart.tablet.entities.Hotel;
import com.smart.tablet.entities.Media;
import com.smart.tablet.entities.Offer;
import com.smart.tablet.entities.Sale;
import com.smart.tablet.entities.Service;
import com.smart.tablet.entities.Setting;
import com.smart.tablet.entities.Testimonial;
import com.smart.tablet.helpers.Util;
import com.smart.tablet.listeners.AsyncResultBag;
import com.smart.tablet.tasks.DeleteCategories;
import com.smart.tablet.tasks.DeleteMedia;
import com.smart.tablet.tasks.DeleteServices;
import com.smart.tablet.tasks.RetrieveSetting;
import com.smart.tablet.tasks.StoreArrival;
import com.smart.tablet.tasks.StoreCategory;
import com.smart.tablet.tasks.StoreDevice;
import com.smart.tablet.tasks.StoreHotel;
import com.smart.tablet.tasks.StoreMedia;
import com.smart.tablet.tasks.StoreOffer;
import com.smart.tablet.tasks.StoreSale;
import com.smart.tablet.tasks.StoreService;
import com.smart.tablet.tasks.StoreSetting;
import com.smart.tablet.tasks.StoreTestimonial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.drakeet.support.toast.ToastCompat;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class SyncService extends IntentService {

    public static final String TRANSACTION_DONE = com.smart.tablet.service.SyncService.class.getName() + ":DONE";
    public static final String TRANSACTION_FAILED = com.smart.tablet.service.SyncService.class.getName() + ":FAILED";
    public static final String TRANSACTION_COMPLETE = com.smart.tablet.service.SyncService.class.getName() + ":COMPLETE";
    public static final String TRANSACTION_START = com.smart.tablet.service.SyncService.class.getName() + ":START";
    public static final String TRANSACTION_HEART_BEAT = com.smart.tablet.service.SyncService.class.getName() + ":HEART_BEAT";
    public static final String TRANSACTION_PROGRESS = com.smart.tablet.service.SyncService.class.getName() + ":PROGRESS";
    private static final String TAG = com.smart.tablet.service.SyncService.class.getName();
    private boolean isRunning = false;
    private String SYNC_DONE = Constants.SETTING_SYNC_DONE;
    private String TOKEN = Constants.TOKEN_KEY;
    private String FILE_PATH = Constants.FILE_PATH_KEY;
    private boolean _isSettingsStored;
    private boolean _isHotelInfoStored;
    private boolean _isCategoriesStored;
    private boolean _isServicesStored;
    private boolean _isFilesDownloaded;
    private boolean _isDeviceInfoStored;
    private boolean _isOffersStored;
    private boolean _isArrivalsStored;
    private boolean _isSalesStored;
    private boolean _imageProcessing;
    private boolean _isReset;
    private String _token;
    private int _extraFieldsLength;
    private int _indexesFilled;
    private boolean _hasError;
    private Object _error;
    private int _totalMedia = 0;
    private int _processedMedia = 0;

    public SyncService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "sync_service";
            String CHANNEL_NAME = "Synchronizing Service";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);


            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setCategory(Notification.CATEGORY_SERVICE).setPriority(PRIORITY_MIN).build();

            startForeground(101, notification);
        }
    }

    private void notifyFinish() {
        isRunning = false;

        new StoreSetting(this, new Setting(Constants.SYNC_SERVICE_RUNNING, "0"))
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        sendFinishBroadcast();
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        Log.e(TAG, "notifyFinish: " + error);
                        sendFinishBroadcast();
                    }
                })
                .execute();
    }

    private void notifyComplete() {
        isRunning = false;

        new StoreSetting(this, new Setting(Constants.SYNC_SERVICE_RUNNING, "0"))
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        sendCompleteBroadcast();
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        Log.e(TAG, "notifyComplete: " + error);
                        sendFinishBroadcast();
                    }
                })
                .execute();
    }

    private void sendFinishBroadcast() {
        Intent i = new Intent(TRANSACTION_DONE);
        sendBroadcast(i);
    }

    private void sendProgressBroadcast(int progress, ArrayList args) {
        Intent i = new Intent(TRANSACTION_PROGRESS);
        i.putExtra("progress", progress);
        i.putExtra("downloading", args);

        sendBroadcast(i);
    }

    private void sendFailedBroadcast() {
        Intent i = new Intent(TRANSACTION_FAILED);
        sendBroadcast(i);
    }

    private void sendCompleteBroadcast() {
        Intent i = new Intent(TRANSACTION_COMPLETE);
        sendBroadcast(i);
    }

    private void sendStartBroadcast() {
        Intent i = new Intent(TRANSACTION_START);
        sendBroadcast(i);
    }

    private void sendHeartBeat() {
        Log.d("SchedulingAlarms", "heart beat sent");
        Intent i = new Intent(TRANSACTION_HEART_BEAT);
        sendBroadcast(i);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        _hasError = false;
        _isServicesStored = _isCategoriesStored = _isSettingsStored = _isHotelInfoStored = _isFilesDownloaded = _isDeviceInfoStored = false;
        _isOffersStored = _isArrivalsStored = _isSalesStored = true;
        _extraFieldsLength = 1;
        _indexesFilled = 0;
        _isReset = false;

        long wait_before_seconds = 0;

        if (intent != null) {
            wait_before_seconds = intent.getLongExtra(getString(R.string.param_sync_wait), 0);
            _isReset = intent.getBooleanExtra(getString(R.string.param_sync_reset), false);
        }

        if (!isRunning) {

            isRunning = true;

            if (wait_before_seconds > 0) {
                try {
                    Thread.sleep(wait_before_seconds);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            new StoreSetting(this, new Setting(Constants.SYNC_SERVICE_RUNNING, "1"))
                    .onSuccess(new AsyncResultBag.Success() {
                        @Override
                        public void onSuccess(Object result) {
                            if (isRunning) {
                                retrieveTokenAndStartSync();
                                sendStartBroadcast();
                            } else {
                                Log.d(TAG, "destroyed at setting");
                                _hasError = true;
                            }
                        }
                    })
                    .onError(new AsyncResultBag.Error() {
                        @Override
                        public void onError(Object error) {
                            Log.e(TAG, "init: " + error);

                            _hasError = true;
                            _error = error;

                            decide();
                        }
                    })
                    .execute();

            while (isRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void retrieveTokenAndStartSync() {
        new RetrieveSetting(this, TOKEN)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        if (result != null) {
                            if (isRunning) {
                                _token = result.toString();
                                Log.d("SchedulingAlarms", "token: " + _token);
                                sync();
                            } else {
                                _hasError = true;
                            }
                        }
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        Log.e(TAG, "retrieveToken: " + error);

                        _hasError = true;
                        _error = error;

                        decide();
                    }
                })
                .execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isRunning = false;
        Log.d("SyncService", "destroyed");
        stopForeground(true);
    }

    private void sync() {
        String url = Constants.GetApiUrl("export", "v2");

        if (_isReset) {
            url += "?reset=1";
        }

        Log.d("SchedulingAlarms", "sync");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d("SchedulingAlarms", response + "");
                    if (response.getBoolean("status")) {
                        if (isRunning) {
                            startSync(response);
                        } else {
                            _hasError = true;
                            Log.d(TAG, "destroyed at sync()");
                        }
                    } else {
                        _hasError = true;
                        _error = response.get("message").toString();

                        decide();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "sync: " + e.getMessage());

                    _hasError = true;
                    _error = e;

                    decide();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "sync: " + error.getMessage());

                _hasError = true;
                _error = error;

                decide();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();

                params.put("AppKey", Constants.APP_KEY);
                params.put("Authorization", _token);

                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void startSync(JSONObject response) throws JSONException {
        Log.d("SchedulingAlarms", "sync started");

        JSONObject data = response.getJSONObject("data");
        JSONObject hotel_obj = data.getJSONObject("hotel");
        JSONObject device_obj = data.getJSONObject("device");

        Device device = new Device();

        device.setId(device_obj.getInt("id"));
        device.setDevice_id(device_obj.getString("device_id"));
        device.setHotel_id(device_obj.getInt("hotel_id"));
        device.setLanguage(device_obj.getString("language"));
        device.setLast_sync(device_obj.getString("last_sync"));
        device.setMac_address(device_obj.getString("mac_address"));
        device.setDevice_identity(device_obj.getString("device_identity"));
        device.setStatus(device_obj.getInt("status"));

        if (!device_obj.isNull("meta"))
            device.setMeta(device_obj.getJSONArray("meta").toString());

        storeDeviceInfo(device);

        Hotel hotel = new Hotel();

        hotel.setId(hotel_obj.getInt("id"));
        hotel.setCountry(hotel_obj.getString("country"));
        hotel.setName(hotel_obj.getString("name"));
        hotel.setTimezone(hotel_obj.getString("timezone"));

        if (!hotel_obj.isNull("group_id")) {
            hotel.setGroup_id(hotel_obj.getInt("group_id"));
        } else {
            hotel.setGroup_id(0);
        }

        storeHotelSettings(hotel_obj.getJSONArray("meta"));
        storeHotelInfo(hotel);

        final JSONArray categories_arr = data.getJSONArray("categories");
        final JSONArray services_arr = data.getJSONArray("services");
        final JSONArray media_arr = data.getJSONArray("objects");

        JSONArray deleted_categories = data.getJSONArray("deleted_categories");
        JSONArray deleted_services = data.getJSONArray("deleted_services");
        JSONArray deleted_objects = data.getJSONArray("deleted_objects");

        List<Integer> deleted_category_ids = new ArrayList<>();
        List<Integer> deleted_service_ids = new ArrayList<>();
        List<Integer> deleted_object_ids = new ArrayList<>();

        for (int i = 0; i < deleted_categories.length(); i++) {
            JSONObject deleted_category = deleted_categories.getJSONObject(i);
            deleted_category_ids.add(deleted_category.getInt("id"));
        }

        for (int i = 0; i < deleted_services.length(); i++) {
            JSONObject deleted_service = deleted_services.getJSONObject(i);
            deleted_service_ids.add(deleted_service.getInt("id"));
        }

        for (int i = 0; i < deleted_objects.length(); i++) {
            JSONObject deleted_object = deleted_objects.getJSONObject(i);
            deleted_object_ids.add(deleted_object.getInt("id"));
        }

        new DeleteCategories(this, deleted_category_ids)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        startStoringCategories(categories_arr);
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        startStoringCategories(categories_arr);
                    }
                })
                .execute();

        new DeleteServices(this, deleted_service_ids)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        startStoringServices(services_arr);
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        startStoringServices(services_arr);
                    }
                })
                .execute();

        new DeleteMedia(this, deleted_object_ids)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        if (isRunning) {
                            try {
                                storeMedias(media_arr);
                            } catch (JSONException e) {
                                Log.e(TAG, "deleteMedia: " + e.getMessage());

                                _hasError = true;
                                _error = e;

                                decide();
                            }
                        } else {
                            _hasError = true;
                            Log.d(TAG, "destroyed at delete media");
                        }
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        Log.e(TAG, "deleteMedia: " + error);
                    }
                })
                .execute();
    }

    private void startStoringCategories(JSONArray categories_arr) {
        try {
            storeCategories(categories_arr);
        } catch (Exception e) {
            Log.e(TAG, "sync: " + e.getMessage());

            _hasError = true;
            _error = e;

            decide();
        }
    }

    private void startStoringServices(JSONArray services_arr) {
        try {
            storeServices(services_arr);
        } catch (Exception e) {
            Log.e(TAG, "sync: " + e.getMessage());

            _hasError = true;
            _error = e;

            decide();
        }
    }

    private void storeDeviceInfo(Device device) {
        Log.d("Sync", "storing device info");

        new StoreDevice(this, device)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        if (isRunning) {
                            _isDeviceInfoStored = true;
                            decide();
                        } else {
                            _hasError = true;
                            Log.d(TAG, "destroyed at device info");
                        }
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        Log.e(TAG, "storeDeviceInfo: " + error);

                        _hasError = true;
                        _error = error;

                        decide();
                    }
                })
                .execute();
    }

    private void storeMedias(final JSONArray media_arr) throws JSONException {
        Log.d("Sync", "storing medias");

        Media[] medias = new Media[media_arr.length()];

        if (media_arr.length() > 0) {
            _totalMedia = media_arr.length();

            Log.d("MediaFile", "total medias: " + _totalMedia);

            for (int i = 0; i < _totalMedia; i++) {
                JSONObject m = media_arr.getJSONObject(i);

                Media media = new Media();
                media.setId(m.getInt("id"));
                media.setUrl(m.getString("url"));

                medias[i] = media;
            }
        }

        final Media[] medias_ = medias;

        Log.d("Sync", "medias: " + medias.length);
        if (medias.length > 0) {
            new StoreMedia(this, getFilePath(null), medias)
                    .onSuccess(new AsyncResultBag.Success() {
                        @Override
                        public void onSuccess(Object result) {
                            if (isRunning) {
                                _isFilesDownloaded = true;
                                decide();
                            } else {
                                _hasError = true;
                                Log.d(TAG, "destroyed at storing media");
                            }
                        }
                    })
                    .onError(new AsyncResultBag.Error() {
                        @Override
                        public void onError(Object error) {
                            Log.e(TAG, "storeMedia: " + error);

                            _hasError = true;
                            _error = error;

                            decide();
                        }
                    })
                    .onProgress(new StoreMedia.Progress() {
                        @Override
                        public void onProgress(Object result, Object args) {
                            ArrayList<Integer> downloaded = null;
                            ArrayList<String> downloading = new ArrayList<>();

                            if (args != null && args instanceof ArrayList) {
                                downloaded = (ArrayList) args;

                                for (int i = 0; i < medias_.length; i++) {
                                    if (!downloaded.contains(medias_[i].getId())) {
                                        downloading.add(medias_[i].getUrl());
                                    }
                                }
                            }

                            sendProgressBroadcast((Integer) result, downloading);
                        }
                    })
                    .execute();
        } else {
            _isFilesDownloaded = true;

            decide();
        }
    }

    private void storeCategories(JSONArray categories_arr) throws JSONException {
        Log.d("Sync", "storing categories");

        Category[] categories = new Category[categories_arr.length()];

        for (int i = 0; i < categories_arr.length(); i++) {
            JSONObject c = categories_arr.getJSONObject(i);

            Category category = new Category();
            category.setId(c.getInt("id"));
            category.setName(c.getString("name"));
            category.setDescription(c.getString("description"));
            category.setIs_marketing_partner((c.getInt("is_marketing_partner") == 1));

            if (!c.isNull("display_order"))
                category.setDisplay_order(c.getInt("display_order"));

            if (!c.isNull("meta"))
                category.setMeta(c.getJSONArray("meta").toString());

            if (!c.isNull("embed_url"))
                category.setEmbed_url(c.getString("embed_url"));

            if (!c.isNull("parent_id"))
                category.setParent_id(c.getInt("parent_id"));

            categories[i] = category;
        }

        new StoreCategory(this, categories)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        if (isRunning) {
                            _isCategoriesStored = true;
                            decide();
                        } else {
                            _hasError = true;
                            Log.d(TAG, "destroyed at storing categories");
                        }
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        Log.e(TAG, "storeCategory: " + error);

                        _hasError = true;
                        _error = error;

                        decide();
                    }
                })
                .execute();
    }

    private void storeServices(JSONArray services_arr) throws JSONException {
        Log.d("Sync", "storing services");

        Service[] services = new Service[services_arr.length()];

        for (int i = 0; i < services_arr.length(); i++) {
            JSONObject s = services_arr.getJSONObject(i);
            JSONArray offers = s.getJSONArray("offers");
            JSONArray arrivals = s.getJSONArray("new_arrivals");
            JSONArray sales = s.getJSONArray("sales");
            JSONArray testimonials = s.getJSONArray("testimonials");

            Service service = new Service();
            service.setId(s.getInt("id"));
            service.setTitle(s.getString("title"));
            service.setDescription(s.getString("description"));
            service.setCategory_id(s.getInt("category_id"));
            service.setStatus(s.getInt("status"));
            service.setIs_marketing_partner((s.getInt("is_marketing_partner") == 1));

            if (!s.isNull("hotel_id"))
                service.setHotel_id(s.getInt("hotel_id"));

            if (!s.isNull("meta"))
                service.setMeta(s.getJSONArray("meta").toString());

            if (offers.length() > 0) {
                storeOffers(offers);
            }

            if (arrivals.length() > 0) {
                storeArrivals(arrivals);
            }

            if (sales.length() > 0) {
                storeSales(sales);
            }

            if (testimonials.length() > 0) {
                storeTestimonials(testimonials);
            }

            services[i] = service;
        }

        new StoreService(this, services)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        if (isRunning) {
                            _isServicesStored = true;
                            decide();
                        } else {
                            _hasError = true;
                            Log.d(TAG, "destroyed at storing services");
                        }
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        Log.e(TAG, "storeService: " + error);

                        _hasError = true;
                        _error = error;

                        decide();
                    }
                })
                .execute();
    }

    private void storeTestimonials(JSONArray testimonials_arr) throws JSONException {
        Log.d("Sync", "storing testimonials");

        Testimonial[] testimonials = new Testimonial[testimonials_arr.length()];

        for (int i = 0; i < testimonials_arr.length(); i++) {
            JSONObject o = testimonials_arr.getJSONObject(i);

            Testimonial testimonial = new Testimonial();

            testimonial.setId(o.getInt("id"));
            testimonial.setCite(o.getString("cite"));
            testimonial.setContent(o.getString("content"));
            testimonial.setService_id(o.getInt("service_id"));

            testimonials[i] = testimonial;
        }

        if (testimonials.length > 0) {
            new StoreTestimonial(this, testimonials).execute();
        }
    }

    private void storeOffers(JSONArray offers_arr) throws JSONException {
        Log.d("Sync", "storing offers");

        Offer[] offers = new Offer[offers_arr.length()];

        for (int i = 0; i < offers_arr.length(); i++) {
            JSONObject o = offers_arr.getJSONObject(i);

            Offer offer = new Offer();

            offer.setId(o.getInt("id"));
            offer.setTitle(o.getString("title"));
            offer.setDescription(o.getString("description"));
            offer.setMedia_id(o.getInt("media_id"));
            offer.setService_id(o.getInt("service_id"));

            offers[i] = offer;
        }

        if (offers.length > 0) {
            new StoreOffer(this, offers).execute();
        }
    }

    private void storeArrivals(JSONArray arrivals_arr) throws JSONException {
        Log.d("Sync", "storing arrivals");

        Arrival[] arrivals = new Arrival[arrivals_arr.length()];

        for (int i = 0; i < arrivals_arr.length(); i++) {
            JSONObject o = arrivals_arr.getJSONObject(i);

            Arrival arrival = new Arrival();

            arrival.setId(o.getInt("id"));
            arrival.setTitle(o.getString("title"));
            arrival.setDescription(o.getString("description"));
            arrival.setMedia_id(o.getInt("media_id"));
            arrival.setService_id(o.getInt("service_id"));

            arrivals[i] = arrival;
        }

        if (arrivals.length > 0) {
            new StoreArrival(this, arrivals).execute();
        }
    }

    private void storeSales(JSONArray sales_arr) throws JSONException {
        Log.d("Sync", "storing sales");

        Sale[] sales = new Sale[sales_arr.length()];

        for (int i = 0; i < sales_arr.length(); i++) {
            JSONObject o = sales_arr.getJSONObject(i);

            Sale sale = new Sale();

            sale.setId(o.getInt("id"));
            sale.setTitle(o.getString("title"));
            sale.setDescription(o.getString("description"));
            sale.setMedia_id(o.getInt("media_id"));
            sale.setService_id(o.getInt("service_id"));

            sales[i] = sale;
        }

        if (sales.length > 0) {
            new StoreSale(this, sales).execute();
        }
    }

    private void storeHotelSettings(JSONArray meta) throws JSONException {
        Log.d("Sync", "storing hotel settings");

        int length = meta.length() + _extraFieldsLength;
        Setting[] settings = new Setting[length];

        for (int i = 0; i < meta.length(); i++) {
            JSONObject m = meta.getJSONObject(i);

            Setting setting = new Setting();
            setting.setName(m.getString("meta_key"));
            setting.setValue(m.getString("meta_value"));

            settings[i] = setting;

            _indexesFilled++;
        }

        settings[_indexesFilled++] = new Setting(FILE_PATH, getFilePath(null));

        new StoreSetting(this, settings)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        if (isRunning) {
                            _isSettingsStored = true;
                            decide();
                        } else {
                            _hasError = true;
                            Log.d(TAG, "destroyed at hotel settings");
                        }
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        Log.e(TAG, "storeSetting: " + error);

                        _hasError = true;
                        _error = error;

                        decide();
                    }
                })
                .execute();
    }

    private void storeHotelInfo(Hotel hotel) {
        Log.d("Sync", "storing hotel info");

        new StoreHotel(this, hotel)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        if (isRunning) {
                            _isHotelInfoStored = true;
                            decide();
                        } else {
                            _hasError = true;
                            Log.d(TAG, "destroyed at storing hotel");
                        }
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        Log.e(TAG, "storeHotelInfo: " + error);

                        _hasError = true;
                        _error = error;

                        decide();
                    }
                })
                .execute();
    }

    private void decide() {
        Boolean isDone = _isSettingsStored && _isHotelInfoStored && _isCategoriesStored && _isServicesStored && _isFilesDownloaded && _isDeviceInfoStored;
        Log.d("SchedulingAlarms", "all done: " + (isDone ? "yes" : "no"));

        if (isDone) {
            String url = Constants.GetApiUrl("exported", "v2");
            final StoreSetting storeSetting = new StoreSetting(this, new Setting(SYNC_DONE, "1"))
                    .onSuccess(new AsyncResultBag.Success() {
                        @Override
                        public void onSuccess(Object result) {
                            notifyComplete();
                        }
                    })
                    .onError(new AsyncResultBag.Error() {
                        @Override
                        public void onError(Object error) {
                            _hasError = true;
                            _error = error;

                            decide();
                        }
                    });

            String version_name = Util.getVersionName(this);
            JSONObject jsonRequest = new JSONObject();

            try {
                jsonRequest.put("app_version", version_name);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonRequest, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    storeSetting.execute();
                }
            }, null) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> params = new HashMap<String, String>();

                    params.put("AppKey", Constants.APP_KEY);
                    params.put("Authorization", _token);

                    return params;
                }
            };

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);

        } else if (_hasError) {
            _error = "Sync failed";

            if (_error instanceof ConnectException || _error instanceof UnknownHostException || _error instanceof NetworkError) {
                _error = "Connection failed, please try again later";
            }

            notifyFinish();
            showMessage(_error + "");
        }
    }

    private void showMessage(String message) {
        if (isRunning) {
            Log.d("SYNCMESSAGE", message);
            try {
                ToastCompat.makeText(this, message, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getFilePath(String filename) {
        String path = getFilesDir().getPath();

        if (filename == null)
            return path;

        return path + File.separator + filename;
    }
}

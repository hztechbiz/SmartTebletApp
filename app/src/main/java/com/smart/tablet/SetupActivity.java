package com.smart.tablet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.smart.tablet.entities.Setting;
import com.smart.tablet.helpers.Util;
import com.smart.tablet.listeners.AsyncResultBag;
import com.smart.tablet.service.SyncService;
import com.smart.tablet.tasks.RetrieveSetting;
import com.smart.tablet.tasks.StoreSetting;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SetupActivity extends Activity {

    public static final String TAG = SetupActivity.class.getName();
    private ProgressDialog _progressDialog;
    private Button _btn_sync;
    private String API_KEY = Constants.API_KEY;
    private String TOKEN = Constants.TOKEN_KEY;
    private String _token;
    private RelativeLayout _sync_container;
    private EditText _txt_device_identity;
    private TextView _txt_progress, _txt_sync_debug;
    private BroadcastReceiver syncStartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showSynchronizing(true);
        }
    };
    private BroadcastReceiver syncHeartBeatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showSynchronizing(true);
        }
    };
    private BroadcastReceiver syncProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("SchedulingAlarms", "syncProgressReceiver");
            _txt_progress.setText(intent.getIntExtra("progress", 0) + "% Downloaded. Please wait...");

            ArrayList<String> downloading = intent.getStringArrayListExtra("downloading");
            String debug = "";

            Log.d("FilesList", downloading + "!");
        }
    };
    private BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showSynchronizing(false);
        }
    };
    private BroadcastReceiver syncFailedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showSynchronizing(false);
        }
    };
    private BroadcastReceiver syncCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switchScreen(MainActivity.class);
        }
    };

    private void switchScreen(Class activityClass) {
        Intent intent = new Intent(com.smart.tablet.SetupActivity.this, activityClass);
        startActivity(intent);
        finish();

        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
    }

    private void showSynchronizing(boolean b) {
        _sync_container.setVisibility(b ? View.VISIBLE : View.GONE);
        _txt_progress.setText(getString(R.string.synchronizing_please_wait));

        _btn_sync.setEnabled(!b);
    }

    @Override
    protected void onStart() {
        registerReceiver(syncReceiver, new IntentFilter(SyncService.TRANSACTION_DONE));
        registerReceiver(syncFailedReceiver, new IntentFilter(SyncService.TRANSACTION_FAILED));
        registerReceiver(syncCompleteReceiver, new IntentFilter(SyncService.TRANSACTION_COMPLETE));
        registerReceiver(syncStartReceiver, new IntentFilter(SyncService.TRANSACTION_START));
        registerReceiver(syncHeartBeatReceiver, new IntentFilter(SyncService.TRANSACTION_HEART_BEAT));
        registerReceiver(syncProgressReceiver, new IntentFilter(SyncService.TRANSACTION_PROGRESS));

        //fetchDeviceToken();

        try {
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(task -> {

                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        _token = task.getResult().getToken();
                        Log.d(TAG, "Token:" + _token);
                    });

        } catch (Exception e) {
            _token = null;
            Log.e(TAG, "Token not found!! " + e.getMessage());
        }

        super.onStart();
    }

    private void fetchDeviceToken() {
        new RetrieveSetting(this, Constants.DEVICE_ID)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        if (result != null) {
                            _token = result.toString();
                        }
                    }
                })
                .execute();
    }

    private void checkIfSyncServiceRunning() {
        new RetrieveSetting(this, Constants.SYNC_SERVICE_RUNNING)
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        showSynchronizing((result != null && result.equals("1")));
                    }
                })
                .execute();
    }

    @Override
    protected void onStop() {
        if (syncReceiver != null)
            unregisterReceiver(syncReceiver);

        if (syncFailedReceiver != null)
            unregisterReceiver(syncFailedReceiver);

        if (syncProgressReceiver != null)
            unregisterReceiver(syncProgressReceiver);

        if (syncStartReceiver != null)
            unregisterReceiver(syncStartReceiver);

        if (syncHeartBeatReceiver != null)
            unregisterReceiver(syncHeartBeatReceiver);

        if (syncCompleteReceiver != null)
            unregisterReceiver(syncCompleteReceiver);

        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_setup);

        _progressDialog = new ProgressDialog(this);
        _sync_container = findViewById(R.id.syncContainer);
        _btn_sync = findViewById(R.id.btn_sync);
        _txt_device_identity = findViewById(R.id.txt_device_identity);
        _txt_progress = findViewById(R.id.syncProgressText);

        _txt_device_identity.setText(getMacAddress());
        _txt_progress.setTypeface(Util.getTypeFace(this));

        _btn_sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String key = ((EditText) findViewById(R.id.txt_key)).getText().toString();
                final String device_idn = _txt_device_identity.getText() + "";

                if (key.isEmpty()) {
                    showMessage("Please enter API key provided by the Hotel Manager");
                } else if (device_idn.isEmpty()) {
                    showMessage("Please enter device identity");
                } else {
                    showProgressDialog("Please wait...");

                    String url = Constants.GetApiUrl("auth");
                    JSONObject jsonRequest = new JSONObject();

                    Log.d("DeviceToken", _token + "");

                    try {
                        jsonRequest.put("udid", _token);
                        jsonRequest.put("api_key", key);
                        jsonRequest.put("device_identity", device_idn);
                        jsonRequest.put("mac_address", getMacAddress());
                    } catch (Exception ex) {
                        showMessage(ex.getMessage());
                    }

                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonRequest, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.getBoolean("status")) {
                                    String token = response.getJSONObject("data").getString("token");
                                    storeSettings(new Setting(API_KEY, key), new Setting(TOKEN, token));

                                    _btn_sync.setEnabled(false);
                                } else {
                                    hideProgressDialog();
                                    showMessage(response.getString("message"));
                                }
                            } catch (JSONException ex) {
                                showMessage(ex.getMessage());
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            showMessage("Failed to setup: " + error);
                        }
                    }) {
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> params = new HashMap<String, String>();

                            params.put("AppKey", Constants.APP_KEY);

                            return params;
                        }
                    };

                    AppController.getInstance().addToRequestQueue(request);
                }
            }
        });
    }

    private String getMacAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            WifiInfo wInfo = wifiManager.getConnectionInfo();
            return wInfo != null ? wInfo.getMacAddress() : null;
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }

    /*
     * Method to store API_KEY in database
     */
    private void storeSettings(Setting... settings) {
        new StoreSetting(this, settings)
                .beforeExecuting(new AsyncResultBag.Before() {
                    @Override
                    public void beforeExecuting() {
                        //showProgressDialog("Please wait...");
                    }
                })
                .onError(new AsyncResultBag.Error() {
                    @Override
                    public void onError(Object error) {
                        Exception e = (Exception) error;

                        hideProgressDialog();
                        showMessage(e.getMessage());
                    }
                })
                .onSuccess(new AsyncResultBag.Success() {
                    @Override
                    public void onSuccess(Object result) {
                        hideProgressDialog();

                        /*
                        Intent syncActivityIntent = new Intent(SetupActivity.this, SyncActivity.class);
                        startActivity(syncActivityIntent);
                        finish();

                        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
                        */
                        Intent intent = new Intent(SetupActivity.this, SyncService.class);
                        intent.putExtra(getString(R.string.param_sync_reset), true);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent);
                        } else {
                            startService(intent);
                        }
                    }
                })
                .execute();
    }

    private void showProgressDialog(String message) {
        _progressDialog.setMessage(message);

        try {
            _progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideProgressDialog() {
        _progressDialog.dismiss();
    }

    private void showMessage(String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setPositiveButton("Ok", null);

        try {
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

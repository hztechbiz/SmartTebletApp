package com.smart.tablet.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.smart.tablet.Constants;
import com.smart.tablet.entities.Setting;
import com.smart.tablet.helpers.Util;
import com.smart.tablet.listeners.AsyncResultBag;
import com.smart.tablet.tasks.RetrieveSetting;
import com.smart.tablet.tasks.StoreSetting;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BatteryLevelReceiver extends BroadcastReceiver {
    private String TAG = com.smart.tablet.receivers.BatteryLevelReceiver.class.getName();
    private Context _context;
    private String _token;

    @Override
    public void onReceive(Context context, Intent intent) {
        _context = context;

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        //int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        //final float percentage = level / (float) scale;
        final float percentage = level;

        new StoreSetting(context, new Setting("battery_percentage", String.valueOf(percentage)))
                .execute();

        new RetrieveSetting(context, Constants.TOKEN_KEY)
                .onSuccess(result -> {
                    try {
                        if (result != null) {
                            _token = result.toString();
                            sendStatusToServer(percentage);
                        } else {
                            Log.e(TAG, "Token not found");
                        }
                    } catch (Exception ex) {
                        Log.d(TAG, ex.getMessage());
                    }
                })
                .onError(error -> Log.e(TAG, ((Exception) error).getMessage()))
                .execute();
    }

    private void sendStatusToServer(float percentage) throws JSONException {
        String url = Constants.GetApiUrl("device/update");
        JSONObject jsonRequest = new JSONObject();
        String version_name = Util.getVersionName(_context);

        jsonRequest.put("battery_percentage", percentage);
        jsonRequest.put("app_version", version_name);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonRequest, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.getMessage() + "");
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

        RequestQueue queue = Volley.newRequestQueue(_context);
        queue.add(request);
    }
}

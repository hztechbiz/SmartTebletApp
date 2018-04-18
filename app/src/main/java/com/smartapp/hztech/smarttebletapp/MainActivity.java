package com.smartapp.hztech.smarttebletapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.smartapp.hztech.smarttebletapp.fragments.CategoryFragment;
import com.smartapp.hztech.smarttebletapp.fragments.MainFragment;
import com.smartapp.hztech.smarttebletapp.fragments.ServiceFragment;
import com.smartapp.hztech.smarttebletapp.listeners.AsyncResultBag;
import com.smartapp.hztech.smarttebletapp.listeners.FragmentListener;
import com.smartapp.hztech.smarttebletapp.tasks.RetrieveSetting;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity {

    private String TAG = this.getClass().getName();
    private FrameLayout fragmentContainer;
    private FragmentListener fragmentListener = new FragmentListener() {
        @Override
        public void onUpdateFragment(Fragment newFragment) {
            Log.d("FragmentUpdated", "From: MainActivity, Fragment: " + newFragment.getClass().getName());
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(fragmentContainer.getId(), newFragment);
            transaction.addToBackStack(null);

            transaction.commit();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        fragmentContainer = findViewById(R.id.fragment_container);

        if (fragmentContainer != null) {

            if (savedInstanceState != null) {
                return;
            }

            MainFragment firstFragment = new MainFragment();
            firstFragment.setFragmentListener(fragmentListener);

            getSupportFragmentManager().beginTransaction()
                    .add(fragmentContainer.getId(), firstFragment).commit();
        }

        setupMenuItems();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void setupMenuItems() {
        new RetrieveSetting(this,
                "enable_operating_the_television",
                "enable_connect_to_wifi",
                "enable_how_use_tablet",
                "enable_useful_information_category",
                "enable_weather",
                "enable_news",
                "operating_the_television_service",
                "connect_to_wifi_service",
                "how_use_tablet_category",
                "useful_information_category"
        ).onSuccess(new AsyncResultBag.Success() {
            @Override
            public void onSuccess(Object result) {
                HashMap<String, String> values = result != null ? (HashMap<String, String>) result : null;

                if (values != null) {
                    String enable_operating_the_television = values.containsKey("enable_operating_the_television") ? values.get("enable_operating_the_television") : "1";
                    String enable_connect_to_wifi = values.containsKey("enable_connect_to_wifi") ? values.get("enable_connect_to_wifi") : "1";
                    String enable_how_use_tablet = values.containsKey("enable_how_use_tablet") ? values.get("enable_how_use_tablet") : "1";
                    String enable_useful_information_category = values.containsKey("enable_useful_information_category") ? values.get("enable_useful_information_category") : "1";
                    String enable_weather = values.containsKey("enable_weather")
                            ? values.get("enable_weather") : "1";
                    String enable_news = values.containsKey("enable_news") ? values.get("enable_news") : "1";
                    String operating_the_television_service = values.containsKey("operating_the_television_service") ? values.get("operating_the_television_service") : "0";
                    String connect_to_wifi_service = values.containsKey("connect_to_wifi_service") ? values.get("connect_to_wifi_service") : "0";
                    String how_use_tablet_category = values.containsKey("how_use_tablet_category") ? values.get("how_use_tablet_category") : "0";
                    String useful_information_category = values.containsKey("useful_information_category") ? values.get("useful_information_category") : "0";

                    LinearLayout ott_linear = findViewById(R.id.ott);
                    LinearLayout wifi_linear = findViewById(R.id.itemWifi);
                    LinearLayout howTo_linear = findViewById(R.id.itemHow);
                    LinearLayout Info_linear = findViewById(R.id.itemInfo);
                    //LinearLayout map_linear = findViewById(R.id.itemMap);
                    //LinearLayout localReg_linear = findViewById(R.id.itemLocalRegion);
                    LinearLayout weather_linear = findViewById(R.id.itemWeather);
                    LinearLayout news_linear = findViewById(R.id.itemNews);

                    if (enable_operating_the_television.equals("1")) {
                        ott_linear.setVisibility(View.VISIBLE);
                        ott_linear.setTag(R.string.tag_value, operating_the_television_service);
                        ott_linear.setTag(R.string.tag_action, R.string.tag_action_service);
                    } else {
                        ott_linear.setVisibility(View.INVISIBLE);
                    }

                    if (enable_connect_to_wifi.equals("1")) {
                        wifi_linear.setVisibility(View.VISIBLE);
                        wifi_linear.setTag(R.string.tag_value, connect_to_wifi_service);
                        wifi_linear.setTag(R.string.tag_action, R.string.tag_action_service);
                    } else {
                        wifi_linear.setVisibility(View.INVISIBLE);
                    }

                    if (enable_how_use_tablet.equals("1")) {
                        howTo_linear.setVisibility(View.VISIBLE);
                        howTo_linear.setTag(R.string.tag_value, how_use_tablet_category);
                        howTo_linear.setTag(R.string.tag_action, R.string.tag_action_category);
                    } else {
                        howTo_linear.setVisibility(View.INVISIBLE);
                    }

                    if (enable_useful_information_category.equals("1")) {
                        Info_linear.setVisibility(View.VISIBLE);
                        Info_linear.setTag(R.string.tag_value, useful_information_category);
                        Info_linear.setTag(R.string.tag_action, R.string.tag_action_category);
                    } else {
                        Info_linear.setVisibility(View.INVISIBLE);
                    }

                    if (enable_weather.equals("1")) {
                        weather_linear.setVisibility(View.VISIBLE);
                    } else {
                        weather_linear.setVisibility(View.INVISIBLE);
                    }

                    if (enable_news.equals("1")) {
                        news_linear.setVisibility(View.VISIBLE);
                    } else {
                        news_linear.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }).execute();
    }

    public void onNavItemClick(View view) {
        if (view.getId() != R.id.itemHome)
            makeMenuItemActive(view);

        Object action = view.getTag(R.string.tag_action);
        Object value = view.getTag(R.string.tag_value);

        Bundle bundle = new Bundle();
        MainFragment mainFragment = new MainFragment();
        mainFragment.setFragmentListener(fragmentListener);

        if (action != null && value != null) {
            if (action.equals(R.string.tag_action_category)) {
                bundle.putInt(getString(R.string.param_category_id), Integer.parseInt(value.toString()));

                CategoryFragment fragment = new CategoryFragment();
                fragment.setFragmentListener(fragmentListener);
                fragment.setArguments(bundle);

                mainFragment.setChildFragment(fragment);

            } else if (action.equals(R.string.tag_action_service)) {
                bundle.putInt(getString(R.string.param_service_id), Integer.parseInt(value.toString()));

                ServiceFragment fragment = new ServiceFragment();
                fragment.setArguments(bundle);

                mainFragment.setChildFragment(fragment);
            }
        }

        fragmentListener.onUpdateFragment(mainFragment);
    }

    public void makeMenuItemActive(View view) {
        LinearLayout ott_linear = findViewById(R.id.ott);
        LinearLayout wifi_linear = findViewById(R.id.itemWifi);
        LinearLayout howTo_linear = findViewById(R.id.itemHow);
        LinearLayout Info_linear = findViewById(R.id.itemInfo);
        LinearLayout map_linear = findViewById(R.id.itemMap);
        LinearLayout localReg_linear = findViewById(R.id.itemLocalRegion);
        LinearLayout weather_linear = findViewById(R.id.itemWeather);
        LinearLayout news_linear = findViewById(R.id.itemNews);

        LinearLayout[] all_items = new LinearLayout[]{ott_linear, wifi_linear, howTo_linear, Info_linear, map_linear, localReg_linear,
                weather_linear, news_linear};

        for (LinearLayout all_item : all_items) {
            all_item.setBackgroundColor(0);
        }

        view.setBackgroundColor(Color.parseColor("#2cb3dc"));
        // view.setBackground(R.drawable.sidemenu_gradient_bg);
    }
}

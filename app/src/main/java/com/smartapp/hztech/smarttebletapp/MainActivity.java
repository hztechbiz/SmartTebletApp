package com.smartapp.hztech.smarttebletapp;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.smartapp.hztech.smarttebletapp.fragments.CategoryFragment;
import com.smartapp.hztech.smarttebletapp.fragments.HomeFragment;
import com.smartapp.hztech.smarttebletapp.fragments.ServicesFragment;
import com.smartapp.hztech.smarttebletapp.listeners.AsyncResultBag;
import com.smartapp.hztech.smarttebletapp.listeners.FragmentListener;
import com.smartapp.hztech.smarttebletapp.tasks.RetrieveSetting;

public class MainActivity extends FragmentActivity implements FragmentListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        if (findViewById(R.id.fragment_container) != null) {

            if (savedInstanceState != null) {
                return;
            }

            HomeFragment firstFragment = new HomeFragment();

            firstFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment).commit();
        }

        setupMenuItems();
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
                String[] values = result != null ? (String[]) result : null;

                if (values != null) {
                    String enable_operating_the_television = values[0];
                    String enable_connect_to_wifi = values[1];
                    String enable_how_use_tablet = values[2];
                    String enable_useful_information_category = values[3];
                    String enable_weather = values[4];
                    String enable_news = values[5];
                    String operating_the_television_service = values[6];
                    String connect_to_wifi_service = values[7];
                    String how_use_tablet_category = values[8];
                    String useful_information_category = values[9];

                    LinearLayout ott_linear = findViewById(R.id.ott);
                    LinearLayout wifi_linear = findViewById(R.id.itemWifi);
                    LinearLayout howTo_linear = findViewById(R.id.itemHow);
                    LinearLayout Info_linear = findViewById(R.id.itemInfo);
                    LinearLayout map_linear = findViewById(R.id.itemMap);
                    LinearLayout localReg_linear = findViewById(R.id.itemLocalRegion);
                    LinearLayout weather_linear = findViewById(R.id.itemWeather);
                    LinearLayout news_linear = findViewById(R.id.itemNews);

                    if (enable_operating_the_television.equals("1")) {
                        ott_linear.setVisibility(View.VISIBLE);
                    } else {
                        ott_linear.setVisibility(View.INVISIBLE);
                    }
                    ///////////////////
                    if (enable_connect_to_wifi.equals("1")) {
                        wifi_linear.setVisibility(View.VISIBLE);
                    } else {
                        wifi_linear.setVisibility(View.INVISIBLE);
                    }
                    ///////////////////
                    if (enable_how_use_tablet.equals("1")) {
                        howTo_linear.setVisibility(View.VISIBLE);
                    } else {
                        howTo_linear.setVisibility(View.INVISIBLE);
                    }
                    ///////////////////
                    if (enable_useful_information_category.equals("1")) {
                        Info_linear.setVisibility(View.VISIBLE);
                    } else {
                        Info_linear.setVisibility(View.INVISIBLE);
                    }
                    ///////////////////
                    if (enable_weather.equals("1")) {
                        weather_linear.setVisibility(View.VISIBLE);
                    } else {
                        weather_linear.setVisibility(View.INVISIBLE);
                    }
                    ///////////////////
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

        switch (view.getId()) {
            case R.id.itemHome:
                ServicesFragment fragment = new ServicesFragment();
                updateFragment(fragment);
                break;
            case R.id.itemHow:
                CategoryFragment categoryFragment = new CategoryFragment();
                updateFragment(categoryFragment);
                break;
        }
    }

    public void updateFragment(Fragment newFragment) {

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);

        transaction.commit();
    }

    @Override
    public void onUpdateFragment(Fragment fragment) {
        updateFragment(fragment);
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

        for (int i = 0; i < all_items.length; i++) {
            all_items[i].setBackgroundColor(0);
        }

        view.setBackgroundColor(Color.parseColor("#2cb3dc"));
       // view.setBackground(R.drawable.sidemenu_gradient_bg);
    }
}

package com.smartapp.hztech.smarttebletapp;
import android.app.Activity;
import android.content.Intent;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private static int SPLASH_TIME_OUT = 4000;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        // this is testing 2

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run()
            {

                    Intent HomeIntent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(HomeIntent);
                    finish();
            }
        },SPLASH_TIME_OUT);
    }
}

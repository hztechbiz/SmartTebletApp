package com.smart.tablet.helpers;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.smart.tablet.MainActivity;
import com.smart.tablet.SplashActivity;
import com.smart.tablet.receivers.AdminReceiver;

import me.drakeet.support.toast.ToastCompat;

public class Common {

    public static void showToast(Context context, String text) {
        try {
            ToastCompat.makeText(context, text, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getHomeActivity(Context c) {
        PackageManager pm = c.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        ComponentName cn = intent.resolveActivity(pm);

        if (cn != null)
            return cn.flattenToShortString();
        else
            return "none";
    }

    public static void becomeHomeActivity(Context c) {
        ComponentName deviceAdmin = new ComponentName(c, AdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (!dpm.isAdminActive(deviceAdmin)) {
            showToast(c, "This app is not a device admin!");
            return;
        }
        if (!dpm.isDeviceOwnerApp(c.getPackageName())) {
            showToast(c, "This app is not the device owner!");
            return;
        }

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);

        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addCategory(Intent.CATEGORY_HOME);

        ComponentName activity = new ComponentName(c, SplashActivity.class);
        dpm.addPersistentPreferredActivity(deviceAdmin, intentFilter, activity);

        showToast(c, "Home activity: " + getHomeActivity(c));
    }
}

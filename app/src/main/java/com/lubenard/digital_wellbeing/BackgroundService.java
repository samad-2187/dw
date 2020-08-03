package com.lubenard.digital_wellbeing;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.IntentService;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BackgroundService extends IntentService {

    private void getLaunchedApps()
    {
        /*ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        List< ActivityManager.RunningTaskInfo > runningTaskInfo = manager.getRunningTasks(1);

        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        Log.d("BgService", "This app is running " + componentInfo.getPackageName());*/

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            String string_date = "01-August-2020 00:00:00";

            long milliseconds = 0;
            SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            try {
                Date d = f.parse(string_date);
                milliseconds = d.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Log.d("BgService", "I am going there");
           UsageStatsManager manager = (UsageStatsManager) getApplicationContext().getSystemService(USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                    time - 1000 * 1000, time);
            Log.d("BgService", "I am going there 2 " + appList.size());
            if (appList != null && appList.size() > 0) {
                Log.d("BgService", "I am going there 3");
                for (UsageStats usageStats : appList) {
                    Log.d("BgService", "for " +  usageStats.getPackageName() + "timeSeconds = " + usageStats.getTotalTimeInForeground());
                }
            }

            /*Map<String, UsageStats> usageStatsMap = manager.queryAndAggregateUsageStats(milliseconds, System.currentTimeMillis());
            HashMap<String, Double> usageMap = new HashMap<>();

            Log.d("BgService", "I am going there");
            for (String packageName : usageStatsMap.keySet()) {
                UsageStats us = usageStatsMap.get(packageName);
                Log.d("BgService", "I am looping ?");
                try {
                    long timeMs = us.getTotalTimeInForeground();
                    Double timeSeconds = new Double(timeMs / 1000);
                    usageMap.put(packageName, timeSeconds);
                    Log.d("BgService", "for " +  packageName + "timeSeconds = " + timeSeconds);
                } catch (Exception e) {
                    Log.d("BgService", "Getting timeInForeground resulted in an exception");
                }
            }*/
        }

        Parcelable.Creator<UsageStats> CREATOR;

        /*ActivityManager am = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfo = am.getRunningAppProcesses();

        for (int i = 0; i < runningAppProcessInfo.size(); i++) {
            Log.d("BgService", "This app is running " + runningAppProcessInfo.get(i).processName);
            //app_data_array.add(new App_data(runningAppProcessInfo.get(i).processName));
            if(runningAppProcessInfo.get(i).processName.equals("com.the.app.you.are.looking.for") {
                Log.d("");
            }
        }*/
    }

    public BackgroundService() {
        super("Launch");
    }

    private void sendDataToMainUi(Intent intent)
    {
        Bundle bundle = intent.getExtras();
        Bundle dataReturn = intent.getExtras();
        dataReturn.putInt("updateScreenTime", 1);
        if (bundle != null) {
            Messenger messenger = (Messenger) bundle.get("updateScreenTime");
            Message msg = Message.obtain();
            msg.setData(dataReturn);
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                Log.i("error", "error");
            }
        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        short mTimer = 0;

        String dataString = intent.getDataString();
        Log.d("BgService", "Background service has been started");

        // Launch Broadcast Receiver for screen time
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);

        // Main loop. This loop register if the screen is on which apps are launched.
        while (true)
        {
            Log.d("BgService", "Service is up and running, screen is " + ScreenReceiver.wasScreenOn + " mTimer vaut " + mTimer);
            try {
                if (mTimer == 60) {
                    sendDataToMainUi(intent);
                    mTimer = 0;
                } else if (ScreenReceiver.wasScreenOn)
                {
                    getLaunchedApps();
                    mTimer++;
                }
                // This delay is just here to help synchronising.
                // TODO: Look to remove it ? I will more accurate.
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

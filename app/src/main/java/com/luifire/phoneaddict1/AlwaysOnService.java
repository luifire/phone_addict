package com.luifire.phoneaddict1;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by LuiFire on 27.10.2017.
 */

public class AlwaysOnService extends Service {
    public static final String START_SERVICE_MAIN_ACTIVITY = "com.luifire.phoneaddict1.startServiceMainAct";
    public final int FOREGROUND_NOTIFICATION = 19901005;

    private static final String TAG = MainActivity.TAG + "AlwaysOnService\t";
    private OnOffReceiver mOnOffReceiver = null;

    private void startReceiver() {
        if (mOnOffReceiver == null) {
            // makes sure, that OnOff is not attached to main thread, mandatory
            HandlerThread handlerThread = new HandlerThread("OnOffReceiverThread");
            handlerThread.start();
            Looper looper = handlerThread.getLooper();
            Handler handler = new Handler(looper);

            IntentFilter screenOnOff = new IntentFilter();
            screenOnOff.addAction(Intent.ACTION_SCREEN_OFF);
            screenOnOff.addAction(Intent.ACTION_SCREEN_ON);

            mOnOffReceiver = new OnOffReceiver();

            registerReceiver(mOnOffReceiver, screenOnOff, null, handler);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Database(this).logd(TAG, "service started");

        // set Foreground
        startForeground(FOREGROUND_NOTIFICATION, getCompatNotification());
        startReceiver();

        return START_STICKY;
    }

    /**
     * Notification needs to be shown, to notify user
     *
     * @return
     */
    private Notification getCompatNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "PhoneAddict Running");

        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("PhoneAddict")
                .setTicker("PhoneAddict running")
                .setWhen(System.currentTimeMillis());

        // on click start main activity
        Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 12345678, startIntent, 0);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build ();

        return notification;
    }

    /**
     *
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        new Database(this).logd(TAG, "service destroyed");

        unregisterReceiver(mOnOffReceiver);
        mOnOffReceiver = null;

        // tryRestartApp();
        tryRestartApp(this, TAG);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        new Database(this).logd(TAG, "TaskRemoved");
        //tryRestartApp();
    }


    /**
     *
     */
    public static void tryRestartApp(Context context, String tag) {
        new Database(context).logd(tag, "restart intent");

        // send
        Intent intent = new Intent(context, AlwaysOnService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 5101990, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // restart app after 2^i, i+=2 Time Steps
        for (int i = 5; i < 14; i += 2) {
            long secAfterRestart = (long) Math.pow(2, i);

            long millSec = System.currentTimeMillis() + secAfterRestart * 1000;
            alarmManager.set(AlarmManager.RTC_WAKEUP, millSec, pendingIntent);
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

package com.luifire.phoneaddict1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by LuiFire on 17.11.2017.
 */

public class Autostart extends BroadcastReceiver{

    private static final String TAG = MainActivity.TAG + " AutoStart";

    public void onReceive(Context context, Intent arg1)
    {
        Log.d(TAG, "AutoStart started");
        new Database(context).log("AutoStart started");

        Intent intent = new Intent(context, AlwaysOnService.class);
        context.startService(intent);
    }
}

package com.luifire.phoneaddict1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

/**
 * Created by LuiFire on 26.10.2017.
 */

public class OnOffReceiver extends BroadcastReceiver {
    private static final String TAG = MainActivity.TAG + "OnOffReceiver\t";

    static long mLastId = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive " + intent.toString());

        Database db = new Database(context);

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SCREEN_ON)) {
            mLastId = db.screenTurnedOn();
            db.log("on " + mLastId);
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            db.log("off" + mLastId);
            if (mLastId == -1) {
                Log.w(TAG, "onReceive no Screen on detected!!!!!!!!");
                db.log("no screen on");
            } else {
                db.screenTurnedOff(mLastId);
                mLastId = -1;
            }
        } else {
            Log.w(TAG, "onReceive wrong intent received");
        }
    }
}

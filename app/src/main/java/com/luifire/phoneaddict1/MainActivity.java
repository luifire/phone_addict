package com.luifire.phoneaddict1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "PhoneAddict ";

    private View mCurrentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "start");

        startLoggingService();

        mCurrentView = findViewById(R.id.message);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        findViewById(R.id.Logging).setVisibility(View.VISIBLE);
    }

    private void startLoggingService() {
        Intent service = new Intent(AlwaysOnService.START_SERVICE_MAIN_ACTIVITY);
        service.setClass(this, AlwaysOnService.class);

        startService(service);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Database(this).logd(TAG, "Start App");
        loadEntries();
        loadStatEntries();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        new Database(this).logd(TAG, "onDestroy: App");

        AlwaysOnService.tryRestartApp(this, TAG);
    }

    /**
     * Creates a simple TextView
     *
     * @param
     * @return
     */
    private TextView createTextView(String s) {
        TextView tv = new TextView(this);
        tv.setText(s);

        return tv;
    }

    private void loadEntries() {
        //AlwaysOnService.tryRestartApp(this, TAG);

        Log.d(TAG, "loadEntries");

        Database db = new Database(this);
        ArrayList<String[]> allEntries = db.getEntireData();

        TableLayout onOffTable = findViewById(R.id.OnOffTable);

        // save caption
        TableRow captionRow = (TableRow) onOffTable.getChildAt(0);
        onOffTable.removeAllViews();
        onOffTable.addView(captionRow);

        String currentDate = "";
        // put all on offs in
        for (String[] row : allEntries) {

            // if the day changes, we add a new line with just the date
            if (currentDate.equals(row[0]) == false) {
                // empty row for nice view
                if (currentDate.equals("") == false) {
                    TableRow emptyRow = new TableRow(this);
                    emptyRow.addView(createTextView(""));
                    onOffTable.addView(emptyRow);
                }

                // safe for next round
                currentDate = row[0];

                TableRow dateRow = new TableRow(this);

                TextView tv = createTextView(currentDate);
                // TODO font thicker
                dateRow.addView(tv);
                // date row
                onOffTable.addView(dateRow);
            }

            TableRow newRow = new TableRow(this);

            newRow.addView(createTextView(row[1]));
            newRow.addView(createTextView(row[2]));
            newRow.addView(createTextView(row[3]));
            newRow.addView(createTextView(row[4]));

            onOffTable.addView(newRow);
        }
    }

    public void onClickTableLayout(View v) {
        loadEntries();
    }

    /**
     * sets the given text in view id
     *
     * @param id
     * @param text
     */
    private void setTextInTextView(int id, String text) {
        TextView textView = findViewById(id);
        textView.setText(text);
    }

    // TODO Stemp senden, wenn Service abgeschossen wird
    // Zeit senden, wenn Service bei boot gestarted wird oder doch eher selbst eintragen
    // und dann den Stemp davon senden?
    // Problem: Eintragen dauert eine Weile, könnte wieder runter gefahren sein
    // was passiert, wenn Gerät in der Zeit abgeschalten wurde?
    // => Eintrag geht verloren, nicht so tragisch
    // vielleicht ist beim hochfahren auch gar nicht so schlimm, das wird sowieso evtl gemacht und
    // dann liegt es kurz rum.
    //

    /**
     * Load all entries for stats
     */
    private void loadStatEntries() {
        // TODO do this in a service
        CollectStats stats = new CollectStats(this);

        setTextInTextView(R.id.PhoneOn, stats.getStatus(CollectStats.AVERAGE_ON_TIME));
        setTextInTextView(R.id.PhoneOff, stats.getStatus(CollectStats.AVERAGE_OFF_TIME));
        setTextInTextView(R.id.OffWithoutSleep, stats.getStatus(CollectStats.AVERAGE_OFF_TIME_WITHOUT_SLEEP));

        // average Stuff
        setTextInTextView(R.id.TurnOn, stats.getStatus(CollectStats.AVERAGE_ON_PER_DAY) + " / day");
        setTextInTextView(R.id.StdDevPhoneOn, stats.getStatus(CollectStats.STD_DEV));
        setTextInTextView(R.id.AvgAbsDev, stats.getStatus(CollectStats.AVG_ABS_DEV));
        setTextInTextView(R.id.TotalPhoneOn, stats.getStatus(CollectStats.TOTAL_PHONE_ON));
        setTextInTextView(R.id.TotalDayCount, stats.getStatus(CollectStats.TOTAL_DAY_WITHOUT_EDGES));

        // turned on today
        setTextInTextView(R.id.TurnOnToday, stats.getStatus(CollectStats.PHONE_ON_TODAY));
        setTextInTextView(R.id.TimeOnToday, stats.getStatus(CollectStats.TIME_ON_TODAY));
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            findViewById(R.id.Logging).setVisibility(View.GONE);
            findViewById(R.id.Stats).setVisibility(View.GONE);

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    findViewById(R.id.Logging).setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_dashboard:
                    findViewById(R.id.Stats).setVisibility(View.VISIBLE);
                    loadStatEntries();
                    return true;
                case R.id.navigation_notifications:
                    //mCurrentView.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };
}

package com.luifire.phoneaddict1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by LuiFire on 29.10.2017.
 */

public class Database extends SQLiteOpenHelper {
    private static final String TAG = MainActivity.TAG + "Database";

    public static final int DATABASE_VERSION = 18;
    public static final String DATABASE_NAME = "PhoneAddict.db";

    // SQL Stuff
    final String TABLE_MAIN = "OnOffLog";
    final String ColID = "Id";
    final String ColScreenOn = "ScreenOn";
    final String ColScreenOff = "ScreenOff";

    final String TABLE_LOG = "BugLogg";

    // JULIANDAY(today) - JULIANDAY(yesterday) = 1, JULIANDAY(today) = x.5
    // Julian day - the number of days since noon in Greenwich on November 24, 4714 B.C.
    final String SqliteCurrentDatetime = "JULIANDAY(DATETIME('now','localtime'))";

    final Context mContext;


    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // ###
        // TODO Tag in LogDB einfügen
        String createTable =
                "CREATE TABLE IF NOT EXISTS " + TABLE_MAIN + " (" +
                        ColID + " INTEGER PRIMARY KEY," +
                        ColScreenOn + " REAL ," +
                        ColScreenOff + " REAL DEFAULT NULL)";
        //ColScreenOn + " INTEGER ," +
        //ColScreenOff + " INTEGER DEFAULT NULL)";
        db.execSQL(createTable);

        String createLogTable =
                "CREATE TABLE IF NOT EXISTS " + TABLE_LOG +
                        " (ID INTEGER PRIMARY KEY, " +
                        " message TEXT, time DATETIME)";
        db.execSQL(createLogTable);

       /* Also irgendwie muss der Datentyp für die Zeiten richtig sein und dann muss schon ein Eintrag
        drin sein, wenn am Anfang was geladen werden soll, aber eigentlich ist das auch völlig egal
                weil ja das nur ein Test ist, da müsste ja avg stehen */
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MAIN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG);

        onCreate(db);
    }

    /**
     * turns Date into SQLite Date
     *
     * @param
     * @return
     */
    /*private
    String toSqliteDatetime(Date date) {
        String res;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        res = sdf.format(new Date());

        Log.d(TAG, "toSqliteDatetime: " + res);
        return res;
    }*/
    public void log(String text) {
        SQLiteDatabase db = getWritableDatabase();

        String insertQuery = "INSERT INTO " + TABLE_LOG + "( message, time ) VALUES (\"" + text + "\", DATETIME('now','localtime') )";
        db.execSQL(insertQuery);
        db.close();
    }

    public void loge(String tag, String text) {
        log(text);

        Log.e(TAG, text);
    }

    public void logd(String tag, String text) {
        log(TAG + text);

        Log.d(TAG, text);
    }

    /**
     * Inserts new Row
     *
     * @return rowId
     */
    public long screenTurnedOn() {
        SQLiteDatabase db = getWritableDatabase();

        // Insert stuff, because datetime doesn't work on a normal level
        String insertQuery = "INSERT INTO " + TABLE_MAIN + "(" + ColScreenOn + ") VALUES (" + SqliteCurrentDatetime + ")";
        db.execSQL(insertQuery);
        db.close();

        // get last inserted string
        db = getReadableDatabase();
        String getLastId = "SELECT MAX(" + ColID + ") FROM " + TABLE_MAIN;
        Cursor c = db.rawQuery(getLastId, null);
        c.moveToFirst();
        long newRowId = c.getLong(0);

        return newRowId;
    }

    /**
     * Inserts new Row
     *
     * @return rowId
     */
    public void screenTurnedOff(long screenOnId) {
        // Change Values
        SQLiteDatabase db = getWritableDatabase();
        String updateQuery = "UPDATE " + TABLE_MAIN + " SET " + ColScreenOff + " = " + SqliteCurrentDatetime +
                " WHERE " + ColID + " = " + screenOnId;
        db.execSQL(updateQuery);

        db.close();
    }

    public ArrayList<String[]> getEntireData() {
        // + 1721059.5 to bring it to a normal time
        String query = "SELECT TIME(a." + ColScreenOn + ") AS " + ColScreenOn +
                ", TIME(a." + ColScreenOff + ") AS " + ColScreenOff +
                ", DATE(a." + ColScreenOn + ") AS DATE" +
                ", TIME(a." + ColScreenOff + " - a." + ColScreenOn + " + 0.5) AS DIFF" +
                ", TIME(a." + ColScreenOn + " - b." + ColScreenOff + " + 0.5) AS OffTime " +
                " FROM " + TABLE_MAIN + " a " +
                " INNER JOIN " + TABLE_MAIN + " b ON a." + ColID + " = (b." + ColID + " + 1) " +
                " ORDER BY a." + ColID + " DESC" +
                " LIMIT 100";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        final int onIdx = cursor.getColumnIndexOrThrow(ColScreenOn);
        final int offIdx = cursor.getColumnIndexOrThrow(ColScreenOff);
        final int diffIdx = cursor.getColumnIndexOrThrow("DIFF");
        final int dateIdx = cursor.getColumnIndexOrThrow("DATE");
        final int offTimeIdx = cursor.getColumnIndexOrThrow("OffTime");


        // read DB
        ArrayList<String[]> lastNEntries = new ArrayList<>();
        while (cursor.moveToNext()) {
            String line[] = new String[5];

            line[0] = cursor.getString(dateIdx);
            line[1] = cursor.getString(onIdx);
            line[2] = cursor.getString(offIdx);
            line[3] = cursor.getString(diffIdx);
            line[4] = cursor.getString(offTimeIdx);

            lastNEntries.add(line);
        }
        // close db
        cursor.close();
        db.close();

        return lastNEntries;
    }

    /**
     * Turns the double in its String val or "not yet" (happens on the first day)
     *
     * @param val
     * @return
     */
    String doubleValOrText(Double val) {
        DecimalFormat df = new DecimalFormat("###.###");
        return val.isNaN() || val.isInfinite() ? "not yet" : df.format(val);
    }


    /**
     * get LogData
     *
     * @return
     */
    public ArrayList<String[]> getEntireData2() {
        // + 1721059.5 to bring it to a normal time
        String query = "SELECT TIME(time) AS time" +
                ", message" +
                " FROM " + TABLE_LOG +
                " ORDER BY id DESC" +
                " LIMIT 50";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
        } catch (Exception ex) {
            Log.e(TAG, "getEntireData: " + ex.toString());
        }

        final int logTime = cursor.getColumnIndexOrThrow("time");
        final int logText = cursor.getColumnIndexOrThrow("message");

        // read DB
        ArrayList<String[]> lastNEntries = new ArrayList<>();
        while (cursor.moveToNext()) {
            String line[] = new String[5];

            line[0] = cursor.getString(logTime);
            line[1] = cursor.getString(logText);
            line[2] = "";
            line[3] = "";
            line[4] = "";

            lastNEntries.add(line);
        }
        // close db
        cursor.close();
        db.close();

        return lastNEntries;
    }
}

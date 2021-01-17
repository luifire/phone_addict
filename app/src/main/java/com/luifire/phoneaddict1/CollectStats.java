package com.luifire.phoneaddict1;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by LuiFire on 17.12.2017.
 */

public class CollectStats extends Database {
    public final String TAG = "CollectStats";

    // un hours
    private final String SleepPerNight = "7";
    private final double MinSleep = 2.0 / 24; // how long do you have to sleep to count as a sleep
    private final double MinAwakeTime = 8.0 / 24; // how long do you have to be awake to count as a new goto sleep time

    // gets filled in and will cause problems is used before
    String ConditionNotToday = " ScreenOn <  ";
    String NotLastDay = " ScreenOn > ";
    String NotFirstAndLastDay = "####";

    public static final int TOTAL_PHONE_ON = 0;
    public static final int AVERAGE_ON_PER_DAY = 1;
    public static final int STD_DEV = 2;
    public static final int AVG_ABS_DEV = 3;
    public static final int TOTAL_DAY_WITHOUT_EDGES = 4; // this leaves out today and first day
    public static final int PHONE_ON_TODAY = 5;
    public static final int AVERAGE_ON_TIME = 6;
    public static final int AVERAGE_OFF_TIME = 7;
    public static final int AVERAGE_OFF_TIME_WITHOUT_SLEEP = 8;
    public static final int FIRST_DAY = 9;
    public static final int TODAY_DAY = 10;
    public static final int TIME_ON_TODAY = 11;
    public static final int SLEEP_TIME = 12;

    private static final int ARRAY_COUNT = 13;

    private String Stats[] = new String[ARRAY_COUNT];
    SQLiteDatabase mDB; // will be open for loading everything

    public CollectStats(Context context) {
        super(context);

        mDB = getReadableDatabase();

        // has to be done first
        getDays();

        // collect all data
        getAvgStuff();
        getTodaysStuff();
        getAvgOnTime();
        getAvgOffTime();

        // get sleep stuff
        getSleepTime();

        mDB.close();
    }

    // TODO better int ding
    public String getStatus(int data) {
        return Stats[data];
    }

    /**
     * when do you fall asleep
     */
    private void getSleepTime() {
        String query = "SELECT a.ScreenOff - JULIANDAY(DATE(a.ScreenOff - " + 6.0 / 24 + ")) AS BedTime, " +
                " a.Id AS ID, TIME(a.ScreenOn - b.ScreenOff + 0.5) AS BedTimeString, a.ScreenOn - b.ScreenOff AS Diff " +
                " FROM OnOffLog a " +
                " INNER JOIN OnOffLog b ON a.ID = (b.ID + 1) " +
                " WHERE a.ScreenOn - b.ScreenOff > " + MinSleep;

        Cursor cursor = mDB.rawQuery(query, null);

        final int bedTimeIdx = cursor.getColumnIndex("BedTime");
        final int diffIdx = cursor.getColumnIndex("Diff");

        // calculate average bedtime, because we have them normed, an average should be ok
        double lastBedTime = -10;
        double average = 0;
        int averageCount = 0;

        while (cursor.moveToNext()) {
            double currentBedTime = cursor.getDouble(bedTimeIdx);

            // you have to be awake at least MinAwakeTime
            if (currentBedTime - lastBedTime > MinAwakeTime) {
                average += currentBedTime;
                averageCount++;
                lastBedTime = currentBedTime;
            }
        }
        /*
        Problem aktuell: wenn ich das Datum abziehe (im Query), dann müsste ich eigentlich das datum
        des vortages abziehen. Was aber schlecht geht.
        irgendwas stimmt auch komplett mit dem query nicht.
        Da kommen falsche Uhrzeiten bei raus.
         */

        average /= averageCount;
        logd(TAG, String.valueOf(average));
    }

    /**
     * First, last day, total days
     */
    private void getDays() {
        // Firstday
        String query = "SELECT JULIANDAY(DATE(ScreenOn)) FirstDay, MIN(Id) FROM OnOffLog";
        Double firstDay = DatabaseHelper.getFirstEntryInQuery(mDB, query, "FirstDay", new Double(0));

        // Today
        query = "SELECT JULIANDAY(DATE('now','localtime')) Today";
        Double today = DatabaseHelper.getFirstEntryInQuery(mDB, query, "Today", new Double(0));

        Stats[FIRST_DAY] = firstDay.toString();
        Stats[TODAY_DAY] = today.toString();

        NotLastDay = NotLastDay + Stats[FIRST_DAY] + " ";
        ConditionNotToday = ConditionNotToday + Stats[TODAY_DAY] + " ";
        NotFirstAndLastDay = ConditionNotToday + "AND" + NotLastDay;

        // +1 because yesterday needs to be counted
        Double yesterDay = today - 1;
        Double totalDayCount = yesterDay - firstDay + 1;
        Stats[TOTAL_DAY_WITHOUT_EDGES] = doubleValOrText(totalDayCount);
    }

    /**
     * Gets the average amount phone was on and stdDev
     *
     * @return (TotalCount, Avg, StdDev, NormDev)
     */
    private void getAvgStuff() {
        Double totalDayCount = Double.valueOf(Stats[TOTAL_DAY_WITHOUT_EDGES]);
        //*** get total count
        String queryGetAllCount = "SELECT COUNT(*) AS TotalCount FROM " + TABLE_MAIN +
                " WHERE" + NotFirstAndLastDay;

        Double totalCount = DatabaseHelper.getFirstEntryInQuery(mDB, queryGetAllCount, "TotalCount", new Double(0));

        //*** Average

        Double average = totalCount / totalDayCount;

        //*** StdDev / NormDev
        String queryStdDev = "SELECT SUM(ABS(DayCount)) AS AvgAbsDev, SUM(DayCount * DayCount) AS StdDevSum FROM " +
                "(SELECT Count(*) - " + average + " AS DayCount, ScreenOn, JULIANDAY(ScreenOn) - JULIANDAY(TIME(ScreenOn)) AS SameDay " +
                " FROM OnOffLog WHERE " + NotFirstAndLastDay +
                " GROUP BY SameDay )";

        Cursor c = mDB.rawQuery(queryStdDev, null);
        c.moveToFirst();

        // Sqrt( sum(x_i - avg) / (N-1)) , Sample StdDev
        Double StdDevSum = c.getDouble(c.getColumnIndex("StdDevSum"));
        Double stdDev = Math.sqrt(StdDevSum / (totalDayCount - 1));

        // sum(x_i - avg) / alldays
        Double AvgAbsDev = c.getDouble(c.getColumnIndex("AvgAbsDev"));
        Double normDev = AvgAbsDev / totalDayCount;

        //*** get result together
        Stats[TOTAL_PHONE_ON] = doubleValOrText(totalCount);
        Stats[AVERAGE_ON_PER_DAY] = doubleValOrText(average);
        Stats[STD_DEV] = doubleValOrText(stdDev);
        Stats[AVG_ABS_DEV] = doubleValOrText(normDev);
    }


    /**
     * findes out, how often phone got turned on today
     *
     * @return
     */
    private void getTodaysStuff() {
        // get todays date without time
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(date);

        // nothing can be newer than whats on today
        String query = "SELECT COUNT(*) AS Count, TIME(SUM(ScreenOff - ScreenOn) + 0.5) AS TimeToday " +
                " FROM " + TABLE_MAIN +
                " WHERE " + ColScreenOn + " >= JULIANDAY(DATE('now','localtime'))";

        Cursor c = mDB.rawQuery(query, null);
        c.moveToFirst();

        Long countToday = c.getLong(c.getColumnIndex("Count"));
        String timeToday = c.getString(c.getColumnIndex("TimeToday"));

        c.close();

        Stats[PHONE_ON_TODAY] = countToday.toString();
        Stats[TIME_ON_TODAY] = makeStatTimeReadable(timeToday);
    }


    /**
     * turns 00:30 into 00min 30s
     *
     * @param time
     * @return
     */
    private String makeStatTimeReadable(String time) {
        String s;

        if (time == null)
            s = "nothing yet";
        else
            s = time; //"" + time.charAt(3) + time.charAt(4) + "min " + time.charAt(6) + time.charAt(7) + "s";

        return s;
    }

    /**
     * gets the average on time
     * onTime = false then off time
     *
     * @return
     */
    private void getAvgOnTime() {
        // + 0.5 because counting started at noon
        String query = "SELECT TIME(AVG(" + ColScreenOff + " - " + ColScreenOn + ") + 0.5)  AS AvgTime" +
                " FROM " + TABLE_MAIN + " WHERE ScreenOff != ''";

        String avgTime = DatabaseHelper.getFirstEntryInQuery(mDB, query, "AvgTime", "");

        // cut the hours, they are irrelevant
        Stats[AVERAGE_ON_TIME] = makeStatTimeReadable(avgTime);
    }

    /**
     * gets the average on time
     * onTime = false then off time
     *
     * @return
     */
    private void getAvgOffTime() {
        int TotalNightCount = Integer.valueOf(Stats[TOTAL_DAY_WITHOUT_EDGES]) + 1;
        // + 0.5 because counting started at noon
        // LEFT JOIN not supported
        // OffTimeWithoutSleep = Sum of OffTime - TotalDays - Sleep
        String query = "SELECT TIME(AVG(b." + ColScreenOn + " - a." + ColScreenOff + ") + 0.5) AS AvgTime " +
                ", TIME((SUM(b.ScreenOn - a.ScreenOff) - " + TotalNightCount + " * " + SleepPerNight + "/24)" +
                "/ COUNT(b.ID) + 0.5) AS OffTimeWithoutSleep" +
                " FROM " + TABLE_MAIN + " a" +
                " INNER JOIN " + TABLE_MAIN + " b on a.id = (b.id - 1)" + // -1 gives the last entry
                " WHERE a." + ColScreenOff + " != '' ";

        Cursor c = mDB.rawQuery(query, null);
        c.moveToFirst();

        String avgTime = c.getString(c.getColumnIndex("AvgTime"));
        String withoutSleep = c.getString(c.getColumnIndex("OffTimeWithoutSleep"));

        // cut the hours, they are irrelevant
        Stats[AVERAGE_OFF_TIME] = makeStatTimeReadable(avgTime);
        Stats[AVERAGE_OFF_TIME_WITHOUT_SLEEP] = makeStatTimeReadable(withoutSleep);

        c.close();
    }

}

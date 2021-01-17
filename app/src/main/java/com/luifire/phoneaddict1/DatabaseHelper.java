package com.luifire.phoneaddict1;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.Long;

/**
 * Created by LuiFire on 12.12.2017.
 */

public class DatabaseHelper {

    /**
     * Dummy functions to get the right value by type
     *
     * @param c
     * @param idx
     * @param dummy
     * @return
     */
    //static private <T> T getCursorData(Cursor c, int idx, T dummy) {
      //  return null;
   // }
/*
    static private <Long> Long getCursorData(Cursor c, int idx, Long dummy) {
        long a = c.getLong(idx);

        dummy = Long.valueOf(a);

        return dummy;
    }

    static private <Double> Double getCursorData(Cursor c, int idx, Double dummy) {
        return c.getDouble(idx);
    }

    static private <String> String getCursorData(Cursor c, int idx, String dummy) {
        return c.getString(idx);
    }*/

    /**
     * querys with just one result wrapper
     *
     * @param db
     * @param query
     * @param column
     * @param <T>    type
     * @return
     */
    static public <T> T getFirstEntryInQuery(SQLiteDatabase db, String query, String column, T dummy) {
        Object result = null;
        //T dummy = null;

        Cursor c = db.rawQuery(query, null);
        c.moveToFirst();

        int columnIdx = c.getColumnIndex(column);

        if (dummy instanceof Double)
            result = c.getDouble(columnIdx);
        else if (dummy instanceof Long)
            result = c.getLong(columnIdx);
        else if (dummy instanceof String)
            result = c.getString(columnIdx);

        return (T) result;
    }
}

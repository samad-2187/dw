package com.lubenard.digital_wellbeing;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.HashMap;

public class dbManager extends SQLiteOpenHelper {

    public static final String TAG = "DB";

    static final String dbName = "dataDB";

    // screen time table
    static final String screenTimeTable = "screenTime";
    static final String screenTimeTableDate = "date";
    static final String screenTimeTableScreenTime = "screenTime";

    // appTime table
    static final String appTimeTable = "appTime";
    static final String appTimeTableDate = "date";
    static final String appTimeTableTimeSpent = "timeSpent";
    static final String appTimeTableAppId = "appId";

    // apps table
    static final String appsTable = "apps";
    static final String appsTableId = "id";
    static final String appsTablePkgName = "appPkgName";
    static final String appsTableName = "appName";

    // Join from appTime and apps tables
    static final String viewAppsTables="ViewAppsTables";

    private Context context;

    public dbManager(Context context) {
        super(context, dbName , null,1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create screenTime table
        db.execSQL("CREATE TABLE " + screenTimeTable + " (" + screenTimeTableDate + " DATE PRIMARY KEY, " + screenTimeTableScreenTime + " INTEGER)");

        // Create appTime table
        db.execSQL("CREATE TABLE " + appTimeTable + " (" + appTimeTableAppId + " INTEGER, " + appTimeTableDate + " DATE, " + appTimeTableTimeSpent + " INTEGER, " +
                "PRIMARY KEY(" + appTimeTableAppId + ", " +appTimeTableDate + "))");

        // Create apps table
        db.execSQL("CREATE TABLE " + appsTable + " (" + appsTableId + " INTEGER PRIMARY KEY AUTOINCREMENT, " + appsTablePkgName + " TEXT, " + appsTableName + " TEXT)");

        // Create view joining appTime and apps tables
        db.execSQL("CREATE VIEW " + viewAppsTables + " AS SELECT " +
                appsTable + "." + appsTablePkgName + ", " +
                appsTable + "." + appsTableName + ", " +
                appTimeTable + "." + appTimeTableTimeSpent + ", " +
                appTimeTable + "." + appTimeTableDate + "" +
                " FROM " + appTimeTable + " JOIN " + appsTable +
                " ON " + appTimeTable + "." + appTimeTableAppId + " = " + appsTable + "." + appsTableId
        );

        Log.d("DB", "The db has been created, this message should only appear once.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public String getAppNameFromPkgName(String pkgName) {
        final PackageManager pm = context.getPackageManager();
        String appName;
        try {
            appName = (String) pm.getApplicationLabel(pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA));
        } catch (final PackageManager.NameNotFoundException e) {
            appName = "";
        }
        return appName;
    }

    // Create a app Entry only if non existent:
    // Example: you just installed a app, which is not added in the db yet
    private void createAppRow(String date, String appPkgName) {
        SQLiteDatabase readableDb = this.getReadableDatabase();
        String[] columns = new String[]{appsTablePkgName};
        Cursor c = readableDb.query(appsTable, columns, appsTablePkgName + "=?",
                new String[]{appPkgName}, null, null, null);

        if (c.getCount() == 0) {
            Log.d(TAG, "AppData: I create the entry for " + appPkgName);
            readableDb.close();
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(appsTablePkgName, appPkgName);
            cv.put(appsTableName, getAppNameFromPkgName(appPkgName));
            db.insert(appsTable, null, cv);
            db.close();
        } else
            Log.d(TAG, "AppData: The entry for " + appPkgName + " already seems to exist");
        readableDb.close();
    }

    public void updateScreenTime(int addTime, String date) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(screenTimeTableScreenTime, addTime);

        Log.d(TAG, "updateScreenTime: update with new value (time = " + addTime + ") for date = " + date);
        int u = db.update(screenTimeTable, cv, screenTimeTableDate + "=?", new String []{date});
        if (u == 0) {
            Log.d(TAG, "updateScreenTime: update does not seems to work, insert data: (time = " + addTime + ") for date = " + date);
            cv.put(screenTimeTableDate, date);
            db.insertWithOnConflict(screenTimeTable, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
        db.close();
    }

    public int getIdFromPkgName(String pkgName)
    {
        int value;
        SQLiteDatabase readableDb = this.getReadableDatabase();
        String[] columns = new String[]{appsTableId};
        Cursor c = readableDb.query(appsTable, columns, appsTablePkgName + "=?",
                new String[]{pkgName}, null, null, null);
        if (c.moveToFirst())
            value = c.getInt(0);
        else
            value = -1;
        readableDb.close();
        return value;
    }

    public void updateAppData(HashMap<String, Integer> app_data, String date) {

        SQLiteDatabase writeableDb;

        //SQLiteDatabase readableDb = this.getReadableDatabase();
        String[] columns = new String[]{appTimeTableDate, appTimeTableAppId};

        for (HashMap.Entry<String, Integer> entry : app_data.entrySet()) {
            ContentValues cv = new ContentValues();

            createAppRow(date, entry.getKey());

            writeableDb = this.getWritableDatabase();

            Log.d(TAG, "updateAppData: Data in HASHMAP " + entry.getKey() + ":" + entry.getValue().toString());
            cv.put(appTimeTableTimeSpent, entry.getValue());

            Log.d(TAG, "updateScreenTime: update with new value (timeSpent = " + entry.getValue()+ "IdFromPkgName = " + getIdFromPkgName(entry.getKey()) + ") for date = " + date);
            int u = writeableDb.update(appTimeTable, cv, appTimeTableDate + "=? AND " + appTimeTableAppId + "=?", new String []{date, String.valueOf(getIdFromPkgName(entry.getKey()))});
            if (u == 0) {
                Log.d(TAG, "updateAppData: update does not seems to work, insert data: (timeSpent = " + entry.getValue()+ "IdFromPkgName = " + getIdFromPkgName(entry.getKey()) + ") for date = " + date);
                cv.put(appTimeTableDate, date);
                cv.put(appTimeTableAppId, getIdFromPkgName(entry.getKey()));
                writeableDb.insertWithOnConflict(screenTimeTable, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            writeableDb.close();
        }
        //readableDb.close();
        //writeableDb.close();
    }

    // Use this function for testing
    public void getTableAsString(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("DB", "getTableAsString called for " + tableName);
        Log.d("DB", String.format("Table %s:\n", tableName));
        Cursor allRows  = db.rawQuery("SELECT * FROM " + tableName, null);
        if (allRows.moveToFirst() ){
            String[] columnNames = allRows.getColumnNames();
            do {
                for (String name: columnNames) {
                    Log.d("DB", String.format("%s: %s\n", name,
                            allRows.getString(allRows.getColumnIndex(name))));
                }
                Log.d("DB","\n");

            } while (allRows.moveToNext());
        }
    }

    public HashMap<String, Integer> getAppStats(String date) {
        HashMap<String, Integer> app_data = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String [] columns = new String[]{appsTableName, appTimeTableTimeSpent};
        Cursor c = db.query(viewAppsTables, columns, appTimeTableDate + "=?",
                new String[]{date}, null, null, null);

        Log.d("DB", "getAppStats: Cursor is " + c.moveToFirst() + " date is " + date);

        while (c.moveToNext()) {
            app_data.put(c.getString(c.getColumnIndex(appsTableName)), c.getInt(c.getColumnIndex(appTimeTableTimeSpent)));
            Log.d("DB", "getStatApp adding " + c.getString(c.getColumnIndex(appsTableName)) + " and value " + c.getInt(c.getColumnIndex(appTimeTableTimeSpent)));
        }
        c.close();
        getTableAsString(appsTable);
        getTableAsString(appTimeTable);
        return app_data;
    }

    public short getScreenTime(String date) {
        short value = 0;
        SQLiteDatabase db = this.getReadableDatabase();

        String [] columns = new String[]{screenTimeTableScreenTime};
        Cursor c = db.query(screenTimeTable, columns, screenTimeTableDate + "=?",
                new String[]{date}, null, null, null);

        Log.d("DB", "Cursor is " + c.moveToFirst() + " date is " + date);

        if (c.moveToFirst())
            value = c.getShort(0);
        else
            getTableAsString(screenTimeTable);
        c.close();

        return value;
    }
}
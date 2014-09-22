package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;
import edu.buffalo.cse.cse486586.simpledht.communicator.OperationsHandler;
import edu.buffalo.cse.cse486586.simpledht.communicator.ServerThread;
import edu.buffalo.cse.cse486586.simpledht.databasehelper.CustomSQLiteOpenHelper;

import static edu.buffalo.cse.cse486586.simpledht.constants.Constants.*;

public class SimpleDhtProvider extends ContentProvider {

    private static final UriMatcher URI_MATCHER = new UriMatcher(1);

    private CustomSQLiteOpenHelper sqLiteOpenHelper;
    private SQLiteDatabase db;

    private OperationsHandler operationsHandler;


    @Override
    public boolean onCreate() {
        URI_MATCHER.addURI(AUTHORITY, TABLE_NAME, 1);
        sqLiteOpenHelper = new CustomSQLiteOpenHelper(getContext(), DB_NAME, null, 1);
        operationsHandler = new OperationsHandler(getContext().getContentResolver(), getMyPort());
        new ServerThread(operationsHandler).start();
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        getDbIfNull();

        final Object key = values.get(KEY_FIELD);
        final Object value = values.get(VALUE_FIELD);
        if (!operationsHandler.isInMyScope((String) key)) {
            operationsHandler.forwardInsertRequest(key, value, null);
            return uri;
        }

        String sql = "REPLACE INTO content (key, value) VALUES ('" + key + "','" + value + "')";
        db.execSQL(sql);

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        getDbIfNull();

        if (ALL_KEYS.equals(selection)) {
            return operationsHandler.forwardQueryRequest(ALL_KEYS, null);
        } else if (LOCAL_KEYS.equals(selection)) {
            selection = null; // to return all from current
        } else {
            if (!operationsHandler.isInMyScope(selection)) {
                return operationsHandler.forwardQueryRequest(selection, null);
            } else {
                selection = KEY_FIELD + "='" + selection + "'";
            }
        }

        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        qBuilder.setTables(TABLE_NAME);

        Cursor resultCursor = qBuilder.query(db, projection, selection, null, null, null, null);
        if (resultCursor != null) {
            resultCursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return resultCursor;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        getDbIfNull();

        if (ALL_KEYS.equals(selection)) {
            operationsHandler.forwardDeleteRequest(ALL_KEYS, null);
            selection = null; //to delete all from current
        } else if (LOCAL_KEYS.equals(selection)) {
            selection = null; // to delete all from current
        } else {
            if (!operationsHandler.isInMyScope(selection)) {
                operationsHandler.forwardDeleteRequest(selection, null);
                return 0;
            }
        }

        String sql = "DELETE FROM " + TABLE_NAME;
        if (selection != null) {
            sql += " where key = '" + selection + "'";
        }
        db.execSQL(sql);

        Log.v("delete", selection);
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


    private void getDbIfNull() {
        if (db == null) {
            db = sqLiteOpenHelper.getWritableDatabase();
        }
    }

    private int getMyPort() {
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        return Integer.parseInt(portStr)*2;
    }
}
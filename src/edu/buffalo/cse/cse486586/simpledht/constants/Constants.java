package edu.buffalo.cse.cse486586.simpledht.constants;

import android.net.Uri;

import static edu.buffalo.cse.cse486586.simpledht.utils.Utils.buildUri;

public class Constants {
    public static final String DELIMITER = "---";
    public static final String KEY_FIELD = "key";
    public static final String VALUE_FIELD = "value";
    public static final String TABLE_NAME = "content";
    public static final String DB_NAME = "db";
    public static final String SQL_CREATE_CONTENT = "CREATE TABLE content (key TEXT UNIQUE, value TEXT)";
    public static final String COUNT_QUERY = "select COUNT(*) AS count from " + TABLE_NAME;
    public static final int LEADER_PORT = 11108;
//    public static final int PORTS_LIST[] = {LEADER_PORT, 11112, 11116, 11120, 11124};
    public static final String AUTHORITY = "edu.buffalo.cse.cse486586.simpledht.provider";
    public static final Uri URI = buildUri(TABLE_NAME, AUTHORITY);
    public static final String LOCAL_KEYS = "@";
    public static final String ALL_KEYS = "*";
    public static final int SERVER_PORT = 10000;
    public static final String AWAITED =  "";

}

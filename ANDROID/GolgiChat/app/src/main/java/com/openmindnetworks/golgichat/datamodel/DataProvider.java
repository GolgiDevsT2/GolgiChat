package com.openmindnetworks.golgichat.datamodel;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.openmindnetworks.golgichat.utils.DBG;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by derekdoherty on 13/01/2015.
 * This is a Content Provider based on an SQLite Database
 * A content provider is a Server Abstraction to an SQLiteDatabase in this case
 */

public class DataProvider extends ContentProvider
{
    public enum dateType
    {
        FULLDATE,
        HOURMINUTE,
        MILLISECONDS
    }
    // This defines the table URis
    public static final Uri CONTENT_URI_MESSAGES = Uri.parse("content://com.openmindnetworks.golgichat.provider/messages");
    public static final Uri CONTENT_URI_CONTACTS = Uri.parse("content://com.openmindnetworks.golgichat.provider/contacts");

    private static final int MESSAGES_ALLROWS = 1;
    private static final int MESSAGES_SINGLE_ROW = 2;
    private static final int CONTACTS_ALLROWS = 3;
    private static final int CONTACTS_SINGLE_ROW = 4;

    private static final UriMatcher uriMatcher;

    static
    {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("com.openmindnetworks.golgichat.provider", "messages", MESSAGES_ALLROWS);
        uriMatcher.addURI("com.openmindnetworks.golgichat.provider", "messages/#", MESSAGES_SINGLE_ROW);
        uriMatcher.addURI("com.openmindnetworks.golgichat.provider", "contacts", CONTACTS_ALLROWS);
        uriMatcher.addURI("com.openmindnetworks.golgichat.provider", "contacts/#", CONTACTS_SINGLE_ROW);
    }

    public final static String COL_ID = "_id";

    public static final String TABLE_MESSAGES = "messages";

    public static final String COL_MSG = "msg";
    public static final String COL_FROM = "email";
    public static final String COL_FROM_IN_GROUP = "emailwithingroup";

    public static final String COL_TO = "email2";
    public static final String COL_DELIVERY_STATUS = "delivery_status";

    public static final String COL_AT = "at";
    public static final String COL_SEND_TIMESTAMP = "send_timestamp";
    public static final String COL_IS_GROUP_MESSAGE = "is_group_message";

    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_LOCATION_TIMESTAMP = "location_timestamp";


    public static final String TABLE_CONTACTS = "contacts";

    public static final String COL_NAME = "name";
    public static final String COL_REGID = "regid";
    public static final String COL_IMAGE_REF = "imageref";
    public static final String COL_COUNT = "count";    // Number of unviewed messages for this contact

    public static final String COL_GROUPNAME = "groupname";
    public static final String COL_GROUPMEMBERS  = "groupmembers";
    public static final String COL_GROUPREGIDS = "groupregids";
    public static final String COL_ISCHECKED = "ischecked";
    public static final String COL_ACKED_SEEN = "acked_seen";

    private DbHelper dbHelper;


    public static final String DELIVERY_STATUS_DELIVERED = "Delivered";
    public static final String DELIVERY_STATUS_SEEN = "Seen";
    public static final String DELIVERY_STATUS_ERROR_SENDING = "error sending";


    @Override
    public boolean onCreate() {
        dbHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch(uriMatcher.match(uri)) {
            case MESSAGES_ALLROWS:
            case CONTACTS_ALLROWS:
                qb.setTables(getTableName(uri));
                break;

            case MESSAGES_SINGLE_ROW:
            case CONTACTS_SINGLE_ROW:
                qb.setTables(getTableName(uri));
                qb.appendWhere("_id = " + uri.getLastPathSegment());
                break;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long id;
        switch(uriMatcher.match(uri)) {
            case MESSAGES_ALLROWS:
                id = db.insertOrThrow(TABLE_MESSAGES, null, values);
                if (values.get(COL_TO) == null)
                {
                    db.execSQL("update contacts set count=count+1 where name = ?", new Object[]{values.get(COL_FROM)});
                    getContext().getContentResolver().notifyChange(CONTENT_URI_CONTACTS, null);
                }
                break;

            case CONTACTS_ALLROWS:
                id = db.insertOrThrow(TABLE_CONTACTS, null, values);
                break;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        Uri insertUri = ContentUris.withAppendedId(uri, id);
        getContext().getContentResolver().notifyChange(insertUri, null);
        return insertUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int count;
        switch(uriMatcher.match(uri)) {
            case MESSAGES_ALLROWS:
            case CONTACTS_ALLROWS:
                count = db.delete(getTableName(uri), selection, selectionArgs);
                break;

            case MESSAGES_SINGLE_ROW:
            case CONTACTS_SINGLE_ROW:
                count = db.delete(getTableName(uri), "_id = ?", new String[]{uri.getLastPathSegment()});
                break;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int count;
        switch(uriMatcher.match(uri)) {
            case MESSAGES_ALLROWS:
            case CONTACTS_ALLROWS:
                count = db.update(getTableName(uri), values, selection, selectionArgs);
                break;

            case MESSAGES_SINGLE_ROW:
            case CONTACTS_SINGLE_ROW:
                count = db.update(getTableName(uri), values, "_id = ?", new String[]{uri.getLastPathSegment()});
                break;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
    public static String getDateTime(dateType dt)
    {
        SimpleDateFormat dateFormat;
        Date date = new Date();

        switch (dt)
        {
            case FULLDATE:
                dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss", Locale.getDefault() );
                break;

            case HOURMINUTE:
                dateFormat = new SimpleDateFormat( "HH:mm", Locale.getDefault() );
                break;

            case MILLISECONDS:
                return String.valueOf( date.getTime() );


            default:
                throw new IllegalArgumentException("Unsupported Date Type: ");
        }


        return dateFormat.format(date);
    }


    private static class DbHelper extends SQLiteOpenHelper
    {

        private static final String DATABASE_NAME = "golgichat.db";
        // Version 2 06/03 new column "acked_seen" in contacts table
        private static final int DATABASE_VERSION = 3;
        // Version 3 22/03 new columns for location in messages table
        //private static final int DATABASE_VERSION = 3;

        public DbHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            DBG.write("DataProvider.DBHelper.onCreate() ");

            db.execSQL("create table messages (_id integer primary key autoincrement, msg text, email text, emailwithingroup text, email2 text," +
                    " delivery_status text default '', at datetime default (strftime('%H:%M:%S','now' , 'localtime')), is_group_message text default '' , " +
                    " send_timestamp integer default 0 unique , latitude text default '' ,  longitude text default '' ,  location_timestamp text default '' );");

            // (strftime('%s','now')) gives Epoch Time

            db.execSQL("create table contacts (_id integer primary key autoincrement, ischecked integer default 1, " +
                    " name text unique, regid text, groupname text default '', groupmembers text default '', groupregids text default '', acked_seen text default '', imageref text default 'a', count integer default 0);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            DBG.write("DataProvider.DBHelper.onUpgrade() oldVersion = >" + oldVersion + "< newVersion = >" + newVersion + "< ");

            switch(oldVersion)
            {
                // For installations where the database vesion is 1, add this acked_seen Column
                case 1:
                    db.execSQL("ALTER TABLE " + " contacts " + " ADD COLUMN " + " acked_seen text default '' ");
                case 2:
                    db.execSQL("ALTER TABLE " + " messages " + " ADD COLUMN " + " latitude text default '' ");
                    db.execSQL("ALTER TABLE " + " messages " + " ADD COLUMN " + " longitude text default '' ");
                    db.execSQL("ALTER TABLE " + " messages " + " ADD COLUMN " + " location_timestamp text default '' ");
            }
        }
    }



    private String getTableName(Uri uri)
    {
        switch(uriMatcher.match(uri))
        {
            case MESSAGES_ALLROWS:
            case MESSAGES_SINGLE_ROW:
                return TABLE_MESSAGES;

            case CONTACTS_ALLROWS:
            case CONTACTS_SINGLE_ROW:
                return TABLE_CONTACTS;
        }
        return null;
    }
}

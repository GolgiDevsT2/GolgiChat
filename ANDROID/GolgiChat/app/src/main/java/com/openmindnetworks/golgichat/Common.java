package com.openmindnetworks.golgichat;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.openmindnetworks.golgichat.datamodel.DataProvider;
import com.openmindnetworks.golgichat.utils.DBG;
import com.openmindnetworks.golgichat.utils.Random;


/**
 * Created by derekdoherty on 13/01/2015.
 * This the Application class which can hold Global Data
 * through out the App in Particular the userName and RegId
 * once they are confirmed by the Server
 */

public class Common extends Application
{
    public static String nickName;
    public static String regId;
    public static String golgiChatServer;
    public static boolean alreadyRegisteredOnce = false;
    public static boolean inFg = false;

    public static boolean ContactsTabView = true;
    public static final String GLOBALCONFIGURATIONSERVER = "GCCPRODSERVER";
    public static String maybeNickName = "";
    public static final String GOLGICHAT_ALL_MEMBER_GROUP_NAME = "ALL MEMBERS GROUP";
    public static final int NOTIFICATION_ONE = 2468;


    public static int storedVersionNumber = 999; // Should be minimum version
    public static int versionNumber = 3;   // Update this vesion IF Group Wipe is Needed

    public static boolean isNewVersion = false;
    public static boolean isNewInstall = false;

    public static boolean isContactMessageFragmentVisible = false;

    public static String contactNameCurrentlyInView = "";
    public static String groupContactCurrentlyInView = "";

    public static double myLatestLat = 0;
    public static double myLatestLong = 0;
    public static String myLatestLocationTimeStamp = "";
    public static boolean requestingLocationUpdates = true; // Should use this as a preference parameter

    //private static SharedPreferences prefs;

    public boolean isNetworkAvailable()
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected())
        {
            return true;
        }
        return false;
    }




    @Override
    public void onCreate()
    {
        super.onCreate();

        SharedPreferences prefs = getSharedPreferences("GolgiChatPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        nickName = prefs.getString("nickName", "");
        regId = prefs.getString("regId", "");
        golgiChatServer = prefs.getString("golgiChatServer", "");
        alreadyRegisteredOnce = prefs.getBoolean("alreadyRegisteredOnce", false);

        storedVersionNumber = prefs.getInt("VERSION_NUMBER", 0);
        //inFg = prefs.getBoolean("inFg", true);
        editor.putInt("VERSION_NUMBER", versionNumber);
        editor.commit();

        DBG.write("Stored Version Number = >" + storedVersionNumber + "< " + " Version Number = >" + versionNumber + "< ");




        // Check for empty regId, get a new one if needed and store
        if (regId.equals(""))
        {
            regId = Random.genRandomStringWithLength(20);
            editor.putString("regId", regId);
            editor.commit();
        }
        editor.putBoolean("alreadyRegisteredOnce" , alreadyRegisteredOnce);
        editor.commit();
        //editor.putBoolean("inFg" , inFg);
        //editor.commit();


        if (storedVersionNumber == 0)
        {
            DBG.write("New Install!!");
            isNewInstall = true;
        }

        if (versionNumber != storedVersionNumber)
        {
            DBG.write("Upgrade !!");
            isNewVersion = true;
        }

        DBG.write("isNewInstall = >" + isNewInstall + "< " + " isNewVersion = >" + isNewVersion + "< ");


        // Delete Group Entries in this Case !!
        // if (isNewVersion && !isNewInstall)
        // TODO: Not sure this is the behaviour that is wanted when we have a new version
        if (isNewVersion)
        {
            // Delete Contacts that are groups
            String select = "(" + DataProvider.COL_GROUPNAME + " <> '') ";
            DBG.write("Common.deleteRows from Groups  Select statement is = " + select);

            int numOfRowsDeleted = getContentResolver().delete(
                    DataProvider.CONTENT_URI_CONTACTS,
                    select,
                    null);

            // Remove messages that belong to groups
            select = "(" + DataProvider.COL_IS_GROUP_MESSAGE + " <> '') ";
            DBG.write("Common.deleteGroupMessages from Groups  Select statement is = " + select);

            numOfRowsDeleted = getContentResolver().delete(
                    DataProvider.CONTENT_URI_MESSAGES,
                    select,
                    null);
        }


        // Default All member Group, This special group can be defined by the fullgroupName and GroupName been the same!!
        DBG.write("Common.onCreate() Making a default ALL MEMBER Group >" + GOLGICHAT_ALL_MEMBER_GROUP_NAME + "<");
        try
        {
            ContentValues values = new ContentValues(6);
            //values.put(DataProvider.COL_NAME, email.substring(0, email.indexOf('@')));

            values.put(DataProvider.COL_NAME, GOLGICHAT_ALL_MEMBER_GROUP_NAME );
            values.put(DataProvider.COL_GROUPNAME, GOLGICHAT_ALL_MEMBER_GROUP_NAME );
            values.put(DataProvider.COL_GROUPMEMBERS, "");
            values.put(DataProvider.COL_GROUPREGIDS, "");

            String capitalFirstLetter = GOLGICHAT_ALL_MEMBER_GROUP_NAME.substring(0, 1).toLowerCase();
            DBG.write("First Letter of Group Contact =  >" + capitalFirstLetter + "<");
            values.put(DataProvider.COL_IMAGE_REF, capitalFirstLetter);

            // TODO: For a Group , setting REGID to empty
            values.put(DataProvider.COL_REGID, "");

            getContentResolver().insert(DataProvider.CONTENT_URI_CONTACTS, values);

        } catch (SQLException sqle)
        {
            DBG.write("Add All Group Members  SQL Error!!, Assuming is Already existing Error !!! error = " + sqle.getLocalizedMessage() + " Cause  " + sqle.getCause() + " Message  " + sqle.getMessage());
        }
    }
}



package com.openmindnetworks.golgichat;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.database.SQLException;

import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;


import com.openmindnetworks.golgi.api.GolgiAPI;
import com.openmindnetworks.golgi.api.GolgiAPIHandler;
import com.openmindnetworks.golgi.api.GolgiException;
import com.openmindnetworks.golgi.api.GolgiTransportOptions;
import com.openmindnetworks.golgichat.datamodel.DataProvider;
import com.openmindnetworks.golgichat.gen.GolgiKeys;
import com.openmindnetworks.golgichat.gen.GroupMembers;
import com.openmindnetworks.golgichat.gen.LocationInfo;
import com.openmindnetworks.golgichat.gen.RegCode;
import com.openmindnetworks.golgichat.utils.DBG;

import io.golgi.apiimpl.android.GolgiAbstractService;
import com.openmindnetworks.golgichat.gen.golgiChatService;

import java.util.ArrayList;


/**
 * Created by derekdoherty on 13/01/2015.
 */
public class GolgiService extends GolgiAbstractService
{
    private static Object syncObj = new Object();

    // Strings for intents to the Doorman UI
    final static String DOORMAN_ACCESS_INTENT = "com.openmindnetworks.golgichat.GolgiService.DOORMAN_ACCESS_INTENT";
    final static String DOORMAN_RESULT = "com.openmindnetworks.golgichat.GolgiService.DOORMAN_INTENT";
    final static String TEXT_TO_DISPLAY = "com.openmindnetworks.golgichat.GolgiService.TEXT_TO_DISPLAY";
    final static String REGNAME = "com.openmindnetworks.golgichat.GolgiService.REGNAME";

    // Local Broadcast Manager
    private static LocalBroadcastManager sBroadcaster = null;
    private golgiChatService.typingStatus.RequestReceiver typingReceiver = new golgiChatService.typingStatus.RequestReceiver()
    {
        @Override
        public void receiveFrom(golgiChatService.typingStatus.ResultSender resultSender, String regName, String textToDisplay)
        {
            resultSender.success();
           // DBG.write("Recieved text from " + regName + " Text to DISPLAY " + textToDisplay);
            try
            {
                Intent intent = new Intent(DOORMAN_ACCESS_INTENT);
                intent.putExtra(TEXT_TO_DISPLAY,textToDisplay);
                intent.putExtra(REGNAME, regName); // The person who is Typing!!
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
            }
            catch (Exception e){
            }
            finally {
            }
        }
    };



    @Override
    public void onCreate()
    {
        super.onCreate();
        sBroadcaster = LocalBroadcastManager.getInstance(this);
    }

    public void readyForRegister()
    {
        GolgiAPI.setOption("USE_TEST_SERVER", "0");

        // Just Doing a sanity Check for existing RegId
        if (Common.regId.length() > 0 )
        {
            golgiChatService.seenReceipt.registerReceiver(new golgiChatService.seenReceipt.RequestReceiver()
            {
                @Override
                public void receiveFrom(golgiChatService.seenReceipt.ResultSender resultSender, String regName, String ackedSeenTime)
                {
                    DBG.write("Received SEENRECEIPT message from UserName >" + regName + "< " +  " messageSendTime = >" + ackedSeenTime + "< " );
                    resultSender.success();

                    // This indicates that all messages that I have sent to this contact up to this time have now been seen
                    // Update the COL_ACKED_SEEN column for this contact to this time
                    try
                    {
                        String select = "((" + DataProvider.COL_NAME + " = '" + regName + "'))";
                        ContentValues values = new ContentValues();
                        values.put(DataProvider.COL_ACKED_SEEN, ackedSeenTime);

                        getContentResolver().update(DataProvider.CONTENT_URI_CONTACTS, values, select, null);
                    } catch (SQLException sqle)
                    {
                        DBG.write("GolgiService SEENRECEIPT SQL Error!! in update COL_ACKED_SEEN" + sqle.getMessage());
                    }

                    // Maybe Just select messages TO this user whoese status is delivered and set to Seen
                    // Could be residual messages which have not been delivered for some reason
                    // Select messages that were sent to this contact and whoese status is Delivered
                    try
                    {
                        String select = "((" + DataProvider.COL_TO + " = '" + regName + "') AND (" + DataProvider.COL_DELIVERY_STATUS + " = '" + DataProvider.DELIVERY_STATUS_DELIVERED + "' ))";
                        ContentValues values = new ContentValues();
                        values.put(DataProvider.COL_DELIVERY_STATUS, DataProvider.DELIVERY_STATUS_SEEN);
                        getContentResolver().update(DataProvider.CONTENT_URI_MESSAGES, values, select, null);

                    } catch (SQLException sqle)
                    {
                        DBG.write("GolgiService SEENRECEIPT SQL Error!! in update COL_DELIVERY_STATUS " + sqle.getMessage());
                    }
                }
            });

            // This is the transaction that receives updated Contact info from new users as they register with the server
            golgiChatService.updateOrAddContact.registerReceiver(new golgiChatService.updateOrAddContact.RequestReceiver()
            {
                @Override
                public void receiveFrom(golgiChatService.updateOrAddContact.ResultSender resultSender, String regName, String regCode)
                {
                    resultSender.success();
                    DBG.write("This is a message from the SERVER UPDATEORADCONTACT , add contact if it does not exist >" + regName + "<");
                    try {
                        ContentValues values = new ContentValues();
                        //values.put(DataProvider.COL_NAME, email.substring(0, email.indexOf('@')));
                        values.put(DataProvider.COL_NAME, regName);
                        values.put(DataProvider.COL_REGID, regCode);

                        String capitalFirstLetter = regName.substring(0, 1).toLowerCase();
                        DBG.write("First Letter of Contact =  >" + capitalFirstLetter + "<");
                        values.put(DataProvider.COL_IMAGE_REF, capitalFirstLetter);

                        getContentResolver().insert(DataProvider.CONTENT_URI_CONTACTS, values);
                    } catch (SQLException sqle)
                    {
                        try {
                            // If it does exist then just update the REGID for a Normal Contact
                            DBG.write("GolgiService UPDATEORADCONTACT SQL Error!! in ADD Transaction normal Contact" + sqle.getMessage());

                            String select = "((" + DataProvider.COL_NAME + " = '" + regName + "'))";
                            ContentValues values = new ContentValues();
                            values.put(DataProvider.COL_REGID, regCode);
                            getContentResolver().update(DataProvider.CONTENT_URI_CONTACTS, values, select, null);
                            //DBG.write("Number of rows updated with new regId is " + String.valueOf(result));
                        } catch (SQLException sqle2)
                        {
                            DBG.write("GolgiService SQL Error!! in UPDATEORADCONTACT UPDATE Transaction normal Contact" + sqle.getMessage());
                        }
                    }
                }
            });


            // This is the transaction that receives the messages from other users
            golgiChatService.userMessageTransfer.registerReceiver(new golgiChatService.userMessageTransfer.RequestReceiver()
            {
                @Override
                public void receiveFrom(golgiChatService.userMessageTransfer.ResultSender rs, String userMessage, String userName, String userRegId, String fgn, String gn, GroupMembers gm, LocationInfo li, String ts)
                {
                    String capitalFirstLetter;

                    DBG.write("Received USERTRANSFER message with Data " + userMessage + " UserName = " + userName + " UserRegId = " + userRegId );
                    DBG.write("Received USERTRANSFER LOCATION INFORMATION LAT =  >" + li.getLat() + "< LON = >" + li.getLon() + "< TimeStamp = >" + li.getTime() + "<" );

                    SharedPreferences prefs = getSharedPreferences("GolgiChatPrefs", Context.MODE_PRIVATE);
                    boolean inFg = prefs.getBoolean("inFg", true);

                    // Send back ResultSender success code OK
                    RegCode rc = new RegCode();
//                    rc.setCode(1);  // Set status to delivered
//                    rs.success(rc);

                    // Condition for Special group
                    boolean isSpecialGroup = fgn.equals(gn);

                    // Check if it is a new contact or group, if so add to contacts Table
                    // Simply add contact to Table, if it is already there then there will just be an sql error
                    if (  !fgn.equals("") )
                    {
                        DBG.write("This is a message from a group , add group if it does not exist >" + fgn + "<");

                        rc.setCode(1);  // Set status to delivered
                        rs.success(rc);

                        try {
                            DBG.write("Writing Matched Username information to Contacts Table");
                            ContentValues values = new ContentValues();
                            //values.put(DataProvider.COL_NAME, email.substring(0, email.indexOf('@')));
                            values.put(DataProvider.COL_NAME, fgn);
                            values.put(DataProvider.COL_REGID, userRegId); // Should Be Empty


                            // This handles ALL MEMBERS GROUP
                            if (!isSpecialGroup)
                            {
                                    values.put(DataProvider.COL_GROUPNAME, gn);
                                    capitalFirstLetter = gn.substring(0, 1).toLowerCase();
                            } // Tab Change Listner
                            else // Special Group
                            {
                                values.put(DataProvider.COL_GROUPNAME, gn);
                                capitalFirstLetter = gn.substring(0, 1).toLowerCase();
                            }


                            String spaceSepStringOfContacts = "";
                            ArrayList<String> iList = gm.getIList();
                            for(String i : iList)
                                spaceSepStringOfContacts = spaceSepStringOfContacts + " " + i ;

                            String spaceSepStringOfRegids = "";
                            ArrayList<String> rList = gm.getRList();
                            for(String r : rList)
                                spaceSepStringOfRegids = spaceSepStringOfRegids + " " + r ;

                            // Tidy Up
                            spaceSepStringOfContacts.trim();
                            spaceSepStringOfRegids.trim();


                            values.put(DataProvider.COL_GROUPMEMBERS, spaceSepStringOfContacts);
                            values.put(DataProvider.COL_GROUPREGIDS, spaceSepStringOfRegids);

                            DBG.write("First Letter of Group Contact =  >" + capitalFirstLetter + "<");
                            values.put(DataProvider.COL_IMAGE_REF, capitalFirstLetter);

                            getContentResolver().insert(DataProvider.CONTENT_URI_CONTACTS, values);
                        } catch (SQLException sqle)
                        {
                            DBG.write("GolgiService SQL Error!! in USERMESSAGETRANSFER Transaction Group Contact " + sqle.getMessage());
                        }


                        // Write message for a group, need to use fgn for COL_FROM to tie message to group
                        try {

                            ContentValues values = new ContentValues();
                            values.put(DataProvider.COL_MSG, userMessage);
                            values.put(DataProvider.COL_FROM, fgn);
                            // Indicates actual sender of message
                            values.put(DataProvider.COL_FROM_IN_GROUP, userName);

                            // handles IOS when there is no TimeStamp yet filled out
                            if (ts.equals(""))
                            {
                                ts = DataProvider.getDateTime(DataProvider.dateType.MILLISECONDS);
                                li.setLon("0");
                                li.setLat("0");
                            }
                            values.put(DataProvider.COL_SEND_TIMESTAMP, Long.valueOf(ts));
                            values.put(DataProvider.COL_LATITUDE, li.getLat());
                            values.put(DataProvider.COL_LONGITUDE, li.getLon());
                            values.put(DataProvider.COL_LOCATION_TIMESTAMP,li.getTime());

                            // Just a flag in messages table to indicate a message from a group
                            values.put(DataProvider.COL_IS_GROUP_MESSAGE, "YES");
                            values.put(DataProvider.COL_DELIVERY_STATUS,    (userName.split("\\@"))[0]    ); // TODO: HACK for putting name of person who sent message in delivery status place!!

                            getContentResolver().insert(DataProvider.CONTENT_URI_MESSAGES, values);
                        } catch (SQLException sqle)
                        {
                            // Should Fail if the TimeStamp is not unique
                            DBG.write("GolgiService SQL Error!! in userMessageTransfer Transaction Group Conact " + sqle.getMessage());
                        }
                        // Only set a Notification if in the BackGround
                        if(!Common.inFg)
                        {
                            setMessageArrivedNotification("New Message From " + gn );
                        }
                    }
                    else   // Normal Contact Message
                    {
                        DBG.write("This is a message from a normal contact, add contact if it does not exist >" + userName + "<");

                        // Use 2 as a value to indicate message fragment for this Normal user is currently in view
                        // Which means we have seen this message
                        // Need extra check for Fragment to be currently visble and just in Memory!!
                        if (Common.contactNameCurrentlyInView.equals(userName) && (Common.isContactMessageFragmentVisible))
                        {
                            rc.setCode(2);  // Set status to SEEN
                            rs.success(rc);
                        }
                        else
                        {
                            rc.setCode(1);  // Set status to DELIVERED
                            rs.success(rc);
                        }

                        try {
                            DBG.write("Writing Matched Username information to Contacts Table");
                            ContentValues values = new ContentValues();
                            //values.put(DataProvider.COL_NAME, email.substring(0, email.indexOf('@')));
                            values.put(DataProvider.COL_NAME, userName);
                            values.put(DataProvider.COL_REGID, userRegId);

                            capitalFirstLetter = userName.substring(0, 1).toLowerCase();
                            DBG.write("First Letter of Contact =  >" + capitalFirstLetter + "<");
                            values.put(DataProvider.COL_IMAGE_REF, capitalFirstLetter);

                            getContentResolver().insert(DataProvider.CONTENT_URI_CONTACTS, values);
                        } catch (SQLException sqle)
                        {
                            // Not always as Conatact could be offline
                            DBG.write("GolgiService SQL Error!! in userMessageTransfer Transaction normal Contact" + sqle.getMessage());
                            try {
                               String select = "((" + DataProvider.COL_NAME + " = '" + userName + "'))";
                                ContentValues values = new ContentValues();
                                values.put(DataProvider.COL_REGID, userRegId);
                                getContentResolver().update(DataProvider.CONTENT_URI_CONTACTS, values, select, null);
                                //DBG.write("Number of rows updated with new incoming regId is " + String.valueOf(result));
                            } catch (SQLException sqle2)
                            {
                                DBG.write("GolgiService SQL Error!! in USERMESSAGETRANSFERMESSAGE UPDATE Transaction normal Contact" + sqle.getMessage());
                            }
                        }


                        // Write message for this user, use username for normal Contact
                        try {
                            ContentValues values = new ContentValues();
                            values.put(DataProvider.COL_MSG, userMessage);
                            values.put(DataProvider.COL_FROM, userName);

                            // handles IOS when there is no TimeStamp yet filled out
                            if (ts.equals(""))
                            {
                                ts = DataProvider.getDateTime(DataProvider.dateType.MILLISECONDS);
                            }
                            values.put(DataProvider.COL_SEND_TIMESTAMP, Long.valueOf(ts));

                            values.put(DataProvider.COL_LATITUDE, li.getLat());
                            values.put(DataProvider.COL_LONGITUDE, li.getLon());
                            values.put(DataProvider.COL_LOCATION_TIMESTAMP,li.getTime());

                            getContentResolver().insert(DataProvider.CONTENT_URI_MESSAGES, values);
                        } catch (SQLException sqle)
                        {
                            // Not always as Conatact could be offline
                            DBG.write("GolgiService SQL Error!! in userMessageTransfer Transaction Normal Contact " + sqle.getMessage());
                        }

                        // Only set a Notification if in the BackGround
                        if(!Common.inFg)
                        {
                            setMessageArrivedNotification("New Message From " + ((userName.split("\\@"))[0] ) );
                        }
                    }
                }
            });


            golgiChatService.typingStatus.registerReceiver(typingReceiver);


            registerGolgi(new GolgiAPIHandler() {
                              @Override
                              public void registerSuccess()
                              {
                                  SharedPreferences prefs = getSharedPreferences("GolgiChatPrefs", Context.MODE_PRIVATE);
                                  boolean alreadyRegisteredOnce = prefs.getBoolean("alreadyRegisteredOnce", false);
                                  String golgiChatServer = prefs.getString("golgiChatServer", "");
                                  //Common.alreadyRegisteredOnce++;
                                  DBG.write("Registered Sucessfully with Golgi as '" + Common.regId + "'" + " SharedPreferences.alreadyRegisteredOnce = " + String.valueOf(alreadyRegisteredOnce));

                                  if (!alreadyRegisteredOnce)
                                  {
                                      alreadyRegisteredOnce = true;
                                      SharedPreferences.Editor editor = prefs.edit();
                                      editor.putBoolean("alreadyRegisteredOnce" , alreadyRegisteredOnce);
                                      editor.commit();
                                  }
                              }
                              @Override
                              public void registerFailure ()
                              {
                                  DBG.write("Registration Failure with Golgi as '" + Common.regId + "'");
                              }
                          }
                    ,
                    GolgiKeys.DEV_KEY,
                    GolgiKeys.APP_KEY,
                    Common.regId
            );
        }
        else
        {
            DBG.write("ERROR: No RegId Id Set, Cannot Register !!");
        }
    }

    private void setMessageArrivedNotification(String textToNotify)
    {
        Intent notificationIntent = new Intent(getBaseContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);


        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getBaseContext());
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(notificationIntent);



        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        //PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, 0);


        Notification n = new Notification.Builder(this)
                .setContentTitle("New GolgiChat Message")
                .setContentText(textToNotify)
//                .setNumber(2)
                 .setSmallIcon(R.drawable.header_logo)
                 // .setSmallIcon(R.drawable.icon)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .setLights(Color.rgb(255,165,0), 500, 500)
                .build();


        //n.defaults |= Notification.DEFAULT_VIBRATE | Notification.FLAG_AUTO_CANCEL;
        n.defaults |=  Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify( Common.NOTIFICATION_ONE, n); // Unique ID for this Notification
    }


    public static void cancelNotification(Context ctx, int notifyId)
    {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }

    public static void gcsUserMessTransfer(String messageToSend, GroupMembers gm, String fromContact, String fromRegid, String fgn, String gn, String ts, LocationInfo locInfo, final Uri inserMessagetUri, String dri, final boolean isGroupContact, final Context context)
    {
        GolgiTransportOptions stdGto = new GolgiTransportOptions();
        stdGto.setValidityPeriod(100000); // One Day expiry Approx on Sending Messages

        golgiChatService.userMessageTransfer.sendTo(new golgiChatService.userMessageTransfer.ResultReceiver()
                                                    {
                                                        //private int messageSentCount = 1;
                                                        @Override
                                                        public void failure(GolgiException ex)
                                                        {
                                                            DBG.write("Send Failure on sending message to other user " + ex.getErrText());
                                                            if ( !isGroupContact) {
                                                                ContentValues values = new ContentValues();
                                                                values.put(DataProvider.COL_DELIVERY_STATUS, DataProvider.DELIVERY_STATUS_ERROR_SENDING);
                                                                context.getContentResolver().update(inserMessagetUri, values, null, null);
                                                            }
                                                        }

                                                        @Override
                                                        public void success(RegCode result)
                                                        {
                                                            // Delivered
                                                            DBG.write("Send Success on sending message to other user , Update Delivery Status of Message if Normal Contact with Result Code = >" + result.getCode() + "<");
                                                            // messageSentCount--;

                                                            // When all messages are Sucessfully Sent then can set status to Delivered
                                                            // Just do Delivery Receipts for Normal contacts at the moment until Threading Issue sorted out
                                                            if ( !isGroupContact)
                                                            {
                                                                try
                                                                {
                                                                    //String select = "((" + DataProvider.COL_ID + " = '" + userName + "'))";
                                                                    ContentValues values = new ContentValues();

                                                                    // Use the Result Code to show the delivery status of the snet Message
                                                                    // code -- 2 should mean that the recipient is currently viewing this Contact and has therefore seen the message
                                                                    if (result.getCode() == 1)
                                                                        values.put(DataProvider.COL_DELIVERY_STATUS, DataProvider.DELIVERY_STATUS_DELIVERED);
                                                                    else if (result.getCode() == 2)
                                                                        values.put(DataProvider.COL_DELIVERY_STATUS, DataProvider.DELIVERY_STATUS_SEEN);

                                                                    context.getContentResolver().update(inserMessagetUri, values, null, null);
                                                                } catch (SQLException sqle2)
                                                                {
                                                                    DBG.write("GolgiService SQL Error!! in userMessagetransfer UPDATE Delivery Status normal Contact" + sqle2.getMessage());
                                                                }
                                                            }
                                                        }
                                                    },
                stdGto,
                dri,   // Destination
                messageToSend,
                fromContact,
                fromRegid,
                fgn,
                gn,
                gm,
                locInfo,
                ts);
    }
}

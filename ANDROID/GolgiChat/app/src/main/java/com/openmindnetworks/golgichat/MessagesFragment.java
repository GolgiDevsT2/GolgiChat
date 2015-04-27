package com.openmindnetworks.golgichat;

import android.app.ActionBar;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.openmindnetworks.golgi.api.GolgiException;
import com.openmindnetworks.golgi.api.GolgiTransportOptions;
import com.openmindnetworks.golgichat.datamodel.DataProvider;
import com.openmindnetworks.golgichat.gen.GroupMembers;
import com.openmindnetworks.golgichat.gen.LocationInfo;
import com.openmindnetworks.golgichat.gen.golgiChatService;
import com.openmindnetworks.golgichat.utils.DBG;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by derekdoherty on 23/01/2015.
 */


public class MessagesFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    private SimpleCursorAdapter adapter;
    private EditText msgEdit;
    private Button sendBtn;

    private  String currentContact;
    private  boolean isGroupContact = false;
    private  boolean isSpecialGolgiChatGroup = false;
    public   String currentRegId;
    private  String currentGN;
    private  String currentSGM;
    private  String currentSGR;

    private static boolean canSendAnotherTypingMessageToOtherUser = true;

    private View footer;
    //private static TextView tvFooter; // TODO : Does this need to be static
    // These are Static so they can be used in th
    private static TextView tvFooterContact;
    private static TextView tvFooterGroup;

    private BroadcastReceiver UIAccessReceiver;

    private  Cursor listOfContacts = null;

    private int messageSentCountOuterClass = 0;

    private GolgiTransportOptions stdGto;

    private static final String CURRENTCONTACT = "currentContact";
    private static final String CURRENTREGID = "currentRegId";
    private static final String ISGROUPCONTACT = "isGroupContact";
    private static final String ISSPECIALGOLGICHATGROUP = "isSpecialGolgiChatGroup";
    private static final String CURRENTGN = "currentGN";
    private static final String CURRENTSGM = "currentSGM";
    private static final String CURRENTSGR = "currentSGR";


    // This is the correct way to instansiate a fragment instead of an overloaded constructer
    public static MessagesFragment newInstance(String selectedName, String regId, boolean igc, boolean isgcg,   String gn, String sgm, String sgr)
    {
        DBG.write("MessagesFragment.newInstance() called ");

        MessagesFragment fragment = new MessagesFragment();

        Bundle args = new Bundle();

        args.putString(CURRENTCONTACT, selectedName);
        args.putString(CURRENTREGID, regId);
        args.putBoolean(ISGROUPCONTACT, igc);
        args.putBoolean(ISSPECIALGOLGICHATGROUP,  isgcg);
        args.putString(CURRENTGN, gn);
        args.putString(CURRENTSGM, sgm);
        args.putString(CURRENTSGR, sgr);

        fragment.setArguments(args);
        return fragment;
    }
    // This default constructer is needed by default
    public MessagesFragment()
    {

    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater)
    {
        //this.optionsMenu = menu;
        inflater.inflate(R.menu.menu_normal_contact_message, menu);
    }

    final static String CURRENTMAPCONTACT = "com.openmindnetworks.golgichat.MessagesFragment.CURRENTMAPCONTACT";
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_location:

                DBG.write("----------------------------------------------------------------------------------------------------------");
                DBG.write("MessagesFragment.onOptionsItemSelected() ACTION_LOCATION");
                DBG.write("-----------------------------------------------------------------------------------------------------------");

                // Starting the Login Activity
                Intent intent = new Intent(getActivity(), LocationActivity.class);
                intent.putExtra(CURRENTMAPCONTACT, currentContact); //
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        DBG.write("MessagesFragment.onCreateView()");
        return inflater.inflate(R.layout.activity_chat, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // If the fragment is destroyed and re created then we get these variables properly re initialized
        currentContact = getArguments().getString(CURRENTCONTACT);
        currentRegId =  getArguments().getString(CURRENTREGID);
        isGroupContact = getArguments().getBoolean(ISGROUPCONTACT);
        currentGN = getArguments().getString(CURRENTGN);
        isSpecialGolgiChatGroup = getArguments().getBoolean(ISSPECIALGOLGICHATGROUP);
        currentSGM = getArguments().getString(CURRENTSGM);
        currentSGR = getArguments().getString(CURRENTSGR);

        //  TODO: Not sure if should use retainInstance here but seems to be faster when enabled !!
        //  Possible that it is not correct when loaders are present
        //  setRetainInstance(true);
        //  setHasOptionsMenu(true);

        if (isSpecialGolgiChatGroup)
        {
            getLoaderManager().initLoader(1, null, this);  // List of all Contacts for sending to all member group
        }

        adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.chat_list_item,
                null,
                new String[]{DataProvider.COL_MSG, DataProvider.COL_AT, DataProvider.COL_DELIVERY_STATUS},
                new int[]{R.id.text1, R.id.text2, R.id.text3},
                0);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
        {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex)
            {
                switch(view.getId())
                {
                    case R.id.text1:
                        LinearLayout root = (LinearLayout) view.getParent().getParent();
                        LinearLayout underRoot = (LinearLayout) view.getParent();
                        if (cursor.getString(cursor.getColumnIndex(DataProvider.COL_FROM)) == null)
                        {
                            root.setGravity(Gravity.RIGHT);
                            root.setPadding(50, 10, 10, 10);
                            underRoot.setBackgroundResource(R.drawable.box_blue);
                        } else
                        {
                            root.setGravity(Gravity.LEFT);
                            root.setPadding(10, 10, 50, 10);
                            underRoot.setBackgroundResource(R.drawable.box_white);
                        }
                        break;
                }
                return false;
            }
        });
        setListAdapter(adapter);


        UIAccessReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // do something here.
                String ttdf = intent.getStringExtra(GolgiService.TEXT_TO_DISPLAY);
                String contactTyping = intent.getStringExtra(GolgiService.REGNAME);

                //DBG.write("Received information in INTENT onReceive  TTD = >" + ttdf + "< REGNAME = >" + contactTyping + "< ");

                if (!isGroupContact)
                {
                    if (currentContact.equals(contactTyping))
                    {
                        handler.removeMessages(EVENT1); // if we get a new Typing status, can cancel any previous
                        // Send an Event to reset text  via Handler
                        // Start a Timer
                        Message msg = handler.obtainMessage(EVENT1);
                        handler.sendMessageDelayed(msg, 1500); // Set Delay of 3 Seconds

                        tvFooterContact.setText(ttdf);
                    }
                }
            }
        };
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    private void sendTypingStatus (String cc, String ri, String tts)
    {
        stdGto = new GolgiTransportOptions();
        stdGto.setValidityPeriod(2);
        stdGto.setHighPriority();

        // Need to send empty string case always as it indicates message sending
        if (canSendAnotherTypingMessageToOtherUser || tts.equals(""))
        {
            // Setting this to prevent further messages been sent until timer expires
            canSendAnotherTypingMessageToOtherUser = false;

            // Reset the canSendAnotherTypingMessageToOtherUser static variable back to true after one second
            handler.removeMessages(EVENT2);
            Message msg = handler.obtainMessage(EVENT2);
            handler.sendMessageDelayed(msg, 2500); // Set Delay of 1 Seconds


            //DBG.write("sendTypingStatus() sending from " + cc + " Regid = " + ri + " TextToSend = " + tts);
            golgiChatService.typingStatus.sendTo(new golgiChatService.typingStatus.ResultReceiver() {
                                                     @Override
                                                     public void failure(GolgiException ex) {
                                                         DBG.write("Send Failure on sending TypingStatus error =  " + ex.getMessage() + ex.getCause());
                                                     }

                                                     @Override
                                                     public void success() {
                                                         //DBG.write("Send Success on sending TypingStatus  =  ");
                                                     }
                                                 },
                    stdGto,
                    ri,
                    cc,
                    tts);
        }
    }


    public void sendAckedSeenStatus (String me, String ri, String asts)
    {
        stdGto = new GolgiTransportOptions();
        stdGto.setValidityPeriod(1);
        //stdGto.setHighPriority();

        DBG.write("sendAckedSeenStatus() sending from " + me + " to Regid = " + ri + " ackedSeenTimeStamp = " + asts);

        golgiChatService.seenReceipt.sendTo(new golgiChatService.seenReceipt.ResultReceiver() {
                                                @Override
                                                public void failure(GolgiException ex) {
                                                    DBG.write("Send Failure on sending AckedSeenStatus error =  " + ex.getMessage() + ex.getCause());
                                                }

                                                @Override
                                                public void success() {
                                                    //DBG.write("Send Success on sending AckedSeenStatus  =  ");
                                                }
                                            },
                stdGto,
                ri,
                me,
                asts);
    }



    private final static int EVENT1 = 1;
    private final static int EVENT2 = 2;
    private   static Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case EVENT1:

                    if (tvFooterContact != null)
                         tvFooterContact.setText("");

                    if (tvFooterGroup != null)
                         tvFooterGroup.setText("");

                    //DBG.write("Clear Typing Status Footer");
                    //Toast.makeText(MyActivity.this, "Event 1", Toast.LENGTH_SHORT).show();
                    break;

                case EVENT2:

                    DBG.write("Reset to true the canSendTypingMessageToOtherUser ......");
                    //Toast.makeText(MyActivity.this, "Event 1", Toast.LENGTH_SHORT).show();
                    canSendAnotherTypingMessageToOtherUser = true;
                    break;

                default:
                    DBG.write("This should'nt happen!!");
                    //Toast.makeText(MyActivity.this, "Unhandled", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        DBG.write("MessagesFragment.onActvityCreated()");

        ListView listView = getListView();

        footer = getActivity().getLayoutInflater().inflate(R.layout.footer_for_type_ahead, null);
        listView.setFooterDividersEnabled(true);
        listView.addFooterView(footer);

        // Only want to set this static variable for a Normal contact MessageFragment
        // Need Handler to be static hence this variable needs to be static too
        if (!isGroupContact) {
            tvFooterContact = (TextView) ((LinearLayout) footer).getChildAt(0); // Hack to get view

            if (tvFooterContact != null)
               tvFooterContact.setText("");
        }
        else
        {
            tvFooterGroup = (TextView) ((LinearLayout) footer).getChildAt(0); // Hack to get view

            if (tvFooterGroup != null)
                tvFooterGroup.setText("");
        }

        msgEdit = (EditText) getView().findViewById(R.id.msg_edit);
        sendBtn = (Button) getView().findViewById(R.id.send_btn);

        // Add Some listeners for Edit Text Field
        msgEdit.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
               // DBG.write("beforeTextChanged");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
               // DBG.write("onTextChanged");
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                if (!isGroupContact)
                {
                    //DBG.write("afterTextChanged for Normal Contact");

                    if (!s.toString().trim().equals(""))
                    {
                        // TODO: Maybe make this opearation global instead of computing each time
                        sendTypingStatus(Common.nickName, currentRegId, (Common.nickName.split("\\@"))[0] + " is typing...");
                    } else
                    {
                        // TODO: Not Sure if I need this , as timer on other side will remove after two seconds
                        sendTypingStatus(Common.nickName, currentRegId, "");
                    }
                }
            }
        });


        sendBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                DBG.write("sendBtn.onClickView()");
                String messageToSend = msgEdit.getText().toString();

                // Check for at least one ASCII Character in send field
                // if ( !(messageToSend.matches(".*\\w.*")) )
                if (  messageToSend.isEmpty() || messageToSend.trim().isEmpty()    )
                {
                    msgEdit.setText(null);
                    return;
                }

                stdGto = new GolgiTransportOptions();
                stdGto.setValidityPeriod(100000); // One Day expiry Approx on Sending Messages

                // Bit of a Gotcha here as there needs to be at least one member present in each list
                // for there not to be an error in the sending
                GroupMembers gm = new GroupMembers();
                ArrayList<String> li = new ArrayList<String>();
                ArrayList<String> ri = new ArrayList<String>();


                String destRegid;
                String fromContact;
                String fromRegid;
                String fgn;
                String gn;
                String ts = DataProvider.getDateTime(DataProvider.dateType.MILLISECONDS); // Time Message Sent !!


                LocationInfo locInfo = new LocationInfo();
                locInfo.setLat(String.valueOf(Common.myLatestLat));
                locInfo.setLon(String.valueOf(Common.myLatestLong));
                locInfo.setTime(String.valueOf(Common.myLatestLocationTimeStamp));



                // Create List of Regids to send to
                ArrayList<String> destRegidsList = new ArrayList<String>(); // Only really need to fill this out for compbatability with IOS
                                                                            // On Android need to fetch potentially updated RegIds from unique FGN
                                                                            // Regids in this list are essentially static from creation time
                ArrayList<String> destContactidsList = new ArrayList<String>();


                if (isGroupContact)
                {

                    if (isSpecialGolgiChatGroup)
                    {
                        DBG.write("Sending to a special Group Contact >" + currentContact);

                        fromContact = Common.nickName;
                        fromRegid = Common.regId;
                        fgn = currentContact;
                        gn = currentGN;


                        // Need to fill out these arrays for the special group
                        // Retreive all current contacts and fill out array
                        // Create space seperated string which can then be parsed
                        String spaceSepStringOfContacts = "";
                        String spaceSepStringOfRegids = "";

                        if (listOfContacts != null)
                        {
                            DBG.write("Cursor count for ALL_MEMBERS_GROUP = " + String.valueOf(listOfContacts.getCount()));
                            //listOfContacts.moveToFirst();
                            listOfContacts.moveToPosition(-1);  // Seem to need this to bring cursor back to beginning again
                            while (listOfContacts.moveToNext()) {
                                DBG.write(" Cursor contents  for ALL_MEMBERS_GROUP = " + listOfContacts.getString(listOfContacts.getColumnIndex(DataProvider.COL_NAME)));

                                String selectedName = (listOfContacts.getString(listOfContacts.getColumnIndex(DataProvider.COL_NAME)));
                                String selectedRegid = (listOfContacts.getString(listOfContacts.getColumnIndex(DataProvider.COL_REGID)));

                                spaceSepStringOfContacts = spaceSepStringOfContacts + " " + selectedName;
                                spaceSepStringOfRegids = spaceSepStringOfRegids + " " + selectedRegid;
                            }
                        }

                        // Need to add Myself to list of Group Contacts
                        spaceSepStringOfContacts = spaceSepStringOfContacts + " " + Common.nickName;
                        spaceSepStringOfRegids = spaceSepStringOfRegids + " " + Common.regId;

                        spaceSepStringOfContacts.trim();
                        spaceSepStringOfRegids.trim();


                        // Parse Space seperated strings
                        destContactidsList = new ArrayList(Arrays.asList(spaceSepStringOfContacts.trim().split(" ")));
                        destRegidsList = new ArrayList(Arrays.asList(spaceSepStringOfRegids.trim().split(" ")));


                        gm.setIList(destContactidsList);
                        gm.setRList(destRegidsList);

                        // Indicates Number of Recipients for this message in a Group
                        messageSentCountOuterClass = destContactidsList.size() - 1;
                    }
                    else // Ordinary Group
                    {
                        DBG.write("Sending to an ordinary Group Contact");

                        fromContact = Common.nickName;
                        fromRegid = Common.regId;
                        fgn = currentContact;
                        gn = currentGN;

                        destContactidsList = new ArrayList(Arrays.asList(currentSGM.trim().split(" ")));
                        destRegidsList = new ArrayList(Arrays.asList(currentSGR.trim().split(" ")));

                        gm.setIList(destContactidsList);
                        gm.setRList(destRegidsList);

                        // Indicates Number of Recipients for this message in a Group
                        messageSentCountOuterClass = destContactidsList.size() - 1;
                    }
                }
                else // Normal Conatact
                {
                    DBG.write("Sending to A Normal Contact");
                    destRegid = currentRegId;
                    fromContact = Common.nickName;
                    fromRegid = Common.regId;
                    fgn = "";
                    gn = "";

                    li.add("Hello");  // Dummy
                    ri.add("Hello Too"); // Dummy

                    gm.setIList(li);
                    gm.setRList(ri);

                    destRegidsList.add(destRegid);
                    destContactidsList.add(currentContact);

                    // For a Normal Contact, this value is always One
                    messageSentCountOuterClass = 1;

                }
                for (String s : destContactidsList)
                {
                    DBG.write("BEFORE SENDING Contacts in List is >" + s + "<" );
                }
                for (String r : destRegidsList)
                {
                    DBG.write("BEFORE SENDING RegIds in List is >" + r + "<" );
                }

                // Save message to Database BEFORE Sending, Does'nt make any Difference
                ContentValues values = new ContentValues();
                values.put(DataProvider.COL_MSG, msgEdit.getText().toString());
                values.put(DataProvider.COL_TO, currentContact);
                values.put(DataProvider.COL_SEND_TIMESTAMP, ts);


                if (isGroupContact) {
                    // Just a flag in messages table to indicate a message from a group
                    values.put(DataProvider.COL_IS_GROUP_MESSAGE, "YES");
                }

                final Uri inserMessagetUri = getActivity().getContentResolver().insert(DataProvider.CONTENT_URI_MESSAGES, values);

                msgEdit.setText(null);

                // And finally send !!
                String dri = "";

                for(String dcil : destContactidsList)
                {
                    // Send to all members in Destination list for Group except MySelf
                    if ( !(dcil.equals(Common.nickName)) )
                    {
                        // Need to Query DB directly to get Latest RegId for this contact
                        // TODO: Maybe update the COL_GROUPREGID list here too when sending - Complicated !!

                        String select = "((" + DataProvider.COL_NAME + " = '" + dcil + "'))";
                        DBG.write("MessagesFragment Getting RegId for this User Select statement is = " + select);

                        Cursor data = getActivity().getContentResolver().query(
                                DataProvider.CONTENT_URI_CONTACTS,
                                new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_REGID},
                                select,
                                null,
                                DataProvider.COL_ID + " ASC");


                        data.moveToFirst();
                        while(!data.isAfterLast())
                        {
                            dri = data.getString( data.getColumnIndex(DataProvider.COL_REGID));
                            DBG.write("MessagesFragment() Iterating through cursor of returned selected names = " + dri);
                            data.moveToNext();
                        }
                        // Need to close Cursor
                        data.close();


                        DBG.write("Sending to Name >" + dcil + "< ");
                        DBG.write("Sending to RegId >" + dri + "< ");

                        // Need to check for empty DA, Could be the cause of an error
                        if (dri.equals(""))
                        {
                            DBG.write("BIG ERROR HERE in MessagesFragment, SENDING TO AN EMPTY DA !!");
                        }

                        GolgiService.gcsUserMessTransfer(messageToSend, gm, fromContact, fromRegid, fgn, gn, ts, locInfo, inserMessagetUri, dri, isGroupContact, getActivity());
                    }
                }
            }
        });

        getLoaderManager().initLoader(0, null, this);  // Messages for this Contact

    }

    @Override
    public void onPause()
    {
        DBG.write("MessagesFragment.onPause() object is = >" + this.toString() + "<");

        // Need to reset New Message Count for this contact
        String select = "((" + DataProvider.COL_NAME + " = '" + currentContact + "'))";
        ContentValues values = new ContentValues(1);
        values.put(DataProvider.COL_COUNT, 0 );
        int result = getActivity().getContentResolver().update(DataProvider.CONTENT_URI_CONTACTS, values, select,null);
        DBG.write("Number of rows updated with new count is " + String.valueOf(result));


        if(!isGroupContact)
        {
            Common.contactNameCurrentlyInView = "";
        }
        else
        {
            Common.groupContactCurrentlyInView = "";
        }

        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(msgEdit.getWindowToken(), 0);

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(UIAccessReceiver);

        super.onPause();
    }
    @Override
    public void onResume()
    {
        DBG.write("MessagesFragment.onResume(), isGroupContact = >" + isGroupContact + "<");

        // Need to do this as when we resume into the Messages view
        // The HOME button needs to be reInstated
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar == null)
        {
            DBG.write("SORT OF ERROR, MessagesFragment.onResume(), ActionBar returning null, Can't set Home Button");
        }
        else {
            getActivity().getActionBar().setHomeButtonEnabled(true);
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //Also Title name needs to be re instated
        if (actionBar == null || currentGN == null || currentContact == null)
        {
            DBG.write("SORT OF ERROR, MessagesFragment.onActvityCreated(), ActionBar, currentGN or currentContact returning null, Can't set Titles");
        }
        else
        {
            if(isGroupContact)
            {
                //actionBar.setTitle((Common.contactNameCurrentlyInView.split("\\@"))[0]);
                actionBar.setTitle(currentGN);
            }
            else
            {
                //actionBar.setTitle((Common.contactNameCurrentlyInView.split("\\@"))[0]);
                actionBar.setTitle((currentContact.split("\\@"))[0]);
            }
            actionBar.setSubtitle("Online");
        }


        ListView listView = getListView();
        listView.setSelection(adapter.getCount() - 1);

        if(!isGroupContact) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(UIAccessReceiver, new IntentFilter(GolgiService.DOORMAN_ACCESS_INTENT));
        }
        // Send acked_seen message to this current contact to indicate that all messages up to this point have been seen
        // Don't send for groups for the moment

        if(!isGroupContact)
        {
            Common.contactNameCurrentlyInView = currentContact;
            String ts = DataProvider.getDateTime(DataProvider.dateType.MILLISECONDS);
            sendAckedSeenStatus(Common.nickName,currentRegId, ts);
        }
        else
        {
            Common.groupContactCurrentlyInView = currentGN;
        }
        super.onResume();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        CursorLoader loader;
        if (id == 0) {
            // Select string so currentContact is either in the FROM or TO column
            String select = "((" + DataProvider.COL_FROM + " = '" + currentContact + "') OR (" + DataProvider.COL_TO + " = '" + currentContact + "' ))";

            loader = new CursorLoader(this.getActivity(),
                    DataProvider.CONTENT_URI_MESSAGES,
                    new String[]{DataProvider.COL_ID, DataProvider.COL_MSG, DataProvider.COL_FROM, DataProvider.COL_TO, DataProvider.COL_AT, DataProvider.COL_DELIVERY_STATUS, DataProvider.COL_SEND_TIMESTAMP},
                    select,
                    null,
                    //              DataProvider.COL_ID + " ASC"); // Indicates ordering of Cursor
                    DataProvider.COL_SEND_TIMESTAMP + " ASC"); // Allows sorting of messages based on time sent
        }
        else
        {
            // Select string where GroupName is empty means it is a normal contact
            String select = "(" + DataProvider.COL_GROUPNAME + " = '') ";

            loader = new CursorLoader(this.getActivity(),
                    DataProvider.CONTENT_URI_CONTACTS,
                    new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_REGID, DataProvider.COL_COUNT, DataProvider.COL_ISCHECKED},
                    select,
                    null,
                    DataProvider.COL_NAME + " ASC");

        }

        return loader;
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data)
    {
        switch (loader.getId())
        {
            case 0:
                adapter.swapCursor(data);
                ListView listView = getListView();
                listView.setSelection(adapter.getCount() - 1);
                //listView.setSelection(0);
                break;
            case 1:
                listOfContacts = data;
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        switch (loader.getId())
        {
            case 0:
                adapter.swapCursor(null);
                break;
            case 1:
                listOfContacts = null;
                break;
            default:
                break;
        }
    }

    // This should be set to Cursor _id of contact selected in ListFragment
    public void setContactInfoSelected(String selectedName, String regId, boolean igc, boolean isgcg,   String gn, String sgm, String sgr)
    {
        DBG.write("MessagesFragment.setContactNameSelected() called " + selectedName );
        currentContact = selectedName;
        currentRegId = regId;
        isGroupContact = igc;
        currentGN = gn;
        //currentFGN = fgn;
        currentSGM = sgm;
        currentSGR = sgr;
        isSpecialGolgiChatGroup = isgcg;
    }
}

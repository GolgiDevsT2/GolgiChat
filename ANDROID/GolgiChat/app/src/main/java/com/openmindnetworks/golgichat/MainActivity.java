package com.openmindnetworks.golgichat;

import android.app.ActionBar;
import android.app.Activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.openmindnetworks.golgi.api.GolgiAPI;
import com.openmindnetworks.golgichat.datamodel.DataProvider;
import com.openmindnetworks.golgichat.utils.DBG;

import android.support.v13.app.FragmentPagerAdapter;


import java.util.Locale;


public class MainActivity extends Activity implements ContactsListFragment.OnItemSelectedListener,
                                                      GroupContactsListFragment.OnGroupItemSelectedListener
                                                      ,GoogleApiClient.ConnectionCallbacks
                                                      ,GoogleApiClient.OnConnectionFailedListener
                                                      ,com.google.android.gms.location.LocationListener
{
    //private static Object syncObj = new Object();
    private BroadcastReceiver UIAccessReceiver;

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

    //Provides the entry point to Google Play services.
    protected GoogleApiClient mGoogleApiClient;

    //Represents a geographical location
    protected Location mLastLocation;


    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        static final int NUM_ITEMS = 2;
        private final FragmentManager mFragmentManager;

        public Fragment mFragmentAtPos0;
        public Fragment mFragmentAtPos1;
        private  int iteration = 0; // Hack to get position in getItemPosition function

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
            mFragmentManager = fm;
        }

        @Override
        public Fragment getItem(int position)
        {
            DBG.write("SectionsPagerAdapter.getItem(), position = >" + position + "<");
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).

            if (position == 0)
            {
                if (mFragmentAtPos0 == null)
                {
                    mFragmentAtPos0 = ContactsListFragment.newInstance(1);
                }
                return mFragmentAtPos0;
            }
            else if (position == 1)
            {
                if (mFragmentAtPos1 == null)
                {
                    mFragmentAtPos1 = GroupContactsListFragment.newInstance(1);
                }
                return mFragmentAtPos1;
            }
            return null;
        }

        public void onSwitchToMessagesFragment(String selectedName, String regId, boolean igc, boolean isgcg,   String gn, String sgm, String sgr)
        {
            DBG.write("SectionsPagerAdapter.onSwitchToMessagesFragment() Common.ContactsTabView = >" + Common.ContactsTabView + "<");

            if (Common.ContactsTabView)
            {
                mFragmentManager.beginTransaction().remove(mFragmentAtPos0).commit();
                if (mFragmentAtPos0 instanceof ContactsListFragment)
                {
                    mFragmentAtPos0 = MessagesFragment.newInstance(selectedName, regId, igc, isgcg, gn, sgm, sgr);
                    //((MessagesFragment)(mFragmentAtPos0)).setContactInfoSelected(selectedName, regId, igc, isgcg, gn, sgm, sgr);
                    Common.isContactMessageFragmentVisible = true;
                } else
                {
                    mFragmentAtPos0 = ContactsListFragment.newInstance(1);
                    Common.isContactMessageFragmentVisible = false;
                }
            }
            else
            {
                mFragmentManager.beginTransaction().remove(mFragmentAtPos1).commit();
                if (mFragmentAtPos1 instanceof GroupContactsListFragment)
                {
                    mFragmentAtPos1 = MessagesFragment.newInstance(selectedName, regId, igc, isgcg, gn, sgm, sgr);
                    //((MessagesFragment)(mFragmentAtPos1)).setContactInfoSelected(selectedName, regId, igc, isgcg, gn, sgm, sgr);
                    Common.isContactMessageFragmentVisible = false;
                } else
                {
                    mFragmentAtPos1 = GroupContactsListFragment.newInstance(1);
                    Common.isContactMessageFragmentVisible = false;
                }

            }
            iteration = 0; // HACK
            notifyDataSetChanged();
        }

        @Override
        public int getCount()
        {
            return NUM_ITEMS;
        }

        @Override
        public int getItemPosition(Object object)
        {
            //DBG.write("SectionsPagerAdapter.getItemPosition(), THING = >" + object.toString() + "<" + " mFragmentAtPos0 = >"
            //        + mFragmentAtPos0.toString() + "<" + "<" + " mFragmentAtPos1 = >" + mFragmentAtPos1.toString() + "<");
            //if (object instanceof FirstPageFragment && mFragmentAtPos0 instanceof NextFragment)

            iteration++;
            if (iteration == 1) // Noraml Contacts
            {
                if (object instanceof ContactsListFragment && mFragmentAtPos0 instanceof MessagesFragment) {
                    DBG.write("ContactsListFragment && mFragmentAtPos0 instanceof MessagesFragment POSITION_NONE");
                    return POSITION_NONE;
                } else if (object instanceof MessagesFragment && mFragmentAtPos0 instanceof ContactsListFragment) {
                    DBG.write("MessagesFragment && mFragmentAtPos0 instanceof ContactsListFragment POSITION_NONE");
                    return POSITION_NONE;
                }
            }
            else if (iteration == 2) // Groups
            {

                if (object instanceof GroupContactsListFragment && mFragmentAtPos1 instanceof MessagesFragment) {
                    DBG.write("GroupContactsListFragment && mFragmentAtPos1 instanceof MessagesFragment POSITION_NONE");
                    return POSITION_NONE;
                } else if (object instanceof MessagesFragment && mFragmentAtPos1 instanceof GroupContactsListFragment) {
                    DBG.write("MessagesFragment && mFragmentAtPos1 instanceof GroupContactsListFragment POSITION_NONE");
                    return POSITION_NONE;
                }
            }
            DBG.write("POSITION_UNCHANGED");
            return POSITION_UNCHANGED;
        }




        @Override
        public CharSequence getPageTitle(int position)
        {
            Locale l = Locale.getDefault();
            switch (position)
            {
                case 0:
                    return ("CONTACTS" )  ;
                case 1:
                    return ("GROUPS" );
            }
            return null;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        DBG.write("MainActivity.onCreate Called OUTSIDE savedInstance statement");

        final ActionBar actionBar = getActionBar();

//        Drawable d = getResources().getDrawable(R.drawable.box_orange);
//        actionBar.setBackgroundDrawable(d);

        // When we are running in the foreground it is ok to keep TCP/IP
        // connections alive.
        GolgiAPI.usePersistentConnection();
        Common.inFg = true;
        //GolgiService.setMainActivity(this);


            // Specify that tabs should be displayed in the action bar.

actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            //DBG.write("MainActivity.onCreate Called inside SAVEDINSTANCE statement");
            if (!GolgiService.isRunning(this))
            {
                DBG.write("Start GolgiService");
                GolgiService.startService(MainActivity.this);
            } else
            {
                DBG.write("GolgiService already started");
            }


            setContentView(R.layout.activity_main_new);

            //======================================================================================
            // Create the adapter that will return a fragment for each of the two
            // primary sections of the activity.
            mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());


            // Set up the ViewPager with the sections adapter.
            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setAdapter(mSectionsPagerAdapter);

            // TODO: Just testing this setting of 2, meant to improve smoothness
            mViewPager.setOffscreenPageLimit(2);

            mViewPager.setOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
                        @Override
                        public void onPageSelected(int position) {
                            // When swiping between pages, select the
                            // corresponding tab.
                            getActionBar().setSelectedNavigationItem(position);
                        }
                    });

            //======================================================================================

        // Need this check so as not to create multiple fragments
        if (savedInstanceState == null)
        {

            // Create a tab listener that is called when the user changes tabs.
            ActionBar.TabListener tabListener = new ActionBar.TabListener()
            {
                public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft)
                {
                    DBG.write("onTabSelected " + tab.getText());

                    // When the tab is selected, switch to the
                    // corresponding page in the ViewPager.
                    mViewPager.setCurrentItem(tab.getPosition());

                    // If we clicked on Group TAB
                    if(tab.getPosition() == 1)
                    {
                        Common.ContactsTabView = false;
                        // If the fragment currently displayed is the Messages Fragment
                        if (mSectionsPagerAdapter.mFragmentAtPos1 instanceof MessagesFragment)
                        {
                            if (getActionBar() != null) {
                                getActionBar().setHomeButtonEnabled(true);
                                getActionBar().setDisplayHomeAsUpEnabled(true);

                                getActionBar().setTitle((Common.groupContactCurrentlyInView.split("\\@"))[0]);
                                getActionBar().setSubtitle("Online");
                            }
                            // Need this for ackSeen in GolgiService
                            Common.isContactMessageFragmentVisible = false;
                        }
                        // List of Groups view
                        else
                        {
                            if (getActionBar() != null)
                            {
                                getActionBar().setHomeButtonEnabled(false);
                                getActionBar().setDisplayHomeAsUpEnabled(false);

                                getActionBar().setTitle((Common.nickName.split("\\@"))[0]);
                                getActionBar().setSubtitle("Online");
                            }
                            // Need this for ackSeen in GolgiService
                            Common.isContactMessageFragmentVisible = false;

                        }
                    }
                    // If we clicked on Contacts TAB
                    else if(tab.getPosition() == 0)
                    {
                        Common.ContactsTabView = true;
                        // If the fragment currently displayed is the Messages Fragment
                        if (mSectionsPagerAdapter.mFragmentAtPos0 instanceof MessagesFragment)
                        {
                            if (getActionBar() != null) {
                                getActionBar().setHomeButtonEnabled(true);
                                getActionBar().setDisplayHomeAsUpEnabled(true);

                                getActionBar().setTitle((Common.contactNameCurrentlyInView.split("\\@"))[0]);
                                getActionBar().setSubtitle("Online");
                            }
                            // Need this for ackSeen in GolgiService
                            Common.isContactMessageFragmentVisible = true;

                            // Need this in this Special case where tabbing over to a contact message fragment
                            // that is in viewPager Memory
                            String curRegId = ((MessagesFragment) mSectionsPagerAdapter.mFragmentAtPos0).currentRegId;
                            String ts = DataProvider.getDateTime(DataProvider.dateType.MILLISECONDS);
                            ((MessagesFragment) mSectionsPagerAdapter.mFragmentAtPos0).sendAckedSeenStatus(Common.nickName, curRegId, ts);
                        }
                        else // Normal Contact list in View
                        {
                            if (getActionBar() != null)
                            {
                                getActionBar().setHomeButtonEnabled(false);
                                getActionBar().setDisplayHomeAsUpEnabled(false);

                                getActionBar().setTitle((Common.nickName.split("\\@"))[0]);
                                getActionBar().setSubtitle("Online");
                            }
                            // Need this for ackSeen in GolgiService
                            Common.isContactMessageFragmentVisible = false;
                        }
                    }


       //             if (tab.getText().equals("Groups") )
       //             {
//                        FragmentManager fm = getFragmentManager();
//                        GroupContactsListFragment gclf = (GroupContactsListFragment) fm.findFragmentByTag("GCLF");
//
//                        //fm.popBackStack("contacts", 0);
//                        // If the Fragment is non-null, then it is currently being
//                        // retained across a configuration change.
//                        if ( gclf == null)
//                        {
//                            gclf = new GroupContactsListFragment();
//                            fm.beginTransaction().replace(R.id.fragment_placeholder, gclf, "GCLF")
//                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                                    .commit();
                       // mSectionsPagerAdapter.onTabSwitch(1);
//                        }
         //               Common.ContactsTabView = false;
         //           }

         //           else if (tab.getText().equals("Contacts") )
         //           {
//                        FragmentManager fm = getFragmentManager();
//                        ContactsListFragment clf = (ContactsListFragment) fm.findFragmentByTag("CLF");
//
//                        // If the Fragment is non-null, then it is currently being
//                        // retained across a configuration change.
//                        if ( clf == null)
//                        {
//                            clf = new ContactsListFragment();
//                            fm.beginTransaction().replace(R.id.fragment_placeholder, clf, "CLF")
//                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                                    .commit();
//                        }
        //                Common.ContactsTabView = true;
        //            }
                }  // onTabSelected

                public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft)
                {
                    // hide the given tab
                    DBG.write("onTabUnSelected" + tab.getText());
                    if (tab.getText().equals("Groups") )
                    {
//                        FragmentManager fm = getFragmentManager();
//                        GroupContactsListFragment gclf = (GroupContactsListFragment) fm.findFragmentByTag("GCLF");
//
//
//
//                        // If the Fragment is non-null, then it is currently being
//                        // retained across a configuration change.
//                        if ( gclf != null)
//                        {
//                                                      //gclf = new GroupContactsListFragment();
//                            fm.beginTransaction().remove( gclf).commit();
//                                                     //ft.replace(R.id.fragment_placeholder, gclf, "GCLF").commit();
//                        }
                    }

                    else if (tab.getText().equals("Contacts") )
                    {
//                        FragmentManager fm = getFragmentManager();
//                        ContactsListFragment clf = (ContactsListFragment) fm.findFragmentByTag("CLF");
//
//                        // If the Fragment is non-null, then it is currently being
//                        // retained across a configuration change.
//                        if ( clf != null)
//                        {
//                                                   //clf = new ContactsListFragment();
//                            fm.beginTransaction().remove( clf).commit();
//                                                   //ft.add(R.id.fragment_placeholder, clf, "CLF").commit();
//                        }
                    }
                }
                public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                    // probably ignore this event
                    DBG.write("onTabReselected" + tab.getText());
                }
            };

            // Add 2 tabs, specifying the tab's text and TabListener
            actionBar.addTab(
                    actionBar.newTab()
                            .setText("Contacts")
                            .setTabListener(tabListener));
            actionBar.addTab(
                    actionBar.newTab()
                            .setText("Groups")
                            .setTabListener(tabListener));


            if (getActionBar() != null)
            {
                getActionBar().setHomeButtonEnabled(false);
                getActionBar().setDisplayHomeAsUpEnabled(false);
            }

        }
        buildGoogleApiClient();
    }


    protected synchronized void buildGoogleApiClient()
    {
        DBG.write("MainActivity.buildGoogleApiClient()  called");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
        {
            Common.myLatestLat = mLastLocation.getLatitude();
            Common.myLatestLong = mLastLocation.getLongitude();
            Common.myLatestLocationTimeStamp = String.valueOf(mLastLocation.getTime());

            DBG.write("MainActivity.onConnected() , LOCATION INFORMATION = LAT = >" + Common.myLatestLat + "<" + " LONG = >" + Common.myLatestLong + "<" + "TIMESTAMP = >" + Common.myLatestLocationTimeStamp + "<");
        }
        if (Common.requestingLocationUpdates)
        {
            startLocationUpdates();
        }
    }
    @Override
    public void onConnectionSuspended(int cause)
    {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        DBG.write( "Connection suspended");
        mGoogleApiClient.connect();
    }
    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        DBG.write("Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    protected LocationRequest createLocationRequest()
    {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);

        return mLocationRequest;
    }

    protected void startLocationUpdates()
    {
        LocationRequest mLocationRequest = createLocationRequest();

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        DBG.write("MainActivity.onLocationChanged() , LOCATION INFORMATION = LAT = >" + Common.myLatestLat + "<" + " LONG = >" + Common.myLatestLong + "<" + "TIMESTAMP = >" + Common.myLatestLocationTimeStamp + "<");

        Common.myLatestLat = location.getLatitude();
        Common.myLatestLong = location.getLongitude();
        Common.myLatestLocationTimeStamp = String.valueOf(location.getTime());
    }

    protected void stopLocationUpdates()
    {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }


    // This handles a Contact Selected in the GroupContactsList Fragment
    // replacing the contacts fragment with that contacts message list fragment
    public void onGroupContactItemSelected(long contactNameCursorid )
    {
        DBG.write("MainActivity.onGroupContactItemSelected(), Selected Group Contact Name ID ", contactNameCursorid + " Selected");

        // Getting the clicked Contact from the Cursor ID selected in Contacts Fragment
        String select = "((" + DataProvider.COL_ID + " = " + String.valueOf(contactNameCursorid) + "))";
        DBG.write("MainActivity.onGroupContactItemSelected() Select statement is = " + select);

        Cursor data = getContentResolver().query(
                DataProvider.CONTENT_URI_CONTACTS,
                new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_REGID, DataProvider.COL_GROUPNAME, DataProvider.COL_GROUPMEMBERS, DataProvider.COL_GROUPREGIDS},
                select,
                null,
                DataProvider.COL_ID + " ASC");

        String selectedName = "";
        String selectedRegId = "";
        String selectedFGN = "";
        String selectedGN = "";
        String selectedGroupMembers = "";
        String selectedGroupRegids = "";

        data.moveToFirst();
        while(!data.isAfterLast())
        {
            selectedRegId = data.getString( data.getColumnIndex(DataProvider.COL_REGID));
            selectedName = data.getString( data.getColumnIndex(DataProvider.COL_NAME) );
            selectedGN = data.getString( data.getColumnIndex(DataProvider.COL_GROUPNAME) );
            selectedGroupMembers = data.getString( data.getColumnIndex(DataProvider.COL_GROUPMEMBERS) );
            selectedGroupRegids = data.getString( data.getColumnIndex(DataProvider.COL_GROUPREGIDS) );

            //DBG.write("MainActivity.onGroupContactItemSelected() Iterating through cursor of returned selected names = " + selectedName);
            //DBG.write("MainActivity.onGroupContactItemSelected() Iterating through cursor of returned selected names = " + "RegId " + selectedRegId);
            data.moveToNext();
        }
        // Need to close Cursor
        data.close();


//        FragmentManager fm = getFragmentManager();
//        MessagesFragment mgclf = (MessagesFragment) fm.findFragmentByTag("MGCLF");
//
//        // If the Fragment is non-null, then it is currently being
//        // retained across a configuration change.
//        if ( mgclf == null) {
//            mgclf = new MessagesFragment();
//        }

        // Determines wether fgn == gn which signifies a special group
        boolean isSpecial = false;
        isSpecial = selectedName.equals(selectedGN);


        // The messages fragment needs to know the contact Name Selected to display it's messages
//        mgclf.setContactInfoSelected(selectedName, selectedRegId, true, isSpecial, selectedGN, selectedGroupMembers, selectedGroupRegids);
//
//        fm.beginTransaction().replace(R.id.fragment_placeholder, mgclf, "MGCLF")
//                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                .commit();


        // ====================================================================
        mSectionsPagerAdapter.onSwitchToMessagesFragment(selectedName, selectedRegId, true, isSpecial, selectedGN, selectedGroupMembers, selectedGroupRegids);
        // ====================================================================

        if (getActionBar() != null)
        {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    // This handles a Contact Selected in the ContactsList Fragment
    // replacing the contacts fragment with that contacts message list fragment
    public void onContactItemSelected(long contactNameCursorid )
    {
        DBG.write("MainActivity.onContactItemSelected(), Selected Contact Name ID ", contactNameCursorid + " Selected");

        // Getting the clicked Contact from the Cursor ID selected in Conatcts Fragment
        String select = "((" + DataProvider.COL_ID + " = " + String.valueOf(contactNameCursorid) + "))";
        DBG.write("MainActivity.onContactItemSelected() Select statement is = " + select);

        Cursor data = getContentResolver().query(
                DataProvider.CONTENT_URI_CONTACTS,
                new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_REGID},
                select,
                null,
                DataProvider.COL_ID + " ASC");

        String selectedName = null;
        String selectedRegId = null;
        data.moveToFirst();
        while(!data.isAfterLast())
        {
            selectedRegId = data.getString( data.getColumnIndex(DataProvider.COL_REGID));
            selectedName = data.getString( data.getColumnIndex(DataProvider.COL_NAME) );

            //DBG.write("MainActivity.onContactItemSelected() Iterating through cursor of returned selected names = " + selectedName);
            //DBG.write("MainActivity.onContactItemSelected() Iterating through cursor of returned selected names = " + "RegId " + selectedRegId);
            data.moveToNext();
        }
        // Need to close Cursor
        data.close();


//        FragmentManager fm = getFragmentManager();
//        MessagesFragment mclf = (MessagesFragment) fm.findFragmentByTag("MCLF");
//
//        // If the Fragment is non-null, then it is currently being
//        // retained across a configuration change.
//        if ( mclf == null) {
//            mclf = new MessagesFragment();
//        }
//        // The messages fragment needs to know the contact Name Selected to display it's messages
//        mclf.setContactInfoSelected(selectedName, selectedRegId, false, false, "", "", "");
//
//        fm.beginTransaction().replace(R.id.fragment_placeholder, mclf, "MCLF")
//                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                .commit();

        if (getActionBar() != null)
        {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // ====================================================================
        mSectionsPagerAdapter.onSwitchToMessagesFragment(selectedName, selectedRegId, false, false, "", "", "");
        // ====================================================================

    }


    // This handles the back button in the Action Bar
    // Selections are routed first through the Mainactivity
    // and if not handled then go to fragment onOptionsItemsSelected Function
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                DBG.write("MainActivity.onOptionsItemSelected() Called, Home item selected");

                if (Common.ContactsTabView)
                {
//                    FragmentManager fm = getFragmentManager();
//                    ContactsListFragment clf = (ContactsListFragment) fm.findFragmentByTag("CLF");
//
//                    // If the Fragment is non-null, then it is currently being
//                    // retained across a configuration change.
//                    if (clf == null) {
//                        clf = new ContactsListFragment();
//                    }
//                    fm.beginTransaction().replace(R.id.fragment_placeholder, clf, "CLF")
//                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                            .commit();
                    mSectionsPagerAdapter.onSwitchToMessagesFragment("", "", false, false, "", "", "");
                    if (mSectionsPagerAdapter.mFragmentAtPos0 instanceof MessagesFragment)
                    {
                        if (getActionBar() != null) {
                            getActionBar().setHomeButtonEnabled(true);
                            getActionBar().setDisplayHomeAsUpEnabled(true);
                        }
                    }
                    else
                    {
                        if (getActionBar() != null) {
                            getActionBar().setHomeButtonEnabled(false);
                            getActionBar().setDisplayHomeAsUpEnabled(false);
                        }
                    }
                }
                else // Groups Here!!
                {
//                    FragmentManager fm = getFragmentManager();
//                    GroupContactsListFragment gclf = (GroupContactsListFragment) fm.findFragmentByTag("GCLF");
//
//                    // If the Fragment is non-null, then it is currently being
//                    // retained across a configuration change.
//                    if (gclf == null) {
//                        gclf = new GroupContactsListFragment();
//                    }
//                    fm.beginTransaction().replace(R.id.fragment_placeholder, gclf, "GCLF")
//                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                            .commit();
                    mSectionsPagerAdapter.onSwitchToMessagesFragment("", "", false, false, "", "", "");
                    if (mSectionsPagerAdapter.mFragmentAtPos1 instanceof MessagesFragment)
                    {
                        if (getActionBar() != null) {
                            getActionBar().setHomeButtonEnabled(true);
                            getActionBar().setDisplayHomeAsUpEnabled(true);
                        }
                    }
                    else
                    {
                        if (getActionBar() != null) {
                            getActionBar().setHomeButtonEnabled(false);
                            getActionBar().setDisplayHomeAsUpEnabled(false);
                        }

                    }
                }

                return true;

                case R.id.action_settings:
                    DBG.write("----------------------------------------------------------------------------------------------------------");
                    DBG.write("MainActivity.onOptionsItemsSelected() SETTINGS pressed");
                    DBG.write("-----------------------------------------------------------------------------------------------------------");

                    // Starting the Login Activity
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                    return true;
            default:
                DBG.write("MainActivity.onOptionsItemSelected() Called, Something selected but not handled here!");
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed()
    {
        DBG.write("MainActivity.onOptionsItemSelected() Called, Home item selected");

        if (Common.ContactsTabView)
        {
            if (mSectionsPagerAdapter.mFragmentAtPos0 instanceof MessagesFragment)
            {
                mSectionsPagerAdapter.onSwitchToMessagesFragment("", "", false, false, "", "", "");
                if (getActionBar() != null) {
                    getActionBar().setHomeButtonEnabled(false);
                    getActionBar().setDisplayHomeAsUpEnabled(false);
                }
            }
            else
            {
                super.onBackPressed();
            }
        }
        else // Groups Here!!
        {
            if (mSectionsPagerAdapter.mFragmentAtPos1 instanceof MessagesFragment)
            {
                mSectionsPagerAdapter.onSwitchToMessagesFragment("", "", false, false, "", "", "");
                if (getActionBar() != null) {
                    getActionBar().setHomeButtonEnabled(false);
                    getActionBar().setDisplayHomeAsUpEnabled(false);
                }
            }
            else
            {
                super.onBackPressed();
            }
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();
        DBG.write("MainActivity.onResume() with UserName/Regid to be displayed = " + Common.nickName + " " + Common.regId);

        if (getActionBar() != null)
        {
            getActionBar().setTitle( (Common.nickName.split("\\@"))[0] );
            getActionBar().setSubtitle("Online");

            getActionBar().setHomeButtonEnabled(false);
            getActionBar().setDisplayHomeAsUpEnabled(false);
        }
        GolgiAPI.usePersistentConnection();
        Common.inFg = true;


        // Moved from onCreate
        maybeSendUserNameToServer();

        GolgiService.cancelNotification(this, Common.NOTIFICATION_ONE);


        if (mGoogleApiClient.isConnected() && !Common.requestingLocationUpdates)
        {
            startLocationUpdates();
        }
    }

    private void maybeSendUserNameToServer() {
        if (Common.nickName.equals(""))
        {
            DBG.write("----------------------------------------------------------------------------------------------------------");
            DBG.write("MainActivity.sendUsername() Nothing in UserName Shared Preferences, Need to do Login and Register Username");
            DBG.write("-----------------------------------------------------------------------------------------------------------");

            // Starting the Login Activity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);

        }
        else
        {
            DBG.write("-----------------------------------------------------------------------------------------------");
            DBG.write("Username already stored in SharedPreferences means name/regid is already registered with server");
            DBG.write("the RegId is = " + Common.regId);
            DBG.write("The nickName is = " + Common.nickName);
            DBG.write("------------------------------------------------------------------------------------------------");
        }
    }


    @Override
    public void onPause()
    {
        DBG.write("MainActivity.onPause()");
        // When being put into the background we no longer want to maintain the
        // persistent connection as it may have an adverse effect on the battery.
        Common.inFg = false;
        GolgiAPI.useEphemeralConnection();

        //LocalBroadcastManager.getInstance(this).unregisterReceiver(UIAccessReceiver);
        super.onPause();
        if (mGoogleApiClient.isConnected() && !Common.requestingLocationUpdates)
        {
            stopLocationUpdates();
        }
    }


    @Override
    public void onStart()
    {
        super.onStart();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

        DBG.write("MainActivity.onStart()");
    }

    @Override
    public void onStop()
    {
        super.onStop();

        if (mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.disconnect();
        }

        DBG.write("MainActivity.onStop()");
    }
}

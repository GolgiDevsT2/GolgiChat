package com.openmindnetworks.golgichat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;

import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.openmindnetworks.golgi.api.GolgiAPI;
import com.openmindnetworks.golgichat.datamodel.DataProvider;
import com.openmindnetworks.golgichat.utils.DBG;


public class LocationActivity extends Activity implements OnMapReadyCallback
{
    private String mapName = "";
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);


        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            mapName = extras.getString(MessagesFragment.CURRENTMAPCONTACT);

        }
        setupActionBar();

        MapFragment mMapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction =  getFragmentManager().beginTransaction();

        fragmentTransaction.add(R.id.map_fragment_placeholder, mMapFragment);
        fragmentTransaction.commit();

        mMapFragment.getMapAsync(this);
    }

//    @Override
//    public void onReceive(Context context, Intent intent)
//    {
//
//    }

    @Override
    public void onMapReady(GoogleMap map)
    {
        double latDoubble = 0;
        double lonDoubble = 0;

        DBG.write("LocationActivity.onMapReady() ");


        String select = "((" + DataProvider.COL_FROM + " = '" + mapName + "'))";
        //DBG.write("MessagesFragment Getting RegId for this User Select statement is = " + select);

        Cursor data = getContentResolver().query(
                DataProvider.CONTENT_URI_MESSAGES,
                new String[]{DataProvider.COL_ID, DataProvider.COL_LATITUDE, DataProvider.COL_LONGITUDE, DataProvider.COL_LOCATION_TIMESTAMP},
                select,
                null,
                DataProvider.COL_ID + " ASC");


        // Hopefully get the last message
        data.moveToLast();
        while(!data.isAfterLast())
        {
            String lat = data.getString( data.getColumnIndex(DataProvider.COL_LATITUDE));
            String lon = data.getString(data.getColumnIndex(DataProvider.COL_LONGITUDE));

            DBG.write("LocationActivity Iterating through cursor of returned selected messages = LAT = >" + lat + "< LON = >" + lon + "<");

            if (lat.equals("") || lon.equals(""))
            {
                lat = "0";
                lon = "0";
            }
            latDoubble = Double.valueOf(lat);
            lonDoubble = Double.valueOf(lon);


            data.moveToNext();
        }
        // Need to close Cursor
        data.close();

        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);


        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latDoubble, lonDoubble), 17));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latDoubble, lonDoubble), 17));

        map.addMarker(new MarkerOptions()
                .position(new LatLng(latDoubble, lonDoubble))
                .title((mapName.split("\\@"))[0] ));

    }


    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle((mapName.split("\\@"))[0] );
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // TODO: If Settings has multiple levels, Up should navigate up
            // that hierarchy.
            //NavUtils.navigateUpFromSameTask(this);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause()
    {
        DBG.write("SettingsActivity.onPause()");
        Common.inFg = false;
        GolgiAPI.useEphemeralConnection();
        super.onPause();
    }
    @Override
    public void onResume()
    {
        DBG.write("SettingsActivity.onResume()");
        GolgiAPI.usePersistentConnection();
        Common.inFg = true;
        super.onResume();
    }
}

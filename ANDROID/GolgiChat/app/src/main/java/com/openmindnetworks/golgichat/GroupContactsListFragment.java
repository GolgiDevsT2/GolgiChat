package com.openmindnetworks.golgichat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.openmindnetworks.golgichat.datamodel.DataProvider;
import com.openmindnetworks.golgichat.utils.DBG;

/**
 * Created by derekdoherty on 06/02/2015.
 */


/**
 * Created by derekdoherty on 20/01/2015.
 */

//TODO: Check for NULL on MainActivity when firing Callbacks to Mainactivity
public class GroupContactsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    private SimpleCursorAdapter adapter;
    private OnGroupItemSelectedListener listener;

    private  Cursor listOfContacts = null;

    private static final String ARG_SECTION_NUMBER = "section_number";
    public static GroupContactsListFragment newInstance(int sectionNumber)
    {
        GroupContactsListFragment fragment = new GroupContactsListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public GroupContactsListFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach()
    {
        super.onDetach();
        listener = null;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        DBG.write("GroupContactsListFragment.onCreateView()", "Called");

        String[] parts = Common.nickName.split("\\@");
        String shortNickName = parts[0];

        if (getActivity().getActionBar() != null)
        {
            getActivity().getActionBar().setTitle(shortNickName);
            getActivity().getActionBar().setSubtitle("Online");
        }

        adapter = new SimpleCursorAdapter(this.getActivity(),
                R.layout.contacts_list_item_new,
                null,
                new String[]{DataProvider.COL_GROUPNAME, DataProvider.COL_COUNT, DataProvider.COL_IMAGE_REF}, // DataProvider.COL_FROM_IN_GROUP},
                new int[]{R.id.text1, R.id.text2, R.id.avatar},
                0);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
        {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex)
            {
                switch(view.getId())
                {
//
//                    case R.id.text1:
//                        ((TextView)view).setText("");
//                        return true;
                    case R.id.text2:
                        int count = cursor.getInt(columnIndex);
                        if (count > 0)
                        {
                            ((TextView)view).setVisibility(View.VISIBLE);
                            // ((TextView)view).setText(String.format("%d new message%s", count, count==1 ? "" : "s"));
                            ((TextView)view).setText(String.format("%d", count));

                        }
                        else
                        {
                            ((TextView)view).setVisibility(View.INVISIBLE);
                        }
                        return true;
                    case R.id.avatar:
                        String imageRefLetterUri = "drawable/" + cursor.getString(columnIndex) ;

                        int imageResource = getResources().getIdentifier(imageRefLetterUri, null, getActivity().getPackageName());

                        Drawable myIcon;
                        try
                        {
                            myIcon = getResources().getDrawable(imageResource);
                            ((ImageView) view).setImageDrawable(myIcon);
                        } catch (Resources.NotFoundException e)   // Fallback Letter if Error abaove
                        {
                            imageRefLetterUri = "drawable/" + "z" ;
                            imageResource = getResources().getIdentifier(imageRefLetterUri, null, getActivity().getPackageName());
                            myIcon = getResources().getDrawable(imageResource);
                            ((ImageView) view).setImageDrawable(myIcon);
                        }
                        return true;
                }
                return false;
            }
        });

        setListAdapter(adapter);
        getLoaderManager().initLoader(0, null, this);

        return super.onCreateView(inflater, container, savedInstanceState);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = getListView();
        //listView.setDivider(new ColorDrawable(Color.WHITE));
        //listView.setDividerHeight(3); // 3 pixels height
        listView.setBackgroundColor(0xFFDBE2ED);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);


        // Long Click to Display Members of this Particular Group

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {

                // I have the id for a query in the Contacts Table for this Groupname
                DBG.write("GroupContactsListFragment.onItemLongClick, Selected Group Contact Name ID ", id + " Selected");

                // Getting the clicked Contact from the Cursor ID selected in Conatcts Fragment
                String select = "((" + DataProvider.COL_ID + " = " + String.valueOf(id) + "))";
                DBG.write("MainActivity.onGroupContactItemSelected() Select statement is = " + select);

                Cursor data = getActivity().getContentResolver().query(
                        DataProvider.CONTENT_URI_CONTACTS,
                        new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_REGID, DataProvider.COL_GROUPNAME, DataProvider.COL_GROUPMEMBERS, DataProvider.COL_GROUPREGIDS},
                        select,
                        null,
                        DataProvider.COL_ID + " ASC");

                data.moveToFirst();
                String selectedRegId;
                String selectedName = "";
                String selectedGroupMembers = "";

                while(!data.isAfterLast())
                {
                    selectedRegId = data.getString( data.getColumnIndex(DataProvider.COL_REGID));
                    selectedName = data.getString( data.getColumnIndex(DataProvider.COL_NAME) );
                    selectedGroupMembers = data.getString( data.getColumnIndex(DataProvider.COL_GROUPMEMBERS) );

                    DBG.write("GroupContactsListFragment.onItemLongClick() Iterating through cursor of returned selected names = " + selectedName);
                    DBG.write("GroupContactsListFragment.onItemLongClick() Iterating through cursor of returned selected names = " + "RegId " + selectedRegId);
                    DBG.write("GroupContactsListFragment.onItemLongClick() Iterating through cursor of returned selected names = " + "GroupMembers " + selectedGroupMembers);
                    data.moveToNext();
                }
                // Need to close Cursor
                data.close();


                // Don't make it work for ALL MEMBER GROUP
                if (selectedName.equals(Common.GOLGICHAT_ALL_MEMBER_GROUP_NAME))
                {
                    return true;
                }

                AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());
                builderSingle.setIcon(R.drawable.header_logo);

                builderSingle.setTitle("Group Members:-");

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.select_dialog_item);

                //selectedGroupMembers.trim().split(" ");
                arrayAdapter.addAll(selectedGroupMembers.trim());

                builderSingle.setAdapter(arrayAdapter, null);

                builderSingle.setNegativeButton("Dismiss",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                builderSingle.show();

                return true;
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        CursorLoader loader;
        if (id == 0) {
            // Select string where GroupName is not empty means it is a Group Contact
            String select = "(" + DataProvider.COL_GROUPNAME + " <> '') ";

            loader = new CursorLoader(this.getActivity(),
                    DataProvider.CONTENT_URI_CONTACTS,
                    new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_COUNT, DataProvider.COL_IMAGE_REF, DataProvider.COL_GROUPNAME},
                    select,
                    null,
                    DataProvider.COL_GROUPNAME + " ASC");
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
        //adapter.swapCursor(null);

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

    @Override
    public void onListItemClick (ListView l, View v, int position, long id)
    {
        // This should be the id in the contacts table
        listener.onGroupContactItemSelected(id);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        if (activity instanceof OnGroupItemSelectedListener)
        {
            listener = (OnGroupItemSelectedListener) activity;
        } else
        {
            throw new ClassCastException(activity.toString()
                    + " must implement Interface OnGroupItemSelectedListener");
        }
    }

    // Declare this interface to communicate with Parent Activity
    public interface OnGroupItemSelectedListener
    {
        public void onGroupContactItemSelected(long contactNameSelectedid);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater)
    {
        //this.optionsMenu = menu;
        inflater.inflate(R.menu.menu_groupcontactslist, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_add:
                AddGroupContactDialog dialog = new AddGroupContactDialog();
                dialog.show(getFragmentManager(), "AddGroupContactDialog");
                return true;

            case R.id.action_settings:
                DBG.write("GroupContactsListFragment.onOptionsItemsSelected() pressed");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause()
    {
        DBG.write("GroupContactsListFragment.onPause()");
        //Common.inFg = false;
        //GolgiAPI.useEphemeralConnection();
        super.onPause();
    }
    @Override
    public void onResume()
    {
        DBG.write("GroupContactsListFragment.onResume()");
        //GolgiAPI.usePersistentConnection();
        //Common.inFg = true;
        super.onResume();
    }
}


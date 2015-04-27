package com.openmindnetworks.golgichat;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
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

import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.openmindnetworks.golgichat.datamodel.DataProvider;
import com.openmindnetworks.golgichat.utils.DBG;


/**
 * Created by derekdoherty on 20/01/2015.
 */

public class ContactsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    private SimpleCursorAdapter adapter;
    private OnItemSelectedListener listener;

    private static final String ARG_SECTION_NUMBER = "section_number";
    public static ContactsListFragment newInstance(int sectionNumber)
    {
        ContactsListFragment fragment = new ContactsListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public ContactsListFragment()
    {
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
        DBG.write("ContactsListFragment.onCreateView()", "Called");

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
                new String[]{DataProvider.COL_NAME, DataProvider.COL_COUNT, DataProvider.COL_IMAGE_REF},
                new int[]{R.id.text1, R.id.text2, R.id.avatar},
                0);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
        {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex)
            {
                switch(view.getId())
                {
                    case R.id.text1:
                        String shortName = (cursor.getString(columnIndex).split("\\@"))[0] ;
                        ((TextView)view).setText(shortName);
                        return true;
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
                        } catch (Resources.NotFoundException e)
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
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        //hideSoftKeyBoard();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        // Select string where GroupName is empty means it is a normal contact
        String select = "(" + DataProvider.COL_GROUPNAME + " = '') ";

        CursorLoader loader = new CursorLoader(this.getActivity(),
                DataProvider.CONTENT_URI_CONTACTS,
                new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_COUNT, DataProvider.COL_IMAGE_REF},
                select,
                null,
                DataProvider.COL_NAME + " ASC");

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data)
    {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        adapter.swapCursor(null);
    }


    @Override
    public void onListItemClick (ListView l, View v, int position, long id)
    {
        // This should be the id in the contacts table
        listener.onContactItemSelected(id);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        if (activity instanceof OnItemSelectedListener)
        {
            listener = (OnItemSelectedListener) activity;
        } else
        {
            throw new ClassCastException(activity.toString()
                    + " must implement MyListFragment.OnItemSelectedListener");
        }
    }

    // Declare this interface to communicate with Parent Activity
    public interface OnItemSelectedListener
    {
        public void onContactItemSelected(long contactNameSelectedid);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater)
    {
        //this.optionsMenu = menu;
        inflater.inflate(R.menu.menu_contactslist, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_add:
                AddContactDialog dialog = new AddContactDialog();
                dialog.show(getFragmentManager(), "AddContactDialog");
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause()
    {
        DBG.write("ContactsListFragment.onPause()");
        //Common.inFg = false;
        //GolgiAPI.useEphemeralConnection();
        super.onPause();
    }
    @Override
    public void onResume()
    {
        DBG.write("ContactsListFragment.onResume()");
        //GolgiAPI.usePersistentConnection();
        //Common.inFg = true;
        super.onResume();
    }



}

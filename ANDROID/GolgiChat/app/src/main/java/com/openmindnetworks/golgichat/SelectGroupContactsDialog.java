package com.openmindnetworks.golgichat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.openmindnetworks.golgichat.datamodel.DataProvider;
import com.openmindnetworks.golgichat.utils.DBG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by derekdoherty on 13/02/2015.
 */


public class SelectGroupContactsDialog extends DialogFragment  implements LoaderManager.LoaderCallbacks<Cursor>
{
    private SimpleCursorAdapter adapter;
    public static Cursor contactsCursor = null;
    public static String groupContactOk = null;
    private String spaceSepStringOfContacts = "";
    private String spaceSepStringOfRegids = "";
    private AlertDialog.Builder builder;

    private ArrayList<Integer> mSelectedItems = new ArrayList<Integer>();
    private ArrayList<String> selectedStrings = new ArrayList<String>();
    private ArrayList<String> selectedRegidsStrings = new ArrayList<String>();

    private HashMap<String, String> filteredStringsMap = new HashMap<String, String>();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        DBG.write("Cursor count = " + String.valueOf(contactsCursor.getCount()));
        while (contactsCursor.moveToNext())
        {
            DBG.write( " Cursor contents = " + contactsCursor.getString( contactsCursor.getColumnIndex(DataProvider.COL_NAME) ));
            selectedStrings.add(contactsCursor.getString( contactsCursor.getColumnIndex(DataProvider.COL_NAME)));
            selectedRegidsStrings.add(contactsCursor.getString( contactsCursor.getColumnIndex(DataProvider.COL_REGID)));
        }

        final String[] simpleArray = new String[ selectedStrings.size() ];
        selectedStrings.toArray( simpleArray );

        final String[] simpleRegidArray = new String[ selectedRegidsStrings.size() ];
        selectedRegidsStrings.toArray( simpleRegidArray );



        builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Contacts for Group")
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                //        .setMultiChoiceItems(contactsCursor, DataProvider.COL_ISCHECKED, DataProvider.COL_NAME,
                // TODO: Consider Using Set instead of HashMap for filteredStringMap
                .setMultiChoiceItems(simpleArray, null, new DialogInterface.OnMultiChoiceClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked)
                    {
                        if (isChecked) {
                            DBG.write("Thing selected is = " + which + " isChecked = " + isChecked);
                            filteredStringsMap.put(simpleArray[which], simpleRegidArray[which]);
                        }
                        else if (filteredStringsMap.containsKey(simpleArray[which]))
                        {
                            DBG.write("Thing unSelected is = " + which + " isChecked = " + isChecked);
                            filteredStringsMap.remove(simpleArray[which]);
                        }
                    }
                })

                 // Set the action buttons
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog
                        if (filteredStringsMap.size() < 2)
                        {
                            // Don't Create, Need at least Two other Contacts for a Group to Make sense
                            Toast.makeText(getActivity() , "Need at least two contacts to make a group!", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            // Form a space seperated from Hashmap
                            for (Map.Entry<String, String> entry : filteredStringsMap.entrySet())
                            {
                                String key = entry.getKey().trim();
                                String value = entry.getValue().trim();

                                spaceSepStringOfContacts = spaceSepStringOfContacts + " " + key;
                                spaceSepStringOfRegids = spaceSepStringOfRegids + " " + value;
                            }

                            // Need to add Myself to list of Group Contacts
                            spaceSepStringOfContacts = spaceSepStringOfContacts + " " + Common.nickName;
                            spaceSepStringOfRegids = spaceSepStringOfRegids + " " + Common.regId;

                            spaceSepStringOfContacts.trim();
                            spaceSepStringOfRegids.trim();
                            String fullGroupName = Common.nickName + "_" + groupContactOk;
                            DBG.write("GroupName Ok storing as contact FGN = " + fullGroupName + " GroupName = " + groupContactOk);
                            DBG.write("GroupName Ok storing SpaceSeperated String of Contacts= >" + spaceSepStringOfContacts + "<");
                            DBG.write("GroupName Ok storing SpaceSeperated String of RegIds= >" + spaceSepStringOfRegids + "<");
                            try
                            {
                                ContentValues values = new ContentValues(6);
                                //values.put(DataProvider.COL_NAME, email.substring(0, email.indexOf('@')));


                                values.put(DataProvider.COL_NAME, fullGroupName );
                                values.put(DataProvider.COL_GROUPNAME, groupContactOk );
                                values.put(DataProvider.COL_GROUPMEMBERS, spaceSepStringOfContacts);
                                values.put(DataProvider.COL_GROUPREGIDS, spaceSepStringOfRegids);

                                String capitalFirstLetter = groupContactOk.substring(0, 1).toLowerCase();
                                DBG.write("First Letter of Group Contact =  >" + capitalFirstLetter + "<");
                                values.put(DataProvider.COL_IMAGE_REF, capitalFirstLetter);

                                // TODO: For a Group , setting REGID to empty
                                values.put(DataProvider.COL_REGID, "");

                                getActivity().getContentResolver().insert(DataProvider.CONTENT_URI_CONTACTS, values);

                            } catch (SQLException sqle)
                            {
                                DBG.write("This should not happen as we have already checked for uniqueness !!");
                                DBG.write("AddGroupContactDialog SQL Error!!, Assuming is Already existing Error !!! error = " + sqle.getLocalizedMessage() + " Cause  " + sqle.getCause() + " Message  " + sqle.getMessage());
                            }
                        }
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // Should just exit as Normal
                    }
                });

        return builder.create();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        // Select string where GroupName is empty means it is a normal contact
        String select = "(" + DataProvider.COL_GROUPNAME + " = '') ";

        CursorLoader loader = new CursorLoader(this.getActivity(),
                DataProvider.CONTENT_URI_CONTACTS,
                new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_COUNT, DataProvider.COL_ISCHECKED},
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
}

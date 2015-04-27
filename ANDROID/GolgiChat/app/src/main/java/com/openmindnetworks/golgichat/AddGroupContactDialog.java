package com.openmindnetworks.golgichat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.openmindnetworks.golgichat.datamodel.DataProvider;
import com.openmindnetworks.golgichat.utils.DBG;

/**
 * Created by derekdoherty on 09/02/2015.
 */
public class AddGroupContactDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private enum addGroupNameFailures
    {
        EMPTY,
        YOURSELF,
        GROUPALREADYEXISTS,
        OTHERREASON,
        NONALPHANUMERIC,
        GROUPOK
    }

    public static Cursor listOfContacts = null;
    public static String groupContactOk = null;

    public static addGroupNameFailures successFlag = addGroupNameFailures.OTHERREASON;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final Context ctx = getActivity();
        final EditText et = new EditText(ctx);

        // Retreive list of contacts in Advance
        getLoaderManager().initLoader(0, null, this);

        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setHint("GolgiChat GroupName");

        final AlertDialog alert = new AlertDialog.Builder(ctx)
                .setTitle("Add New GroupName")
                .setView(et)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        alert.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface dialog)
            {
                Button okBtn = alert.getButton(AlertDialog.BUTTON_POSITIVE);
                okBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        DBG.write("AddContactDialog.onClick");
                        String enteredText = et.getText().toString();



                        switch (isGroupNameToAddValid(enteredText))
                        {
                            case YOURSELF:
                                et.setError("Can't add yourself as a Group Name");
                                break;

                            case EMPTY:
                                et.setError("Empty Group Contact Name !");
                                break;

                            case GROUPALREADYEXISTS:
                                et.setError("Group Name Already Exists");
                                break;

                            case NONALPHANUMERIC:
                                et.setError("Just letters or numbers in Name !");
                                break;

                            case OTHERREASON:
                                alert.dismiss();
                                break;

                            case GROUPOK:
                                // Dont really want spaces in Group Names
                                String groupNameToAdd = enteredText.trim().replaceAll("\\s+","_");

                                SelectGroupContactsDialog sgcd = new SelectGroupContactsDialog();
                                SelectGroupContactsDialog.contactsCursor = listOfContacts;
                                SelectGroupContactsDialog.groupContactOk = groupNameToAdd;
                                sgcd.show(getFragmentManager(), "SelectGroupContactsDialog");

                                alert.dismiss();
                                break;

                            default:
                                System.out.println("Error, Should not get here!");
                                break;
                        }
                    }
                });
            }
        });
        return alert;
    }

    private addGroupNameFailures isGroupNameToAddValid(String groupContactToAdd)
    {
        if (groupContactToAdd.equals(Common.nickName) )
        {
            successFlag = addGroupNameFailures.YOURSELF;
            return successFlag;
        }
        else if(groupContactToAdd.isEmpty()  )
        {
            successFlag = addGroupNameFailures.EMPTY;
            return successFlag;
        }
        // Add Space character below to allow spaces in Group Name
        else if( !(groupContactToAdd.matches("^[a-zA-Z0-9 ]*$")) )
        {
            successFlag = addGroupNameFailures.NONALPHANUMERIC;
            return successFlag;
        }
        // Got this far so just add Group name to contacts list unless it already exists
        else
        {
            // Check if group already exists
            String fullGroupName = Common.nickName + "_" + groupContactToAdd;

            String select = "((" + DataProvider.COL_NAME + " = '" + fullGroupName + "'))";
            DBG.write("AddGroupContactDialog.isGroupNameToAddValid() Select statement is = " + select);

            Cursor data = getActivity().getContentResolver().query(
                    DataProvider.CONTENT_URI_CONTACTS,
                    new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_GROUPNAME},
                    select,
                    null,
                    DataProvider.COL_ID + " ASC");



            if (data.getCount() >= 1 )
            {
                DBG.write("Group Already Exists");
                successFlag = addGroupNameFailures.GROUPALREADYEXISTS;
            }
            else
            {
                successFlag = addGroupNameFailures.GROUPOK;
            }
            data.close();
        }
        return successFlag;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        // Select string where GroupName is empty means it is a normal contact
        String select = "(" + DataProvider.COL_GROUPNAME + " = '') ";

        CursorLoader loader = new CursorLoader(this.getActivity(),
                DataProvider.CONTENT_URI_CONTACTS,
                new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_REGID, DataProvider.COL_COUNT, DataProvider.COL_ISCHECKED},
                select,
                null,
                DataProvider.COL_NAME + " ASC");

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data)
    {
        switch (loader.getId())
        {
            case 0:
                listOfContacts = data;
                break;
            case 1:
                // do some other stuff here
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
                listOfContacts = null;
                break;
            case 1:
                // do some other stuff here
                break;
            default:
                break;
        }
    }

}

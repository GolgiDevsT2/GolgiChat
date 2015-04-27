package com.openmindnetworks.golgichat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.SQLException;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.openmindnetworks.golgi.api.GolgiException;
import com.openmindnetworks.golgichat.datamodel.DataProvider;
//import com.openmindnetworks.golgichat.gen.RegCode;
import com.openmindnetworks.golgichat.gen.AllContactInfo;
import com.openmindnetworks.golgichat.gen.RegInfo;
import com.openmindnetworks.golgichat.gen.golgiChatService;
import com.openmindnetworks.golgichat.utils.DBG;
import com.openmindnetworks.golgi.api.GolgiTransportOptions;

import java.util.ArrayList;

/**
 * Created by derekdoherty on 21/01/2015.
 */
public class AddContactDialog extends DialogFragment
{

    public static boolean asyncGolgiMessageSendingTaskFinished = false;
    private enum addNameFailures
    {
        EMPTY,
        YOURSELF,
        NOTREGISTERED,
        REGISTERED,
        SEND_FAILURE
    }


    public static addNameFailures successFlag = addNameFailures.NOTREGISTERED;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context ctx = getActivity();
        final EditText et = new EditText(ctx);

        asyncGolgiMessageSendingTaskFinished = false;

        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setHint("GolgiChat UserName");

        final AlertDialog alert = new AlertDialog.Builder(ctx)
                .setTitle("Add GolgiChat Contact")
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
                        String contactToAdd = et.getText().toString();


                        switch (isContactToAddValid(contactToAdd))
                        {
                            case YOURSELF:
                                et.setError("Can't add yourself as a Contact");
                                asyncGolgiMessageSendingTaskFinished = false;
                                break;

                            case EMPTY:
                                et.setError("Empty Contact");
                                asyncGolgiMessageSendingTaskFinished = false;
                                break;

                            case NOTREGISTERED:
                                et.setError("Not a registered UserName!");
                                asyncGolgiMessageSendingTaskFinished = false;
                                break;

                            case REGISTERED:
                                alert.dismiss();
                                break;

                            case SEND_FAILURE:
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

    private addNameFailures isContactToAddValid(String contactToAdd)
    {
        GolgiTransportOptions stdGto;
        stdGto = new GolgiTransportOptions();
        stdGto.setValidityPeriod(20);

        if (contactToAdd.equals(Common.nickName) )
        {
            successFlag = addNameFailures.YOURSELF;
            return successFlag;
        }
        else if(contactToAdd.isEmpty() )
        {
            successFlag = addNameFailures.EMPTY;
            return successFlag;
        }

        // Check with Server for a RegId for given UserName, Empty String means no match
        // Some Waiting needed here to make sure we get a response
        golgiChatService.getRegInfo.sendTo(new golgiChatService.getRegInfo.ResultReceiver()
                                           {
                                               @Override
                                               public void failure(GolgiException ex)
                                               {
                                                   DBG.write("Send getRegInfo Failed: " + ex.getErrText());
                                                   successFlag = addNameFailures.SEND_FAILURE;
                                                   asyncGolgiMessageSendingTaskFinished = true;
                                               }

                                               @Override
                                               public void success(RegInfo ri)
                                               {
                                                   if(ri.getRegId().equals(""))
                                                   {
                                                       successFlag = addNameFailures.NOTREGISTERED;
                                                       DBG.write("GetRegInfo Send Success but no match for Username on Server");
                                                   }
                                                   else
                                                   {
                                                       successFlag = addNameFailures.REGISTERED;
                                                       DBG.write("GetRegInfo Send Success with matching Username on Server");
                                                       try
                                                       {
                                                           DBG.write("Writing Matched Username information to Contacts Table");
                                                           ContentValues values = new ContentValues(2);
                                                           //values.put(DataProvider.COL_NAME, email.substring(0, email.indexOf('@')));
                                                           values.put(DataProvider.COL_NAME, ri.getRegName());
                                                           values.put(DataProvider.COL_REGID, ri.getRegId());

                                                           getActivity().getContentResolver().insert(DataProvider.CONTENT_URI_CONTACTS, values);
                                                       } catch (SQLException sqle)
                                                       {
                                                           DBG.write("AddContactDialog SQL Error!!");
                                                       }
                                                   }
                                                   asyncGolgiMessageSendingTaskFinished = true;
                                               }
                                           },
                stdGto,
                Common.golgiChatServer,
                contactToAdd);

        // Need to wait here until I get some sort of Server response
        while (!asyncGolgiMessageSendingTaskFinished)
        {
            try { Thread.sleep(100); }
            catch (InterruptedException e) { e.printStackTrace(); }
        }

        return successFlag;
    }

}

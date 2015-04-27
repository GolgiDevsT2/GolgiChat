package com.openmindnetworks.golgichat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
//import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
//import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
//import android.widget.EditText;
//import android.widget.TextView;

import com.openmindnetworks.golgi.api.GolgiAPI;
import com.openmindnetworks.golgi.api.GolgiException;
import com.openmindnetworks.golgi.api.GolgiTransportOptions;
import com.openmindnetworks.golgichat.datamodel.DataProvider;
import com.openmindnetworks.golgichat.gen.AllContactInfo;
import com.openmindnetworks.golgichat.gen.RegCode;
import com.openmindnetworks.golgichat.gen.RegInfo;
import com.openmindnetworks.golgichat.gen.golgiChatService;
import com.openmindnetworks.golgichat.gen.ServerAddress;
import com.openmindnetworks.golgichat.utils.DBG;

import java.util.ArrayList;
import java.util.List;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {


    // UI references.
    private static AutoCompleteTextView mEmailView;
    private static String email;
    private EditText mEnterCodeView;
    private View mProgressView;
    private View mLoginFormView;
    private static TextView mInfoText;

    private static Button mEmailSignInButton;
    private static Button mEnterCodeButton;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        GolgiAPI.usePersistentConnection();
        Common.inFg = true;

        // Set up the Code input Field
        mEnterCodeView = (EditText) findViewById(R.id.enter_code_field);
        mEnterCodeView.setVisibility(View.GONE);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mInfoText.setText("");
                attemptLogin();
            }
        });

        mEnterCodeButton = (Button) findViewById(R.id.enter_code_button);
        mEnterCodeButton.setVisibility(View.GONE);
        mEnterCodeButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String enteredCode = mEnterCodeView.getText().toString().trim();
                sendVerificationCode(enteredCode, email, Common.golgiChatServer);
               //finish();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mInfoText = (TextView)findViewById(R.id.info_text_field);


        DBG.write("LoginActivity.onCreate()");
    }

    private final static int EVENT1 = 1;
    private final static int EVENT2 = 2;
    private  Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case EVENT1:
                    showProgress(false);
                    mInfoText.setText("No Activation Detected, Retry Code Entry");
                    mEmailSignInButton.setEnabled(true);
                    DBG.write("TIMEOUT on waiting for activiation Code, maybe Retry!!");
                    //Toast.makeText(MyActivity.this, "Event 1", Toast.LENGTH_SHORT).show();
                    break;

                case EVENT2:
                    showProgress(false);
                    mInfoText.setText("Connection Problem, Try Again");
                    mEmailView.setEnabled(true);
                    mEmailSignInButton.setEnabled(true);


                    DBG.write("TIMEOUT on waiting for GolgiChatServer name!!");
                    break;

                default:
                    DBG.write("This should'nt happen!!");
                    //Toast.makeText(MyActivity.this, "Unhandled", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    private void populateAutoComplete()
    {
        getLoaderManager().initLoader(0, null, this);
    }

    // Prevent Back button from working on Login Screen
    @Override
    public void onBackPressed()
    {
        return;
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {

        // Reset errors.
        mEmailView.setError(null);

        // Store values at the time of the login attempt.
        email = mEmailView.getText().toString().trim();
        Common.maybeNickName = email;

        DBG.write("LoginActivity.attemptLogin() , Entered Email is >" + email + "<");

        boolean cancel = false;
        View focusView = null;


        // Check for a valid email address.
        if (TextUtils.isEmpty(email))
        {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email))
        {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mEmailView.setEnabled(false);
            mEmailSignInButton.setEnabled(false);

            // Get and Set the Server name to be used for this particular Domain/Company User
            getGolgiChatServerNameFromGCCServer(Common.maybeNickName);
        }
    }


    private void sendVerificationCode(String verCode, String emailName, String serverToUse) {
        GolgiTransportOptions stdGto;
        stdGto = new GolgiTransportOptions();
        stdGto.setValidityPeriod(25);

        Message msg = handler.obtainMessage(EVENT1);
        handler.sendMessageDelayed(msg, 30000); // Set Delay of 30 Seconds


        golgiChatService.verifyCode.sendTo(new golgiChatService.verifyCode.ResultReceiver()
                                           {
                                             @Override
                                             public void failure(GolgiException ex)
                                             {
                                                 handler.removeMessages(EVENT1);
                                                 DBG.write("Send Verification Code Failed: " + ex.getErrText());
                                                 LoginActivity.this.runOnUiThread(new Runnable()
                                                 {
                                                     @Override
                                                     public void run()
                                                     {
                                                         showProgress(false);
                                                         mInfoText.setText("Communication Problem , Try Again");
                                                         mEmailView.setEnabled(true);
                                                         mEmailSignInButton.setEnabled(true);
                                                     }
                                                 });
                                             }

                                             @Override
                                             public void success(RegCode rc)
                                             {
                                                 handler.removeMessages(EVENT1);

                                                 DBG.write("Send Verification Code Success");
                                                 if (rc.getCode() == 1)
                                                 {
                                                     DBG.write("Verification Code Validated, Writing UserName to SharedPreferences");
                                                     SharedPreferences prefs = getSharedPreferences("GolgiChatPrefs", Context.MODE_PRIVATE);
                                                     SharedPreferences.Editor editor = prefs.edit();
                                                     //editor.putString("nickName", "GOLGI_CHAT_ANDROID_CLIENT");
                                                     editor.putString("nickName", email);
                                                     editor.commit();

                                                     Common.nickName = prefs.getString("nickName", "");
                                                     DBG.write("+++++++++++++++++++++++++++ Verification Success, Going Back to Main Activity +++++++++++++++++++++++++++++++++");

                                                     LoginActivity.this.runOnUiThread(new Runnable() {
                                                         @Override
                                                         public void run()
                                                         {
                                                             // Starting the Login Activity
                                                             //Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                                             //i.setAction(Intent.ACTION_MAIN);
                                                             //i.addCategory(Intent.CATEGORY_LAUNCHER);
                                                             //startActivity(i);
                                                             showProgress(false);
                                                             //Verification Code Validated, Close Activity
                                                             finish();
                                                         }
                                                     });
                                                 }
                                                 // Verification Code Not Matched
                                                 else if (rc.getCode() == 0)
                                                 {
                                                     LoginActivity.this.runOnUiThread(new Runnable() {
                                                         @Override
                                                         public void run()
                                                         {
                                                             showProgress(false);
                                                             mInfoText.setText("Incorrect Code, Try Again");
                                                         }
                                                     });
                                                 }
                                             }
                                         },
                stdGto,
                serverToUse,
                verCode,
                emailName,
                Common.regId
        );
    }




    private void getGolgiChatServerNameFromGCCServer(String maybeEmailUserName) {
        GolgiTransportOptions stdGto;
        stdGto = new GolgiTransportOptions();
        stdGto.setValidityPeriod(20);

        DBG.write("LoginActivity.getGolgiChatServerNameFromGCCServer() with Common.maybeEmailUserName = " + maybeEmailUserName);

        // Set this back up timer to handle it hanging when No Comms at all
        Message msg = handler.obtainMessage(EVENT2);
        handler.sendMessageDelayed(msg, 30000); // Set Delay of 30 Seconds

        golgiChatService.sendEmail.sendTo(new golgiChatService.sendEmail.ResultReceiver() {
                                              @Override
                                              public void failure(GolgiException ex)
                                              {
                                                  DBG.write("SendTo sendEmail Transaction Failed: " + ex.getErrText());
                                                  LoginActivity.this.runOnUiThread(new Runnable()
                                                  {
                                                      @Override
                                                      public void run()
                                                      {
                                                          handler.removeMessages(EVENT2); // Can cancel this as no Longer needed

                                                          showProgress(false);
                                                          mInfoText.setText("Communication Problem, Try Again");
                                                          mEmailView.setEnabled(true);
                                                          mEmailSignInButton.setEnabled(true);
                                                      }
                                                  });
                                              }

                                              @Override
                                              public void success(ServerAddress myGolgiChatServer)
                                              {
                                                  DBG.write("Received Server name to use for GolGiChat in this domain >" + myGolgiChatServer.getServerAddress() + "<");
                                                  handler.removeMessages(EVENT2); // Can cancel this as no Longer needed

                                                  SharedPreferences prefs = getSharedPreferences("GolgiChatPrefs", Context.MODE_PRIVATE);
                                                  SharedPreferences.Editor editor = prefs.edit();
                                                  editor.putString("golgiChatServer", myGolgiChatServer.getServerAddress());
                                                  editor.commit();
                                                  Common.golgiChatServer = myGolgiChatServer.getServerAddress();

                                                  // Register Entered UserName with GolgiChat Server
                                                  sendUserNameToGolgiChatServerForRegistration(Common.golgiChatServer);


                                              }
                                          },
                stdGto,
                Common.GLOBALCONFIGURATIONSERVER,
                maybeEmailUserName);
    }

    private void sendUserNameToGolgiChatServerForRegistration(final String serverToUse) {
        GolgiTransportOptions stdGto;
        stdGto = new GolgiTransportOptions();
        stdGto.setValidityPeriod(20);

        RegInfo ri = new RegInfo();
        ri.setRegName(email);
        ri.setRegId(Common.regId);


        golgiChatService.register.sendTo(new golgiChatService.register.ResultReceiver() {
                                             @Override
                                             public void failure(GolgiException ex) {
                                                 DBG.write("SendTo in Register transaction Failed: " + ex.getErrText());
                                                 // TODO: Add Toast to indicate problem with Network perhaps
                                                 LoginActivity.this.runOnUiThread(new Runnable() {
                                                     @Override
                                                     public void run() {
                                                         showProgress(false);
                                                         mInfoText.setText("Communication Problem , Try Again");
                                                         mEmailView.setEnabled(true);
                                                         mEmailSignInButton.setEnabled(true);
                                                     }
                                                 });
                                             }

                                             @Override
                                             public void success(RegCode rc)
                                             {
                                                 DBG.write("Send Register Successto Server >" + serverToUse + "<" );
                                                 // All is OK
                                                 if (rc.getCode() == 1)
                                                 {
                                                     //Common.maybeNickName = email;
                                                     //DBG.write("MaybeNickName is " + Common.maybeNickName);
                                                     // Might aswell get contacts at this point
                                                     // Fill out MaybeNickName to exclude YOURSELF
                                                     // Start a Timer
                                                    // Message msg = handler.obtainMessage(EVENT1);
                                                    // handler.sendMessageDelayed(msg, 120000); // Set Delay of 60 Seconds

                                                     getContactsFromServer(Common.golgiChatServer);


                                                     LoginActivity.this.runOnUiThread(new Runnable()
                                                     {
                                                         @Override
                                                         public void run()
                                                         {
                                                             showProgress(false);
                                                             //Things went ok, just wait for Email with Verification Code
                                                             //mInfoText.setText("Verification Email Contaning Link Sent to Your Email Account");
                                                             //mEmailSignInButton.setEnabled(false);

                                                             // Need to wait for verification entry here, either E-Mail or Code from E-Mail
                                                             // Change Text Input Field
                                                             // Change Button listener
                                                             mInfoText.setText("Enter Code received in verification E-Mail");
                                                             mEmailSignInButton.setVisibility(View.GONE);
                                                             mEmailView.setVisibility(View.GONE);

                                                             mEnterCodeView.setVisibility(View.VISIBLE);
                                                             mEnterCodeButton.setVisibility(View.VISIBLE);

                                                         }
                                                     });
                                                 }
                                                 // Not Unique Error
                                                 else if (rc.getCode() == 0)
                                                 {
                                                     LoginActivity.this.runOnUiThread(new Runnable() {
                                                         @Override
                                                         public void run()
                                                         {
                                                             showProgress(false);
                                                             mEmailView.setEnabled(true);
                                                             mEmailSignInButton.setEnabled(true);

                                                             mEmailView.setError(getString(R.string.error_invalid_email));
                                                             mEmailView.requestFocus();
                                                             mInfoText.setText("Email Already Registered");
                                                         }
                                                     });
                                                 }
                                             }
                                         },
                stdGto,
                serverToUse,
                ri
        );
    }


    private void getContactsFromServer(String mgcsn) {
        GolgiTransportOptions stdGto;
        stdGto = new GolgiTransportOptions();
        stdGto.setValidityPeriod(30);

        DBG.write("LoginActivity.getContactsFromServer() with mgcsn = " + mgcsn);

        golgiChatService.getContactInfo.sendTo(new golgiChatService.getContactInfo.ResultReceiver()
                                               {
                                                   @Override
                                                   public void failure(GolgiException ex)
                                                   {
                                                       DBG.write("SendTo getContacInfo Transaction Failed: " + ex.getErrText());
                                                       DBG.write("Could be due to empty list arriving which is fine");
                                                   }

                                                   @Override
                                                   public void success(AllContactInfo aci) {

                                                       ArrayList<RegInfo> alri;
                                                       alri = aci.getContactList();

                                                       int ii = 0;
                                                       for (RegInfo ri : alri) {
                                                           // Don't add YOURSELF to Contact List or unverified YOURSELF
                                                           if (!((ri.getRegName().equals(Common.nickName) == true) || (ri.getRegName().equals(Common.maybeNickName)) == true)) {
                                                               try {
                                                                   DBG.write("Writing Contact " + ri.getRegName() + " With RegId " + ri.getRegId() + " regname by Array position " + alri.get(ii).getRegName());
                                                                   ii++;
                                                                   ContentValues values = new ContentValues(3);
                                                                   values.put(DataProvider.COL_NAME, ri.getRegName());
                                                                   values.put(DataProvider.COL_REGID, ri.getRegId());

                                                                   String capitalFirstLetter = ri.getRegName().substring(0, 1).toLowerCase();
                                                                   DBG.write("First Letter of Contact =  >" + capitalFirstLetter + "<");
                                                                   values.put(DataProvider.COL_IMAGE_REF, capitalFirstLetter);

                                                                   getContentResolver().insert(DataProvider.CONTENT_URI_CONTACTS, values);
                                                               } catch (SQLException sqle) {
                                                                   DBG.write("AddContactDialog SQL Error!!" + sqle.getMessage());
                                                               }
                                                           }
                                                       }
                                                   }
                                               },
                stdGto,
                mgcsn);
    }


    private boolean isEmailValid(String email)
    {
        //TODO: Replace this with your own logic
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password)
    {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle)
    {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor)
    {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }


    private void addEmailsToAutoComplete(List<String> emailAddressCollection)
    {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }



    @Override
    public void onPause()
    {
        DBG.write("LoginActivity.onPause()");
        Common.inFg = false;
        GolgiAPI.useEphemeralConnection();
        super.onPause();
    }
    @Override
    public void onResume()
    {
        DBG.write("LoginActivity.onResume()");
        GolgiAPI.usePersistentConnection();
        Common.inFg = true;


        DBG.write("Nickname at this point is = >" + Common.nickName + "<");
        if (!(Common.nickName.equals("")) )
        {
            DBG.write("+++++++++++++++++++++++++++ Going Back to Main Activity as we have already been verified !! +++++++++++++++++++++++++++++++++");
//            Intent i = new Intent(LoginActivity.this, MainActivity.class);
//            i.setAction(Intent.ACTION_MAIN);
//            i.addCategory(Intent.CATEGORY_LAUNCHER);
//            startActivity(i);
            finish();
        }

        super.onResume();
    }


}




package com.openmindnetworks.golgichat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.*;


import java.util.Map;
import java.util.HashMap;



import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStreamWriter;

import com.openmindnetworks.golgi.JavaType;
import com.openmindnetworks.golgi.api.GolgiAPI;
import com.openmindnetworks.golgi.api.GolgiAPIHandler;
import com.openmindnetworks.golgi.api.GolgiAPIImpl;
import com.openmindnetworks.golgi.api.GolgiAPINetworkImpl;
import com.openmindnetworks.golgi.api.GolgiException;
import com.openmindnetworks.golgi.api.GolgiTransportOptions;
import com.openmindnetworks.slingshot.ntl.NTL;
import com.openmindnetworks.slingshot.tbx.TBX;

import com.openmindnetworks.golgichat.gen.*;




class UserNameInfo implements Serializable
{
   String regId;
   boolean verified;
   String verifCode;
}




public class Server  implements GolgiAPIHandler{

    public static final String SERIALIZE_FILE = "serializedHashMapOfUserstoRegIds";

    private String devKey = null;
    private String appKey = null;
    private String nameKey = null;

    private GolgiTransportOptions stdGto;
    private GolgiTransportOptions hourGto;
    private GolgiTransportOptions dayGto;


    Map<String, UserNameInfo> userNameToInfoMap = new HashMap<String, UserNameInfo>();

    @Override
    public void registerSuccess() {

        String fdt = getCurrentTimeStamp();

        System.out.println("| " + fdt + " | " + "---------------------------- Starting New Session -------------------------------------------------------"); 
        System.out.println("");
        System.out.println("| " + fdt + " | " + "Registered successfully with Golgi API Using DevKey = >" + devKey + "<" + " and AppKey = >" + appKey + "<");
    }


    @Override
    public void registerFailure() 
    {
        String fdt = getCurrentTimeStamp();
        System.err.println("| " + fdt + " | " + "Failed to register with Golgi API");
        System.exit(-1);
    }

    static void abort(String err) 
    {
        //String fdt = getCurrentTimeStamp();
        System.err.println("Error: " + err);
        System.exit(-1);
    }

    private  String getCurrentTimeStamp()
    {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM h:mm:ss a");
        String fdt = sdf.format(date);
       // System.out.println(fdt); 
        return fdt;
    }


    private golgiChatService.verifyCode.RequestReceiver inboundVerifyCode = new golgiChatService.verifyCode.RequestReceiver()
    {
        public void receiveFrom(golgiChatService.verifyCode.ResultSender resultSender, String vcode , String regName, String regId)
        {
            RegCode rc = new RegCode();
            UserNameInfo uni = new UserNameInfo();


            String fdt = getCurrentTimeStamp();

            System.out.println( "| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 
            System.out.println( "| " + fdt + " | " + "RECIEVED MESSAGE IN VerifyCode Transaction =>" + "<=" 
                              + ", Register Name = " + regName
                              + ", Verification Code = " + vcode );
  
            // Get User info for this userName
            if (userNameToInfoMap.containsKey( regName ))
            {
               System.out.println("| " + fdt + " | " + "Username Found, Get Verification Code, "); 
               uni = userNameToInfoMap.get(regName);

               if ( vcode.equals(uni.verifCode)   )
               {
                   rc.setCode(1);
                   System.out.println("| " + fdt + " | " + "Verification Code Match!, Incoming Code = >" + vcode + "< " + "Stored Code = >" + uni.verifCode + "<"); 
                   resultSender.success(rc);


                   // Before storing this new User send an update to all users in the Array to update there
                   // Contacts with this potential new user and RegId
                   //
                   // TO DO - Need always to have at least one enrty to avoid corruption in the transaction
                   String destRegName;
                   String destRegId ;
                   for (Map.Entry<String, UserNameInfo> cursor : userNameToInfoMap.entrySet())
                   {
                        destRegName = cursor.getKey() ;
                        destRegId =  cursor.getValue().regId ;

                // Only send Verified Users or two test IOS devices and not to person registering
//if ((!(destRegName.equals(regName))) && ( (cursor.getValue().verified == true) || (cursor.getKey().equals("derek@ios.com")) || ( cursor.getKey().equals("derek@mac.com")) )  )
                        if ((!(destRegName.equals(regName))) && ( (cursor.getValue().verified == true))   )
                        {
                             sendAddOrUpdateContactToDest(regName, regId , destRegId);
                        }
                   }


                   // Verified at this point!
                   uni.verified = true;
                   userNameToInfoMap.put(regName, uni );
 
                //  Need to write to the File here too as the HashMap has Changed
                try
                {
                      FileOutputStream fos = new FileOutputStream(SERIALIZE_FILE);
                      ObjectOutputStream oos = new ObjectOutputStream(fos);
                      oos.writeObject(userNameToInfoMap);
                      oos.close();
                      fos.close();
                      System.out.printf("| " + fdt + " | " + "Saving Verified userName  Field to File\n");
                }catch(IOException ioe)
                {
                      ioe.printStackTrace();
                }
                    
                   return;
               }
               else
               {
                   rc.setCode(0);
                   System.out.println("| " + fdt + " | " + "NO Code Match!, Incoming Code = >" + vcode + "< " + "Stored Code = >" + uni.verifCode + "<"); 
               }
            }
            else
            {
                   rc.setCode(2);
                   System.out.println("| " + fdt + " | " + "No Username found to Verify Code, "); 
            }

            System.out.println("| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 
            resultSender.success(rc);
        }
    };

    private golgiChatService.register.RequestReceiver inboundRegister = new golgiChatService.register.RequestReceiver()
    {
        public void receiveFrom(golgiChatService.register.ResultSender resultSender, RegInfo regInfo)
        {
            RegCode rc = new RegCode();
            UserNameInfo uni = new UserNameInfo();

            String fdt = getCurrentTimeStamp();

            System.out.println("| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 
            System.out.println("| " + fdt + " | " + "RECIEVED MESSAGE IN Register Transaction =>" + "<=" 
                              + ", Register Name = " + regInfo.getRegName()
                              + ", Register ID = " + regInfo.getRegId() );

            // Add Username to map if it is not already added
            // If already their then send back false as userName is already used
            if (userNameToInfoMap.containsKey( regInfo.getRegName() ))
            {
               System.out.println("| " + fdt + " | " + "Username already used, "); 
               rc.setCode(0);

               // ----------------HACK for Multiple SAME UserNames ------------------------------
               // This is a HAck so that new users with the SAME UID get there RegID updated
               // So no need to restart server all the time
               // Just overWrites RegID for this UID
               uni.regId = regInfo.getRegId();
               // Not yet verified
               uni.verified = false;
               uni.verifCode = "1234";


               // This will basically over write the existing entry with the new RegId
               // Can do something similar for Multiple Devices in the Future
               userNameToInfoMap.put(regInfo.getRegName(), uni );
               rc.setCode(1);
               //
               // ----------------HACK for Multiple SAME UserNames ------------------------------
            }
            // UserName not used yet, store name and assoc regId
            else
            {
               System.out.println("| " + fdt + " | " + "Username not used, storing userName and related Info"); 
               uni.regId = regInfo.getRegId();
               // Not yet verified
               uni.verified = false;
               uni.verifCode = "1234";

               userNameToInfoMap.put(regInfo.getRegName(), uni );
               rc.setCode(1);
            }

            resultSender.success(rc);

            // Only do this if a new name is been registered
            if (rc.getCode() == 1)
            {
                // IF Registration was OK then send EMail with verification code
                // And wait for Verification Transaction
                System.out.printf("| " + fdt + " | " + "Sending Verification Email to User\n");
                String executableStringToCall = "./sendGolgiChatVerificationEmail.pl " + regInfo.getRegName() + " " + uni.verifCode;
                System.out.printf("Executable String is = " + executableStringToCall);

                try
                {
                   Runtime.getRuntime().exec(executableStringToCall);
                }catch(IOException ioe)
                {
                   ioe.printStackTrace();
                }

                // Write out the userNameToInfoMap HashMap to a file each time a new user is added
                try
                {
                      FileOutputStream fos = new FileOutputStream(SERIALIZE_FILE);
                      ObjectOutputStream oos = new ObjectOutputStream(fos);
                      oos.writeObject(userNameToInfoMap);
                      oos.close();
                      fos.close();
                      System.out.printf("| " + fdt + " | " + "Saving Added userName to File\n");
                }catch(IOException ioe)
                {
                      ioe.printStackTrace();
                }
           }
            System.out.println("| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 
        }
    };


    private void sendAddOrUpdateContactToDest(String newRegName, String newRegId, String destRegId)
    {
            final String fdt = getCurrentTimeStamp();

            System.out.println("| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 
            System.out.println("| " + fdt + " | " + "Sending updated Name =  " + newRegName  + " to " + destRegId); 

            GolgiTransportOptions stdGto;
            stdGto = new GolgiTransportOptions();
            stdGto.setValidityPeriod(100000);  // Just over ONE DAY
            
            golgiChatService.updateOrAddContact.sendTo(new golgiChatService.updateOrAddContact.ResultReceiver() {
                @Override
                public void failure(GolgiException ex) 
                {
                     System.out.println("| " + fdt + " | " + "Send Failure in updateOrAddContact Transaction" + ex.getMessage() + ex.getCause() ); 
                }

                @Override
                public void success() 
                {
                     System.out.println("| " + fdt + " | " + "Send success in updateOrAddContact Transaction"); 
                }
            },
            stdGto, 
            destRegId, 
            newRegName, 
            newRegId);

            System.out.println("| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 
    }






    private golgiChatService.getContactInfo.RequestReceiver inboundGetContactInfo = new golgiChatService.getContactInfo.RequestReceiver()
    {
        public void receiveFrom(golgiChatService.getContactInfo.ResultSender resultSender)
        {
            String fdt = getCurrentTimeStamp();

            System.out.println("| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 
            System.out.println("| " + fdt + " | " + "RECIEVED MESSAGE IN GetContactInfo Transaction from = >" + "<=" );

            //RegInfo ri = new RegInfo();
            // Go Through array of stored names and create an array to return
            ArrayList<RegInfo> alri = new ArrayList<RegInfo>();

            
            // TO DO - Need always to have at least one enrty to avoid corruption in the transaction
            // Maybe OK as causes exception in receiving code which might be OK
            for (Map.Entry<String, UserNameInfo> cursor : userNameToInfoMap.entrySet())
            {
                RegInfo ri = new RegInfo();
                ri.setRegName( cursor.getKey() );
                ri.setRegId( cursor.getValue().regId );

                // Only send Verified Users or two test IOS devices
                //if (  ( cursor.getValue().verified == true)  ||  (cursor.getKey().equals("derek@ios.com"))   || ( cursor.getKey().equals("derek@mac.com")) )
                if (  ( cursor.getValue().verified == true)  )
                {
                    alri.add(ri);
                }
            }


            AllContactInfo aci = new AllContactInfo();
            aci.setContactList(alri);
            resultSender.success(aci);

            System.out.println("| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 

       }
    };



    private golgiChatService.getRegInfo.RequestReceiver inboundGetRegInfo = new golgiChatService.getRegInfo.RequestReceiver()
    {
        public void receiveFrom(golgiChatService.getRegInfo.ResultSender resultSender, String regName)
        {
            String fdt = getCurrentTimeStamp();

            RegInfo ri = new RegInfo();
            UserNameInfo uni = new UserNameInfo();

            System.out.println("| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 
            System.out.println("| " + fdt + " | " + "RECIEVED MESSAGE IN GetRegInfo Transaction =>" + "<=" 
                              + ", Register Name = " + regName );

            //  Check Username Map for valid UserName
            if (userNameToInfoMap.containsKey( regName ))
            {
               uni = userNameToInfoMap.get(regName);
               ri.setRegName (regName);
               ri.setRegId (uni.regId);
               System.out.println("| " + fdt + " | " + "UserName Matched  =>" + "<=" 
                              + ", Register Name = " + regName 
                              + ", Register Id = " + uni.regId );
            }
            // UserName not found, fill out reginfo with empty strings
            else
            {
               System.out.println("| " + fdt + " | " + "Username not Matched"); 
               ri.setRegName ("");
               ri.setRegId ("");
            }
            
            resultSender.success(ri);
            System.out.println("| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 
        }
    };



    public void looper()
    {
      String fdt = getCurrentTimeStamp();
      try
      {
         FileInputStream fis = new FileInputStream(SERIALIZE_FILE);
         ObjectInputStream ois = new ObjectInputStream(fis);
         userNameToInfoMap = (HashMap) ois.readObject();
         ois.close();
         fis.close();
      }
      catch(FileNotFoundException fnf)
      {
         System.out.println("| " + fdt + " | " + "No File with Stored UserNames found, Should not be a problem");
         //return;
      }
      catch(IOException ioe)
      {
         ioe.printStackTrace();
         //return;
      }
      catch(ClassNotFoundException c)
      {
         System.out.println("| " + fdt + " | " + "Class not found");
         c.printStackTrace();
         //return;
      }



      // Just Print out the List of Contacts Stored if it exists
      String destRegName;
      String destRegId ;
      boolean destVerified;
      String destVcode;

      System.out.println("| " + fdt + " | " + "----------------------- List Of Stored Conatacts -----------------------------------"); 
      for (Map.Entry<String, UserNameInfo> cursor : userNameToInfoMap.entrySet())
      {
           destRegName = cursor.getKey() ;
           destRegId =  cursor.getValue().regId ;
           destVerified =  cursor.getValue().verified ;
           destVcode =  cursor.getValue().verifCode ;
           System.out.println("| " + fdt + " | " + "Stored Name  =>"  
                              + ", Register Name = >" + destRegName + "< "
                              + ", Register Id = >" + destRegId  + "< "
                              + ", Verification Code Sent = >" + destVcode  + "< "
                              + ", Verified Yes/No = >" + destVerified  + "< ");
      }
      System.out.println("| " + fdt + " | " + "-------------------------------------------------------------------------------------"); 







        Class<GolgiAPI> apiRef = GolgiAPI.class;
        GolgiAPINetworkImpl impl = new GolgiAPINetworkImpl();
        GolgiAPI.setAPIImpl(impl);

        GolgiAPI.setOption("USE_TEST_SERVER","0");

        // Register the following receivers to handle inbound requests
        golgiChatService.register.registerReceiver(inboundRegister);
        golgiChatService.getRegInfo.registerReceiver(inboundGetRegInfo);
        golgiChatService.getContactInfo.registerReceiver(inboundGetContactInfo);
        golgiChatService.verifyCode.registerReceiver(inboundVerifyCode);


        GolgiAPI.register(devKey,
                          appKey,
                          nameKey,
                          this);
        
        
    }
    
    public Server(String[] args){
        for(int i = 0; i < args.length; i++){
	    if(args[i].compareTo("-devKey") == 0){
		devKey = args[i+1];
		i++;
	    }
	    else if(args[i].compareTo("-appKey") == 0){
		appKey = args[i+1];
		i++;
	    }
	    else if(args[i].compareTo("-name") == 0){
		nameKey = args[i+1];
		i++;
	    }
	    else{
		System.err.println("Zoikes, unrecognised option '" + args[i] + "'");
		System.exit(-1);
	    }
        }
	if(devKey == null){
	    System.out.println("No -devKey specified");
	    System.exit(-1);
	}
	else if(appKey == null){
	    System.out.println("No -appKey specified");
	    System.exit(-1);
	}
        System.out.println("");
        System.out.println("Server Started, Calling Registration to Golgi");
        System.out.println("");
    }
        
    public static void main(String[] args) {
        (new Server(args)).looper();
    }
}

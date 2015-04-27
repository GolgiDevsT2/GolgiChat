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
        System.out.println("");
        System.out.println("Registered successfully with Golgi API Using DevKey = >" + devKey + "<" + " and AppKey = >" + appKey + "<");
    }


    @Override
    public void registerFailure() {
        System.err.println("Failed to register with Golgi API");
        System.exit(-1);
    }

    static void abort(String err) {
        System.err.println("Error: " + err);
        System.exit(-1);
    }

    private golgiChatService.sendEmail.RequestReceiver inboundSendEmail = new golgiChatService.sendEmail.RequestReceiver()
    {
        public void receiveFrom(golgiChatService.sendEmail.ResultSender resultSender, String emailAddress)
        {
            System.out.println("------------------------------------------------------------------------------------------------------------------------------"); 
            System.out.println("GCCSERVER: RECIEVED MESSAGE IN SendEmail Transaction =>" + "<=" );
            ServerAddress machine = new ServerAddress();

            machine.setServerAddress("GCREGISTRATIONSERVER");
            System.out.println("Got email Address >" + emailAddress + "<" + " , Sending back Server to Use For GolgiChat = >" + "GCREGESTRATIONSERVER" + "<");

            resultSender.success(machine);
            System.out.println("------------------------------------------------------------------------------------------------------------------------------"); 
        }
    };

    public void looper()
    {

        Class<GolgiAPI> apiRef = GolgiAPI.class;
        GolgiAPINetworkImpl impl = new GolgiAPINetworkImpl();
        GolgiAPI.setAPIImpl(impl);

        GolgiAPI.setOption("USE_TEST_SERVER","0");

        golgiChatService.sendEmail.registerReceiver(inboundSendEmail);


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

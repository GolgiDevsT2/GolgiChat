namespace java com.openmindnetworks.golgichat.gen

struct RegInfo
{
    1: required string regName,
    2: required string regId
}

struct AllContactInfo
{
       1:required	list<RegInfo>	contactList
}

struct RegCode
{
    1: required i32 code
}

struct GroupMembers{
       1:required	list<string>	iList,
       2:required	list<string>	rList
}

struct LocationInfo{
       1:required	string	lat,
       2:required	string	lon,
       3:required	string	time
}

struct ServerAddress {
    1: required string serverAddress
}


service golgiChat
{
    RegCode  register(1:RegInfo regInfo),
    RegInfo  getRegInfo(1:string regName),
    AllContactInfo  getContactInfo(),
    //RegCode  userMessageTransfer(1:string userMessage, 2:string senderName, 3:string groupName, 4:GroupMembers groupMembers),
    RegCode  userMessageTransfer(1:string userMessage, 2:string senderName, 3:string userRegId ,  4:string fullGroupName, 5:string groupName,  6:GroupMembers groupMembers, 7:LocationInfo locInfo, 8:string sendTime),
    void sendMeAnEmail(1:string emailAddress, 2:string myGolgiId),
    ServerAddress  sendEmail(1:string emailAddress),
    RegCode verifyCode(1:string vcode, 2:string regName, 3:string regId),
    void updateOrAddContact(1:string regName, 2:string regCode),
    void typingStatus(1:string regName, 2:string textToDisplay),
    void seenReceipt(1:string regName, 2:string messageSendTime),
}

// This service defintion will result in a GolgiChat.java file being generated
// in the "gen" directory. 
// In this file you will find all the API methods you need to use
// Golgi to implement this simple service.
//
// There are four key methods of interest:
//
//
// Client Side:
// sentTo - You send your Greeting struct here
// ResultReceiver - You get your response here with the Greeting struct
//
//
// Server Side:
// ReceiveFrom - You get the Greeting struct given to you here
// ResultSender - You send back your response in a Greeting struct here

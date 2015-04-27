# GolgiChat
GolgiChat is an example of a messaging application with server side co ordination for registration and Contact/Group member co ordination.

* Server side element with two Java Endpoints GCCSERVER/GCREGISTRATIONSERVER
* An Android Application implmenting the GolgiChat Client

To build and run GolgiChat there are some common steps that must be taken:

* Install the latest version of the Golgi SDK somewhere (on OS X it will go under ~/Golgi-Pkg).
* In the developer portal at https://devs.golgi.io, create a new application, call it GolgiChat.
* In the top level directory create two files:
  * `Golgi.DevKey`: a single line which has your Developer Key from the developer portal
  * `Golgi.AppKey` a single line which has your Application Key for GA1 from the developer portal.
* For location services a new Google Maps V2 API key needs to be used and placed in the Manifest file






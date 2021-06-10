Solar2d Yodo1 MAS plugin<br>
=====
This is Android Rivendell plugin for Cordova.

When you integrate the SDK, you import the SDK library file into your project so you can use that file’s functions. Integrating the MAS SDK into your mobile app project gives you access to MAS's fully managed monetization solution. This solution taps into multiple add mediation platforms, selecting the one best suited for your game and monetizing through high quality ad networks that serve in-app ads.

Before you can start monetizing your app, you’ll need to integrate the MAS SDK.

Integrating the SDK
----------
First of all, you need download this repository. 

## Platform SDK supported ##
* latest Android, Min android sdk version: 19

Take a look at offitial documetation [here](https://docs.coronalabs.com/native/android/index.html).

In order to build Solar2d project you need to open android project in Android Studio.

he project contains of two modules:
* app - application module.
* plugin - module with MAS SDK plugin. LuaLoader class is an entry point for the plugin.


## Apk compilation ##
In the AndroidManifest.xml of  app module you need to put your admob id for android platforms. Also, you can rename package there.

Android:
```
    <config-file target="AndroidManifest.xml" parent="/manifest/application" >
      <meta-data
              android:name="com.google.android.gms.ads.APPLICATION_ID"
              android:value="<PUT ADMOB ID HERE>"/>
    </config-file>
```

## Code example ##
in Corona folder there is a script main.lua which contains a example. Remember,  that if you want to start the project in corona emulator you have to comment all code
related to the MAS plugin.


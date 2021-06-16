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


In order to build Solar2d project you need to open android project in Android Studio.

The project contains of two modules:
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


## Syntax and functions
If you want to use rivendell SDK in you lua script you have to import the SDK first. So on the top of the lua script add this line:
```lua
local _, rivendell = pcall(require, "plugin.rivendell")
```
Now you are able to call rivendell methods in the script.

## Code manual

### Set GDPR, COPPA, CCPA
1. rivendell.setGDPR(arg)
2. rivendell.setCOPPA(arg)
3. rivendell.setCCPA(arg)
Where *arg* - boolean argument.

### Set App Ids.
Initializes the Rivendell plugin. This call is required and must be executed before making other Rivendell calls.

**Syntax**
```lua
rivendell.init(listener, appId )
```
* listener - handle all sdk events.
* appId - app id of the application. You can find your app id in MAS dashboard. AppId should be set up for android platform.

**Example**
```lua

local appId = "n/a" -- app id variable


-- set app id sepaately for android
if platformName == "Android" then
  appId = "ANDROID APP ID HERE"
else
  print "Unsupported platform"
end

-- listener
local rivendellListener = function(event)
  processEventTable(event)

  if (event.phase == "loaded") then
    if (event.type == "interstitial") then
      setGreen(iReady)
    elseif (event.type == "rewardedVideo") then
      setGreen(rReady)
    elseif (event.type == "banner") then
      setGreen(bReady)
    end
  end
end

-- initialize Rivendell
if rivendell then rivendell.init(rivendellListener, appId) end
```

### Banner integration
1. **rivendell.isBannerAdLoaded()** returns true/false if banner loaded or not.
2. **rivendell.showBannerAdWithAlign(bannerAlign)** where bannerAlign - position of the banner on the screen.
3. rivendell.dismissBannerAd()

### Interstitial integration
1. **rivendell.isInterstitialAdLoaded()** returns true/false if interstitial loaded or not.
2. **rivendell.showInterstitialAd()** displays interstitial.

### Reward video integration
1. **rivendell.isRewardedAdLoaded()** returns true/false if reward video loaded or not.
2. **rivendell.showRewardedAd()** displays rewarded video.

//
// LuaLoader.java
// Yodo1 Rivendell Plugin
//
// Copyright (c) 2021 Yodo1. All rights reserved.
//

// @formatter:off

package plugin.rivendell;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaLuaEvent;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTaskDispatcher;
import com.google.android.gms.ads.AdView;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.LuaType;
import com.naef.jnlua.NamedJavaFunction;
import com.yodo1.mas.Yodo1Mas;
import com.yodo1.mas.error.Yodo1MasError;
import com.yodo1.mas.event.Yodo1MasAdEvent;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// Plugin imports

/**
 * Implements the Lua interface for the Rivendell Plugin.
 * <p>
 * Only one instance of this class will be created by Corona for the lifetime of the application.
 * This instance will be re-used for every new Corona activity that gets created.
 */
@SuppressWarnings({"unused", "RedundantSuppression"})
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
    private static final String PLUGIN_NAME = "plugin.rivendell";
    private static final String PLUGIN_VERSION = "1.0.0";
    private static final String PLUGIN_SDK_VERSION = "0";//getVersionString();

    private static final String EVENT_NAME = "adsRequest";
    private static final String PROVIDER_NAME = "rivendell";

    // response keys
    private static final String RESPONSE_LOAD_FAILED = "loadFailed";

    // missing Corona Event Keys
    private static final String EVENT_PHASE_KEY = "phase";
    private static final String EVENT_TYPE_KEY = "type";
    private static final String EVENT_DATA_KEY = "data";

    // event data keys
    private static final String DATA_ERRORMSG_KEY = "errorMsg";
    private static final String DATA_ERRORCODE_KEY = "errorCode";
    private static final String DATA_ADUNIT_ID_KEY = "adUnitId";

    // message constants
    private static final String CORONA_TAG = "Corona";
    private static final String ERROR_MSG = "ERROR: ";
    private static final String WARNING_MSG = "WARNING: ";

    private static String functionSignature = "";                             // used in error reporting functions
    private static final Map<String, Object> rivendellObjects = new HashMap<>();  // keep track of loaded objects

    // object dictionary keys
    private static final String HAS_RECEIVED_INIT_EVENT_KEY = "hasReceivedInitEvent";

    private static int coronaListener = CoronaLua.REFNIL;
    private static CoronaRuntimeTaskDispatcher coronaRuntimeTaskDispatcher = null;

    private static void invalidateAllViews() {
        final CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
        if (activity != null) {
            invalidateChildren(activity.getWindow().getDecorView());
        }
    }

    private static void invalidateChildren(View v) {
        if (v instanceof ViewGroup) {
            ViewGroup viewgroup = (ViewGroup) v;
            for (int i = 0; i < viewgroup.getChildCount(); i++) {
                invalidateChildren(viewgroup.getChildAt(i));
            }
        }
        v.invalidate();
    }


    // -------------------------------------------------------
    // Plugin lifecycle events
    // -------------------------------------------------------

    @SuppressWarnings("unused")
    public LuaLoader() {
        CoronaEnvironment.addRuntimeListener(this);
    }

    @Override
    public int invoke(LuaState L) {
        // Register this plugin into Lua with the following functions.
        NamedJavaFunction[] luaFunctions = new NamedJavaFunction[]{
                new SetGDPR(),
                new SetCCPA(),
                new SetCOPPA(),

                new Init(),

                new IsBannerAdLoaded(),
                new ShowBannerAd(),
                new ShowBannerAdWithAlign(),
                new DismissBannerAd(),

                new IsInterstitialAdLoaded(),
                new ShowInterstitialAd(),

                new IsRewardedAdLoaded(),
                new ShowRewardedAd()
        };
        String libName = L.toString(1);
        L.register(libName, luaFunctions);

        // Returning 1 indicates that the Lua require() function will return the above Lua library
        return 1;
    }

    @Override
    public void onLoaded(CoronaRuntime runtime) {
        if (coronaRuntimeTaskDispatcher == null) {
            coronaRuntimeTaskDispatcher = new CoronaRuntimeTaskDispatcher(runtime);

            rivendellObjects.put(HAS_RECEIVED_INIT_EVENT_KEY, false);
        }
    }

    @Override
    public void onStarted(CoronaRuntime runtime) {
    }

    @Override
    public void onSuspended(CoronaRuntime runtime) {
        final CoronaActivity coronaActivity = CoronaEnvironment.getCoronaActivity();

        if (coronaActivity != null) {
            coronaActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String adUnitId = (String) rivendellObjects.get(AdType.BANNER.getValue());
                    if (adUnitId != null) {
                        AdView banner = (AdView) rivendellObjects.get(adUnitId);
                        if (banner != null) {
                            banner.pause();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onResumed(CoronaRuntime runtime) {
        final CoronaActivity coronaActivity = CoronaEnvironment.getCoronaActivity();

        if (coronaActivity != null) {
            coronaActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String adUnitId = (String) rivendellObjects.get(AdType.BANNER.getValue());
                    if (adUnitId != null) {
                        AdView banner = (AdView) rivendellObjects.get(adUnitId);
                        if (banner != null) {
                            banner.resume();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onExiting(final CoronaRuntime runtime) {
        final CoronaActivity coronaActivity = CoronaEnvironment.getCoronaActivity();

        if (coronaActivity != null) {
            coronaActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (runtime != null) {
                        CoronaLua.deleteRef(runtime.getLuaState(), coronaListener);
                    }
                    coronaListener = CoronaLua.REFNIL;

                    rivendellObjects.clear();
                    coronaRuntimeTaskDispatcher = null;
                    functionSignature = "";
                }
            });
        }
    }

    // --------------------------------------------------------------------------
    // helper functions
    // --------------------------------------------------------------------------

    // log message to console
    private void logMsg(String msgType, String errorMsg) {
        String functionID = functionSignature;
        if (!functionID.isEmpty()) {
            functionID += ", ";
        }

        Log.i(CORONA_TAG, msgType + functionID + errorMsg);
    }

    private boolean isSDKInitialized() {
        // check to see if SDK is properly initialized
        if (coronaListener == CoronaLua.REFNIL) {
            logMsg(ERROR_MSG, "rivendell.init() must be called before calling other API functions");
            return false;
        }

        if (!(boolean) rivendellObjects.get(HAS_RECEIVED_INIT_EVENT_KEY)) {
            logMsg(ERROR_MSG, "You must wait for the 'init' event before calling other API functions");
            return false;
        }

        return true;
    }

    // dispatch a Lua event to our callback (dynamic handling of properties through map)
    private void dispatchLuaEvent(final Map<String, Object> event) {
        if (coronaRuntimeTaskDispatcher != null) {
            coronaRuntimeTaskDispatcher.send(runtime -> {
                try {
                    LuaState L = runtime.getLuaState();
                    CoronaLua.newEvent(L, EVENT_NAME);
                    boolean hasErrorKey = false;

                    // add event parameters from map
                    for (String key : event.keySet()) {
                        CoronaLua.pushValue(L, event.get(key));           // push value
                        L.setField(-2, key);                              // push key

                        if (!hasErrorKey) {
                            hasErrorKey = key.equals(CoronaLuaEvent.ISERROR_KEY);
                        }
                    }

                    // add error key if not in map
                    if (!hasErrorKey) {
                        L.pushBoolean(false);
                        L.setField(-2, CoronaLuaEvent.ISERROR_KEY);
                    }

                    // add provider
                    L.pushString(PROVIDER_NAME);
                    L.setField(-2, CoronaLuaEvent.PROVIDER_KEY);

                    CoronaLua.dispatchEvent(L, coronaListener, 0);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    // -------------------------------------------------------
    // plugin implementation
    // -------------------------------------------------------

    // [Lua] init(listener, options)
    private class Init implements NamedJavaFunction {
        @Override
        public String getName() {
            return "init";
        }

        @Override
        public int invoke(final LuaState luaState) {
            functionSignature = "rivendell.init(listener, appId)";

            // prevent init from being called twice
            if (coronaListener != CoronaLua.REFNIL) {
                logMsg(WARNING_MSG, "init() should only be called once");
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if (nargs != 2) {
                logMsg(ERROR_MSG, "Expected 2 arguments, got " + nargs);
                return 0;
            }

            // Get the listener (required)
            if (CoronaLua.isListener(luaState, 1, PROVIDER_NAME)) {
                coronaListener = CoronaLua.newRef(luaState, 1);
            } else {
                logMsg(ERROR_MSG, "Listener expected, got: " + luaState.typeName(1));
                return 0;
            }

            String appId;
            if (luaState.type(2) == LuaType.STRING) {
                appId = luaState.toString(2);
            } else {
                logMsg(ERROR_MSG, "String expected, got: " + luaState.typeName(1));
                return 0;
            }

            // declare final variables for inner loop
            final CoronaActivity coronaActivity = CoronaEnvironment.getCoronaActivity();

            initBannerAd();
            initInterstitialAd();
            initRewardedAd();

            Yodo1Mas.getInstance().init(coronaActivity, appId, new Yodo1Mas.InitListener() {
                @Override
                public void onMasInitSuccessful() {
                    Log.i(CORONA_TAG, PLUGIN_NAME + ": " + PLUGIN_VERSION + " (SDK: " + PLUGIN_SDK_VERSION + ")");
                    Log.i(CORONA_TAG, PLUGIN_NAME + " initialize successful");

                    rivendellObjects.put(HAS_RECEIVED_INIT_EVENT_KEY, true);

                    Map<String, Object> coronaEvent = new HashMap<>();
                    coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_INIT);
                    dispatchLuaEvent(coronaEvent);
                }

                @Override
                public void onMasInitFailed(@NonNull Yodo1MasError error) {
                    Log.i(CORONA_TAG, PLUGIN_NAME + " initialize error");

                    JSONObject data = new JSONObject();
                    try {
                        data.put(DATA_ERRORMSG_KEY, error.getMessage());
                        data.put(DATA_ERRORCODE_KEY, error.getCode());

                        Map<String, Object> coronaEvent = new HashMap<>();
                        coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_INIT);
                        coronaEvent.put(EVENT_DATA_KEY, data.toString());
                        dispatchLuaEvent(coronaEvent);
                    } catch (Exception e) {
                        System.err.println();
                    }
                }
            });


            return 0;
        }
    }


    private class SetGDPR implements NamedJavaFunction {

        @Override
        public String getName() {
            return "setGDPR";
        }

        @Override
        public int invoke(LuaState luaState) {
            functionSignature = "rivendell.setGDPR()";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if (nargs != 1) {
                logMsg(ERROR_MSG, "Expected 1 argument, got " + nargs);
                return 0;
            }

            boolean gdpr;

            // get the gdpr
            if (luaState.type(1) == LuaType.BOOLEAN) {
                gdpr = luaState.toBoolean(1);
            } else {
                logMsg(ERROR_MSG, "gdpr (bool) expected, got " + luaState.typeName(1));
                return 0;
            }

            Yodo1Mas.getInstance().setGDPR(gdpr);

            return 0;
        }
    }

    private class SetCCPA implements NamedJavaFunction {

        @Override
        public String getName() {
            return "setCCPA";
        }

        @Override
        public int invoke(LuaState luaState) {
            functionSignature = "rivendell.setCCPA()";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if (nargs != 1) {
                logMsg(ERROR_MSG, "Expected 1 argument, got " + nargs);
                return 0;
            }

            boolean ccpa;

            // get the ccpa
            if (luaState.type(1) == LuaType.BOOLEAN) {
                ccpa = luaState.toBoolean(1);
            } else {
                logMsg(ERROR_MSG, "ccpa (bool) expected, got " + luaState.typeName(1));
                return 0;
            }

            Yodo1Mas.getInstance().setCCPA(ccpa);

            return 0;
        }
    }

    private class SetCOPPA implements NamedJavaFunction {

        @Override
        public String getName() {
            return "setCOPPA";
        }

        @Override
        public int invoke(LuaState luaState) {
            functionSignature = "rivendell.setCOPPA()";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if (nargs != 1) {
                logMsg(ERROR_MSG, "Expected 1 argument, got " + nargs);
                return 0;
            }

            boolean coppa;

            // get the coppa
            if (luaState.type(1) == LuaType.BOOLEAN) {
                coppa = luaState.toBoolean(1);
            } else {
                logMsg(ERROR_MSG, "coppa (bool) expected, got " + luaState.typeName(1));
                return 0;
            }

            Yodo1Mas.getInstance().setCOPPA(coppa);

            return 0;
        }
    }

    /* Banner
     * ********************************************************************** */

    // [Lua] isBannerAdLoaded()
    private class IsBannerAdLoaded implements NamedJavaFunction {
        @Override
        public String getName() {
            return "isBannerAdLoaded";
        }

        @Override
        public int invoke(final LuaState luaState) {
            functionSignature = "rivendell.isBannerAdLoaded()";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if ((nargs > 0)) {
                logMsg(ERROR_MSG, "Expected 0 arguments, got " + nargs);
                return 0;
            }

            boolean isLoaded = Yodo1Mas.getInstance().isBannerAdLoaded();
            Log.i(CORONA_TAG, PLUGIN_NAME + " IsBannerLoaded: " + isLoaded);

            luaState.pushBoolean(isLoaded);

            return 1;
        }
    }


    // [Lua] showBannerAd()
    private class ShowBannerAd implements NamedJavaFunction {
        @Override
        public String getName() {
            return "showBannerAd";
        }

        @Override
        public int invoke(final LuaState luaState) {
            functionSignature = "rivendell.showBannerAd()";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if ((nargs > 0)) {
                logMsg(ERROR_MSG, "Expected 0 arguments, got " + nargs);
                return 0;
            }

            final CoronaActivity coronaActivity = CoronaEnvironment.getCoronaActivity();

            coronaActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    boolean isBannerAdLoaded = Yodo1Mas.getInstance().isBannerAdLoaded();
                    Log.i(CORONA_TAG, PLUGIN_NAME + " IsBannerLoaded: " + isBannerAdLoaded);

                    if(!isBannerAdLoaded) {

                        try {
                            Map<String, Object> coronaEvent = new HashMap<>();
                            coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_FAILED);
                            coronaEvent.put(EVENT_TYPE_KEY, AdType.BANNER.getValue());
                            dispatchLuaEvent(coronaEvent);
                        } catch (Exception e) {
                            System.err.println();
                        }

                        return;
                    }

                    Yodo1Mas.getInstance().showBannerAd(coronaActivity);
                }
            });
            return 0;
        }
    }


    // [Lua] showBannerAdWithAlign()
    private class ShowBannerAdWithAlign implements NamedJavaFunction {
        @Override
        public String getName() {
            return "showBannerAdWithAlign";
        }

        @Override
        public int invoke(final LuaState luaState) {
            functionSignature = "rivendell.showBannerAdWithAlign(align)";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if (nargs != 1) {
                logMsg(ERROR_MSG, "Expected 1 argument, got " + nargs);
                return 0;
            }

            int align;

            // get the align
            if (luaState.type(1) == LuaType.NUMBER) {
                align = luaState.toInteger(1);
            } else {
                logMsg(ERROR_MSG, "align (int) expected, got " + luaState.typeName(1));
                return 0;
            }

            // declare final variables for inner loop
            final CoronaActivity coronaActivity = CoronaEnvironment.getCoronaActivity();

            coronaActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    boolean isBannerAdLoaded = Yodo1Mas.getInstance().isBannerAdLoaded();
                    Log.i(CORONA_TAG, PLUGIN_NAME + " IsBannerLoaded: " + isBannerAdLoaded);

                    if(!isBannerAdLoaded) {

                        try {
                            Map<String, Object> coronaEvent = new HashMap<>();
                            coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_FAILED);
                            coronaEvent.put(EVENT_TYPE_KEY, AdType.BANNER.getValue());
                            dispatchLuaEvent(coronaEvent);
                        } catch (Exception e) {
                            System.err.println();
                        }

                        return;
                    }

                    Yodo1Mas.getInstance().showBannerAd(coronaActivity, align);
                }
            });
            return 0;
        }
    }

    // [Lua] showBannerAdWithAlignAndOffset()
    private class ShowBannerAdWithAlignAndOffset implements NamedJavaFunction {
        @Override
        public String getName() {
            return "showBannerAdWithAlignAndOffset";
        }

        @Override
        public int invoke(final LuaState luaState) {
            functionSignature = "rivendell.showBannerAdWithAlign(align, offsetX, offsetY)";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if (nargs != 2) {
                logMsg(ERROR_MSG, "Expected 3 argument, got " + nargs);
                return 0;
            }

            int align;
            int offsetX;
            int offsetY;

            // get the align
            if (luaState.type(1) == LuaType.NUMBER) {
                align = luaState.toInteger(1);
            } else {
                logMsg(ERROR_MSG, "align (int) expected, got " + luaState.typeName(1));
                return 0;
            }

            // get the offsetX
            if (luaState.type(2) == LuaType.NUMBER) {
                offsetX = luaState.toInteger(1);
            } else {
                logMsg(ERROR_MSG, "offsetX (int) expected, got " + luaState.typeName(2));
                return 0;
            }

            // get the offsetY
            if (luaState.type(3) == LuaType.NUMBER) {
                offsetY = luaState.toInteger(1);
            } else {
                logMsg(ERROR_MSG, "offsetY (int) expected, got " + luaState.typeName(3));
                return 0;
            }

            // declare final variables for inner loop
            final CoronaActivity coronaActivity = CoronaEnvironment.getCoronaActivity();

            coronaActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    boolean isBannerAdLoaded = Yodo1Mas.getInstance().isBannerAdLoaded();
                    Log.i(CORONA_TAG, PLUGIN_NAME + " IsBannerLoaded: " + isBannerAdLoaded);

                    if(!isBannerAdLoaded) {

                        try {
                            Map<String, Object> coronaEvent = new HashMap<>();
                            coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_FAILED);
                            coronaEvent.put(EVENT_TYPE_KEY, AdType.BANNER.getValue());
                            dispatchLuaEvent(coronaEvent);
                        } catch (Exception e) {
                            System.err.println();
                        }

                        return;
                    }

                    Yodo1Mas.getInstance().showBannerAd(coronaActivity, align, offsetX, offsetY);
                }
            });
            return 0;
        }
    }



    // [Lua] dismissBannerAd()
    private class DismissBannerAd implements NamedJavaFunction {

        @Override
        public String getName() {
            return "dismissBannerAd";
        }

        @Override
        public int invoke(final LuaState luaState) {
            functionSignature = "rivendell.dismissBannerAd()";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if (nargs != 0) {
                logMsg(ERROR_MSG, "Expected 0 arguments, got " + nargs);
                return 0;
            }

            // declare final variables for inner loop
            final CoronaActivity coronaActivity = CoronaEnvironment.getCoronaActivity();
            coronaActivity.runOnUiThread(() -> Yodo1Mas.getInstance().dismissBannerAd());
            return 0;
        }
    }


    /* Interstitial
     * ********************************************************************** */

    // [Lua] isInterstitialAdLoaded()
    private class IsInterstitialAdLoaded implements NamedJavaFunction {
        @Override
        public String getName() {
            return "isInterstitialAdLoaded";
        }

        @Override
        public int invoke(final LuaState luaState) {
            functionSignature = "rivendell.isInterstitialAdLoaded()";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if ((nargs > 0)) {
                logMsg(ERROR_MSG, "Expected 0 arguments, got " + nargs);
                return 0;
            }

            boolean isLoaded = Yodo1Mas.getInstance().isInterstitialAdLoaded();
            Log.i(CORONA_TAG, PLUGIN_NAME + " isInterstitialAdLoaded: " + isLoaded);

            luaState.pushBoolean(isLoaded);

            return 1;
        }
    }

    // [Lua] showInterstitialAd()
    private class ShowInterstitialAd implements NamedJavaFunction {
        @Override
        public String getName() {
            return "showInterstitialAd";
        }

        @Override
        public int invoke(final LuaState luaState) {
            functionSignature = "rivendell.showInterstitialAd()";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if ((nargs > 0)) {
                logMsg(ERROR_MSG, "Expected 0 arguments, got " + nargs);
                return 0;
            }

            final CoronaActivity coronaActivity = CoronaEnvironment.getCoronaActivity();

            coronaActivity.runOnUiThread(() -> {

                boolean isInterstitialAdLoaded = Yodo1Mas.getInstance().isInterstitialAdLoaded();
                Log.i(CORONA_TAG, PLUGIN_NAME + " isInterstitialAdLoaded: " + isInterstitialAdLoaded);

                if(!isInterstitialAdLoaded) {

                    try {
                        Map<String, Object> coronaEvent = new HashMap<>();
                        coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_FAILED);
                        coronaEvent.put(EVENT_TYPE_KEY, AdType.INTERSTITIAL.getValue());
                        dispatchLuaEvent(coronaEvent);
                    } catch (Exception e) {
                        System.err.println();
                    }

                    return;
                }

                Yodo1Mas.getInstance().showInterstitialAd(coronaActivity);
            });
            return 0;
        }
    }

    /* Rewarded Video
     * ********************************************************************** */

    // [Lua] isRewardedAdLoaded()
    private class IsRewardedAdLoaded implements NamedJavaFunction {
        @Override
        public String getName() {
            return "isRewardedAdLoaded";
        }

        @Override
        public int invoke(final LuaState luaState) {
            functionSignature = "rivendell.isRewardedAdLoaded()";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if ((nargs > 0)) {
                logMsg(ERROR_MSG, "Expected 0 arguments, got " + nargs);
                return 0;
            }

            boolean isLoaded = Yodo1Mas.getInstance().isInterstitialAdLoaded();
            Log.i(CORONA_TAG, PLUGIN_NAME + " isRewardedAdLoaded: " + isLoaded);

            luaState.pushBoolean(isLoaded);

            return 1;
        }
    }

    // [Lua] showRewardedAd()
    private class ShowRewardedAd implements NamedJavaFunction {
        @Override
        public String getName() {
            return "showRewardedAd";
        }

        @Override
        public int invoke(final LuaState luaState) {
            functionSignature = "rivendell.showRewardedAd()";

            if (!isSDKInitialized()) {
                return 0;
            }

            // check number of args
            int nargs = luaState.getTop();
            if ((nargs > 0)) {
                logMsg(ERROR_MSG, "Expected 0 arguments, got " + nargs);
                return 0;
            }

            final CoronaActivity coronaActivity = CoronaEnvironment.getCoronaActivity();

            coronaActivity.runOnUiThread(() -> {

                boolean isRewardedAdLoaded = Yodo1Mas.getInstance().isRewardedAdLoaded();
                Log.i(CORONA_TAG, PLUGIN_NAME + " isRewardedAdLoaded: " + isRewardedAdLoaded);

                if(!isRewardedAdLoaded) {

                    try {
                        Map<String, Object> coronaEvent = new HashMap<>();
                        coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_FAILED);
                        coronaEvent.put(EVENT_TYPE_KEY, AdType.REWARDED_VIDEO.getValue());
                        dispatchLuaEvent(coronaEvent);
                    } catch (Exception e) {
                        System.err.println();
                    }

                    return;
                }

                Yodo1Mas.getInstance().showRewardedAd(coronaActivity);
            });
            return 0;
        }
    }

    // -------------------------------------------------------------------
    // Delegates
    // -------------------------------------------------------------------

    private void initBannerAd() {
        Yodo1Mas.getInstance().setBannerListener(new Yodo1Mas.BannerListener() {
            @Override
            public void onAdOpened(@NonNull Yodo1MasAdEvent event) {
                Log.i(CORONA_TAG, PLUGIN_NAME +  "Banner onAdOpened");

                Map<String, Object> coronaEvent = new HashMap<>();
                coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_OPENED);
                coronaEvent.put(EVENT_TYPE_KEY, AdType.BANNER.getValue());
                dispatchLuaEvent(coronaEvent);
            }

            @Override
            public void onAdError(@NonNull Yodo1MasAdEvent event, @NonNull Yodo1MasError error) {
                Log.i(CORONA_TAG, PLUGIN_NAME + " Banner onAdError: " + error.getCode());

                JSONObject data = new JSONObject();
                try {
                    data.put(DATA_ERRORMSG_KEY, error.getMessage());
                    data.put(DATA_ERRORCODE_KEY, error.getCode());

                    Map<String, Object> coronaEvent = new HashMap<>();
                    coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_FAILED);
                    coronaEvent.put(EVENT_TYPE_KEY, AdType.BANNER.getValue());
                    coronaEvent.put(CoronaLuaEvent.RESPONSE_KEY, RESPONSE_LOAD_FAILED);
                    coronaEvent.put(EVENT_DATA_KEY, data.toString());
                    dispatchLuaEvent(coronaEvent);
                } catch (Exception e) {
                    System.err.println();
                }
            }

            @Override
            public void onAdClosed(@NonNull Yodo1MasAdEvent event) {
                Log.i(CORONA_TAG, PLUGIN_NAME + " Banner onAdClosed");

                Map<String, Object> coronaEvent = new HashMap<>();
                coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_CLOSED);
                coronaEvent.put(EVENT_TYPE_KEY, AdType.BANNER.getValue());
                dispatchLuaEvent(coronaEvent);
            }
        });
    }

    private void initInterstitialAd() {
        Yodo1Mas.getInstance().setInterstitialListener(new Yodo1Mas.InterstitialListener() {
            @Override
            public void onAdOpened(@NonNull Yodo1MasAdEvent event) {
                Log.i(CORONA_TAG, PLUGIN_NAME + " Interstitial onAdOpened");

                Map<String, Object> coronaEvent = new HashMap<>();
                coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_OPENED);
                coronaEvent.put(EVENT_TYPE_KEY, AdType.INTERSTITIAL.getValue());
                dispatchLuaEvent(coronaEvent);
            }

            @Override
            public void onAdError(@NonNull Yodo1MasAdEvent event, @NonNull Yodo1MasError error) {
                Log.i(CORONA_TAG, PLUGIN_NAME + " Interstitial onAdError: " + error.getCode());

                JSONObject data = new JSONObject();
                try {
                    data.put(DATA_ERRORMSG_KEY, error.getMessage());
                    data.put(DATA_ERRORCODE_KEY, error.getCode());

                    Map<String, Object> coronaEvent = new HashMap<>();
                    coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_FAILED);
                    coronaEvent.put(EVENT_TYPE_KEY, AdType.BANNER.getValue());
                    coronaEvent.put(CoronaLuaEvent.RESPONSE_KEY, RESPONSE_LOAD_FAILED);
                    coronaEvent.put(EVENT_DATA_KEY, data.toString());
                    dispatchLuaEvent(coronaEvent);
                } catch (Exception e) {
                    System.err.println();
                }
            }

            @Override
            public void onAdClosed(@NonNull Yodo1MasAdEvent event) {
                Log.i(CORONA_TAG, PLUGIN_NAME + " Interstitial onAdClosed");

                Map<String, Object> coronaEvent = new HashMap<>();
                coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_CLOSED);
                coronaEvent.put(EVENT_TYPE_KEY, AdType.BANNER.getValue());
                dispatchLuaEvent(coronaEvent);
            }
        });
    }

    private void initRewardedAd() {
        Yodo1Mas.getInstance().setRewardListener(new Yodo1Mas.RewardListener() {
            @Override
            public void onAdOpened(@NonNull Yodo1MasAdEvent event) {
                Log.i(CORONA_TAG, PLUGIN_NAME + " RewardedAd onAdOpened");

                Map<String, Object> coronaEvent = new HashMap<>();
                coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_OPENED);
                coronaEvent.put(EVENT_TYPE_KEY, AdType.REWARDED_VIDEO.getValue());
                dispatchLuaEvent(coronaEvent);
            }

            @Override
            public void onAdvertRewardEarned(@NonNull Yodo1MasAdEvent event) {
                Log.i(CORONA_TAG, PLUGIN_NAME + " RewardedAd onAdvertRewardEarned");

                Map<String, Object> coronaEvent = new HashMap<>();
                coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_REWARD);
                coronaEvent.put(EVENT_TYPE_KEY, AdType.REWARDED_VIDEO.getValue());
                dispatchLuaEvent(coronaEvent);
            }

            @Override
            public void onAdError(@NonNull Yodo1MasAdEvent event, @NonNull Yodo1MasError error) {
                Log.i(CORONA_TAG, PLUGIN_NAME + " RewardedAd onAdError: " + error.getCode());

                JSONObject data = new JSONObject();
                try {
                    data.put(DATA_ERRORMSG_KEY, error.getMessage());
                    data.put(DATA_ERRORCODE_KEY, error.getCode());

                    Map<String, Object> coronaEvent = new HashMap<>();
                    coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_FAILED);
                    coronaEvent.put(EVENT_TYPE_KEY, AdType.REWARDED_VIDEO.getValue());
                    coronaEvent.put(CoronaLuaEvent.RESPONSE_KEY, RESPONSE_LOAD_FAILED);
                    coronaEvent.put(EVENT_DATA_KEY, data.toString());
                    dispatchLuaEvent(coronaEvent);
                } catch (Exception e) {
                    System.err.println();
                }
            }

            @Override
            public void onAdClosed(@NonNull Yodo1MasAdEvent event) {
                Log.i(CORONA_TAG, PLUGIN_NAME + " RewardedAd onAdClosed");

                Map<String, Object> coronaEvent = new HashMap<>();
                coronaEvent.put(EVENT_PHASE_KEY, PhaseType.PHASE_CLOSED);
                coronaEvent.put(EVENT_TYPE_KEY, AdType.REWARDED_VIDEO.getValue());
                dispatchLuaEvent(coronaEvent);
            }
        });
    }


    // -------------------------------------------------------------------


}

//
// LuaLoader.java
// Yodo1 Rivendell Plugin
//
// Copyright (c) 2021 Yodo1. All rights reserved.
//

// @formatter:off

package plugin.rivendell;

import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.NamedJavaFunction;

import java.util.List;

// Plugin imports

/**
 * Implements the Lua interface for the Rivendell Plugin.
 * <p>
 * Only one instance of this class will be created by Corona for the lifetime of the application.
 * This instance will be re-used for every new Corona activity that gets created.
 */
@SuppressWarnings({"unused", "RedundantSuppression"})
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {


    private final Rivendell rivendell;
    // -------------------------------------------------------
    // Plugin lifecycle events
    // -------------------------------------------------------

    @SuppressWarnings("unused")
    public LuaLoader() {
        CoronaEnvironment.addRuntimeListener(this);
        this.rivendell = new Rivendell();

    }

    @Override
    public int invoke(LuaState L) {
// Register this plugin into Lua with the following functions.
        List<NamedJavaFunction> functionList =  this.rivendell.getLuaFunctions();
        NamedJavaFunction[] luaFunctions = new NamedJavaFunction[functionList.size()];
        functionList.toArray(luaFunctions);
        String libName = L.toString(1);
        L.register(libName, luaFunctions);

        // Returning 1 indicates that the Lua require() function will return the above Lua library
        return 1;
    }

    @Override
    public void onLoaded(CoronaRuntime runtime) {
        this.rivendell.onLoaded(runtime);
    }

    @Override
    public void onStarted(CoronaRuntime runtime) {

    }

    @Override
    public void onSuspended(CoronaRuntime runtime) {
        this.rivendell.onSuspended(runtime);
    }

    @Override
    public void onResumed(CoronaRuntime runtime) {
        this.rivendell.onResumed(runtime);
    }

    @Override
    public void onExiting(final CoronaRuntime runtime) {
        this.rivendell.onExiting(runtime);
    }
}

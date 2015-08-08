package ua.naiksoftware.simpletanks;

import android.content.Context;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ResourceFinder;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility for initialization Lua runtime and some other functions.
 * <p>
 * Created by Naik on 03.08.15.
 */
public class Lua implements ResourceFinder {

    private final Context context;
    private final Globals globals;

    public Lua(Context context) {
        this.context = context;
        globals = JsePlatform.standardGlobals();
        globals.finder = this;
    }

    /**
     * Load code to runtime
     *
     * @param chunk key
     * @param code  lua code
     * @return loaded lua code
     */
    public LuaValue load(Object chunk, String code) {
        return globals.load(code, chunk.toString());
    }

    /**
     * Platform depended loading resources for LuaJ.
     *
     * @param path path to file
     * @return stream or null
     */
    @Override
    public InputStream findResource(String path) {
        try {
            return context.getAssets().open(path);
        } catch (IOException e) {
            return null;
        }
    }
}

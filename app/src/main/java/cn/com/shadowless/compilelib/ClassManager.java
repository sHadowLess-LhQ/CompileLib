package cn.com.shadowless.compilelib;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity管理
 *
 * @author sHadowLess
 */
public enum ClassManager {

    /**
     * Instance my activity manager.
     */
    INSTANCE;

    private final Map<String, Class<?>> dexMap = new HashMap<>();


    /**
     * Get current activity activity.
     *
     * @param name the name
     * @return the activity
     */
    public Class<?> getInitCls(String name) {
        return dexMap.get(name);
    }

    /**
     * Set current activity.
     *
     * @param name the name
     * @param cls  the cls
     */
    public void addInitClass(String name, Class<?> cls) {
        dexMap.put(name, cls);
    }

}

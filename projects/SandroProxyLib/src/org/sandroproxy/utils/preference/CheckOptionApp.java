package org.sandroproxy.utils.preference;

import java.util.Map;

public class CheckOptionApp {
    
    public static int ALL_UID = -2;
    public static int ALL_5228_PORT = -3;
    /*
     * Watch http 
     */
    public boolean WH;
    /*
     * Watch https
     */
    public boolean WHS;

    /*
     * Custom address
     */
    public String CA;
    /*
     * Application Uid
     */
    public int AUid;
    /*
     * Application names
     */
    public String AN;
    
    /*
     * Custom rules enabled
     */
    public boolean CE;
    
    /*
     * List of custom rules
     */
    public Map<Integer, CheckOptionCustomPorts> CustomPortRules;
}

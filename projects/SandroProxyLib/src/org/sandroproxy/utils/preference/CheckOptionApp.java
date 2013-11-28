package org.sandroproxy.utils.preference;

import java.util.Map;

public class CheckOptionApp {
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

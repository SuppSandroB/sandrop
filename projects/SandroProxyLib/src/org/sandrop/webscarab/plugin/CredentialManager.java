/***********************************************************************
 *
 * This file is part of SandroProxy, 
 * For details, please see http://code.google.com/p/sandrop/
 *
 * Copyright (c) 2012 supp.sandrob@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Getting Source
 * ==============
 *
 * Source for this application is maintained at
 * http://code.google.com/p/sandrop/
 *
 * Software is build from sources of WebScarab project
 * For details, please see http://www.sourceforge.net/projects/owasp
 *
 */

package org.sandrop.webscarab.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.sandrop.webscarab.httpclient.AuthDigestManager;
import org.sandrop.webscarab.httpclient.Authenticator;
import org.sandrop.webscarab.model.HttpUrl;
import org.sandrop.webscarab.model.Preferences;
import org.sandrop.webscarab.util.Encoding;

public class CredentialManager implements Authenticator {
    
    // contains Maps per host, indexed by Realm
    private Map<String, Map<String, BasicCredential>> _basicCredentials = new TreeMap<String, Map<String, BasicCredential>>();
    private Map<String, Map<String, DigestCredential>> _digestCredentials = new TreeMap<String, Map<String, DigestCredential>>();
    private Map<String, DomainCredential> _domainCredentials = new TreeMap<String, DomainCredential>();
    
    private CredentialManagerUI _ui = null;
    
    /** Creates a new instance of CredentialManager */
    public CredentialManager() {
    }
    
    public void setUI(CredentialManagerUI ui) {
        _ui = ui;
    }
    
    public synchronized String getCredentials(HttpUrl url, String[] challenges) {
        String creds = getPreferredCredentials(url.getHost(), challenges);
        if (creds != null) return creds;
        boolean prompt = Boolean.valueOf(Preferences.getPreference("WebScarab.promptForCredentials", "false")).booleanValue();
        if (prompt && _ui != null && challenges != null && challenges.length > 0) {
            boolean ask = false;
            for (int i=0; i<challenges.length; i++){
                if (challenges[i] != null){
                    String challengeLowerCase = challenges[i].toLowerCase();
                    if (challengeLowerCase.startsWith("basic") || 
                            challengeLowerCase.startsWith("ntlm") || 
                            challengeLowerCase.startsWith("negotiate") || 
                            challengeLowerCase.startsWith("digest")){
                        ask = true;
                    }
                }
            }
            if (ask){
                _ui.requestCredentials(url.getHost(), challenges);
            }
        }
        return getPreferredCredentials(url.getHost(), challenges);
    }
    
    public synchronized String getProxyCredentials(String hostname, String[] challenges) {
        String creds = getPreferredCredentials(hostname, challenges);
        if (creds != null) return creds;
        boolean prompt = Boolean.valueOf(Preferences.getPreference("WebScarab.promptForCredentials", "false")).booleanValue();
        if (prompt && _ui != null && challenges != null && challenges.length > 0) {
            boolean ask = false;
            for (int i=0; i<challenges.length; i++){
                if (challenges[i] != null){
                    String challengeLowerCase = challenges[i].toLowerCase();
                    if (challengeLowerCase.startsWith("basic") || 
                            challengeLowerCase.startsWith("ntlm") || 
                            challengeLowerCase.startsWith("negotiate") || 
                            challengeLowerCase.startsWith("digest")){
                        ask = true;
                    }
                }
                
            }
            if (ask)
                _ui.requestCredentials(hostname, challenges);
        }
        return getPreferredCredentials(hostname, challenges);
    }
    
    public void addBasicCredentials(BasicCredential cred) {
        if ((cred.getUsername() == null || cred.getUsername().equals("")) && (cred.getPassword() == null || cred.getPassword().equals(""))) return;
        Map<String, BasicCredential> realms = _basicCredentials.get(cred.getHost());
        if (realms == null) {
            realms = new TreeMap<String, BasicCredential>();
            _basicCredentials.put(cred.getHost(), realms);
        }
        realms.put(cred.getRealm(), cred);
    }
    
    public void addDigestCredentials(DigestCredential cred) {
        if ((cred.getUsername() == null || cred.getUsername().equals("")) && (cred.getPassword() == null || cred.getPassword().equals(""))) return;
        Map<String, DigestCredential> realms = _digestCredentials.get(cred.getHost());
        if (realms == null) {
            realms = new TreeMap<String, DigestCredential>();
            _digestCredentials.put(cred.getHost(), realms);
        }
        realms.put(cred.getRealm(), cred);
    }
    
    public void addDomainCredentials(DomainCredential cred) {
        if ((cred.getUsername() == null || cred.getUsername().equals("")) && (cred.getPassword() == null || cred.getPassword().equals(""))) return;
        _domainCredentials.put(cred.getHost(), cred);
    }
    
    public int getBasicCredentialCount() {
        return getAllBasicCredentials().length;
    }
    
    public BasicCredential getBasicCredentialAt(int index) {
        return getAllBasicCredentials()[index];
    }
    
    public void deleteBasicCredentialAt(int index) {
        int i = -1;
        Iterator<String> hosts = _basicCredentials.keySet().iterator();
        while (hosts.hasNext()) {
            Map<String, BasicCredential> realms = _basicCredentials.get(hosts.next());
            Iterator<String> realm = realms.keySet().iterator();
            while (realm.hasNext()) {
                String key = realm.next();
                i++;
                if (i == index)
                    realms.remove(key);
            }
        }
    }
    
    public int getDomainCredentialCount() {
        return _domainCredentials.entrySet().size();
    }
    
    public DomainCredential getDomainCredentialAt(int index) {
        List<DomainCredential> all = new ArrayList<DomainCredential>();
        Iterator<String> hosts = _domainCredentials.keySet().iterator();
        while (hosts.hasNext())
            all.add(_domainCredentials.get(hosts.next()));
        return (DomainCredential) all.toArray(new DomainCredential[0])[index];
    }
    
    public void deleteDomainCredentialAt(int index) {
        int i = -1;
        Iterator<String> hosts = _domainCredentials.keySet().iterator();
        while (hosts.hasNext()) {
            String key = hosts.next();
            i++;
            if (i == index)
                _domainCredentials.remove(key);
        }
    }
    
    private BasicCredential[] getAllBasicCredentials() {
        List<BasicCredential> all = new ArrayList<BasicCredential>();
        Iterator<String> hosts = _basicCredentials.keySet().iterator();
        while (hosts.hasNext()) {
            Map<String, BasicCredential> realms = _basicCredentials.get(hosts.next());
            Iterator<String> realm = realms.keySet().iterator();
            while (realm.hasNext())
                all.add(realms.get(realm.next()));
        }
        return (BasicCredential[]) all.toArray(new BasicCredential[0]);
    }
    
    private String getPreferredCredentials(String host, String[] challenges) {
        // we don't do pre-emptive auth at all
        if (challenges == null || challenges.length == 0)
            return null;
        for (int i=0; i<challenges.length; i++) {
            if (challenges[i] != null){
                String challengeLowerCase = challenges[i].toLowerCase();
                if (challengeLowerCase.startsWith("basic")) {
                    String creds = getBasicCredentials(host, challengeLowerCase);
                    if (creds != null) return "Basic " + creds;
                }
            }
        }
        for (int i=0; i<challenges.length; i++) {
            if (challenges[i] != null){
                String challengeLowerCase = challenges[i].toLowerCase();
                if (challengeLowerCase.startsWith("ntlm")) {
                    String creds = getDomainCredentials(host);
                    if (creds != null) return "NTLM " + creds;
                }
            }
        }
        for (int i=0; i<challenges.length; i++) {
            if (challenges[i] != null){
                String challengeLowerCase = challenges[i].toLowerCase();
                if (challengeLowerCase.startsWith("negotiate")) {
                    String creds = getDomainCredentials(host);
                    if (creds != null) return "Negotiate " + creds;
                }
            }
        }
        for (int i=0; i<challenges.length; i++) {
            if (challenges[i] != null){
                String challengeLowerCase = challenges[i].toLowerCase();
                if (challengeLowerCase.startsWith("digest")) {
                    String creds = getDigestCredentials(host, challenges[i]);
                    if (creds != null) return "Digest " + creds;
                }
            }
        }
        return null;
    }
    
    private String getDigestCredentials(String host, String challenge) {
        int i = challenge.indexOf(' ');
        String authParameters = challenge.substring(i + 1);
        String realm = null;
        String[] params = authParameters.split(",");
        if (params != null && params.length > 0){
            for (String param : params) {
                if (param != null){
                    String[] paramParts = param.split("=");
                    String tokenName = paramParts[0].trim();
                    String tokenVal = paramParts[1].trim();
                    if (tokenVal != null && tokenName != null && tokenName.equalsIgnoreCase(AuthDigestManager.REALM_TOKEN)){
                        realm = AuthDigestManager.trimDoubleQuotesIfAny(tokenVal);
                        break;
                    }
                }
            }
        }
        Map<String, DigestCredential> realms = _digestCredentials.get(host);
        if (realms == null) return null;
        DigestCredential cred = realms.get(realm);
        if (cred == null) return null;
        String encoded = cred.getUsername() + ":" + cred.getPassword();
        return Encoding.base64encode(encoded.getBytes(), false);
    }
    
    private String getBasicCredentials(String host, String challenge) {
        String realm = challenge.substring("basic realm=\"".length(), challenge.length()-1);
        Map<String, BasicCredential> realms = _basicCredentials.get(host);
        if (realms == null) return null;
        BasicCredential cred = realms.get(realm);
        if (cred == null){
            // we should return what we have not searching that realm is the same
            if (realms.size() > 0){
                cred = realms.entrySet().iterator().next().getValue();
            }
        }
        if (cred == null) return null;
        //String credRealm = cred.getRealm();
        //String addRealm = credRealm != null && credRealm.length() > 0 ? credRealm.toUpperCase() + "\\" : "";
        String encoded = cred.getUsername() + ":" + cred.getPassword();
        return Encoding.base64encode(encoded.getBytes(), false);
    }
    
    private String getDomainCredentials(String host) {
        DomainCredential cred = _domainCredentials.get(host);
        if (cred == null) return null;
        String encoded = cred.getDomain() + "\\" + cred.getUsername() + ":" + cred.getPassword();
        return Encoding.base64encode(encoded.getBytes(), false);
    }
    
}

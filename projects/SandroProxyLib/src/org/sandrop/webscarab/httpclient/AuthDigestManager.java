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

package org.sandrop.webscarab.httpclient;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class AuthDigestManager {
    
    public final static String REALM_TOKEN = "realm";
    public final static String NONCE_TOKEN = "nonce";
    public final static String STALE_TOKEN = "stale";
    public final static String OPAQUE_TOKEN = "opaque";
    public final static String QOP_TOKEN = "qop";
    public final static String ALGORITHM_TOKEN = "algorithm";
    

    /**
     * Trims double-quotes around a parameter value if there are any.
     * @return The string value without the outermost pair of double-
     * quotes or null if the original value is null.
     */
    static public String trimDoubleQuotesIfAny(String value) {
        if (value != null) {
            int len = value.length();
            if (len > 2 &&
                value.charAt(0) == '\"' && value.charAt(len - 1) == '\"') {
                return value.substring(1, len - 1);
            }
        }
        return value;
    }
    
    /**
     * "Double-quotes" the argument.
     */
    private static String doubleQuote(String param) {
        if (param != null) {
            return "\"" + param + "\"";
        }

        return null;
    }

    /**
     * Computes a random cnonce value based on the current time.
     */
    private static String computeCnonce() {
        Random rand = new Random();
        int nextInt = rand.nextInt();
        nextInt = (nextInt == Integer.MIN_VALUE) ?
                Integer.MAX_VALUE : Math.abs(nextInt);
        return Integer.toString(nextInt, 16);
    }
    
    /**
     * @return Double-quoted MD5 digest.
     */
    private static String computeDigest(
        String A1, String A2, String nonce, String QOP, String nc, String cnonce) {
        if (QOP == null) {
            return KD(H(A1), nonce + ":" + H(A2));
        } else {
            if (QOP.equalsIgnoreCase("auth")) {
                return KD(H(A1), nonce + ":" + nc + ":" + cnonce + ":" + QOP + ":" + H(A2));
            }
        }
        return null;
    }
    
    /**
     * @return MD5 hash of concat(secret, ":", data).
     */
    private static String KD(String secret, String data) {
        return H(secret + ":" + data);
    }

    /**
     * @return MD5 hash of param.
     */
    private static String H(String param) {
        if (param != null) {
            MessageDigest md5 = null;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String charset = "UTF-8";
            try {
                md5.update(param.getBytes(charset));
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            byte[] d = md5.digest();
            if (d != null) {
                return bufferToHex(d);
            }
        }
        return null;
    }

    /**
     * @return HEX buffer representation.
     */
    private static String bufferToHex(byte[] buffer) {
        final char hexChars[] =
            { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };
        if (buffer != null) {
            int length = buffer.length;
            if (length > 0) {
                StringBuilder hex = new StringBuilder(2 * length);
                for (int i = 0; i < length; ++i) {
                    byte l = (byte) (buffer[i] & 0x0F);
                    byte h = (byte)((buffer[i] & 0xF0) >> 4);
                    hex.append(hexChars[h]);
                    hex.append(hexChars[l]);
                }
                return hex.toString();
            } else {
                return "";
            }
        }
        return null;
    }
    
    /**
     * @return Digest-scheme authentication response.
     */
    public static String computeDigestAuthResponse(String username,
                                             String password,
                                             String realm,
                                             String nonce,
                                             String QOP,
                                             String algorithm,
                                             String opaque,
                                             String method,
                                             String url) {
        String A1 = username + ":" + realm + ":" + password;
        String A2 = method  + ":" + url;
        // because we do not preemptively send authorization headers, nc is always 1
        String nc = "00000001";
        String cnonce = computeCnonce();
        String digest = computeDigest(A1, A2, nonce, QOP, nc, cnonce);
        String response = "";
        response += "username=" + doubleQuote(username) + ", ";
        response += "realm="    + doubleQuote(realm)    + ", ";
        response += "nonce="    + doubleQuote(nonce)    + ", ";
        response += "uri="      + doubleQuote(url)     + ", ";
        response += "response=" + doubleQuote(digest) ;
        if (opaque     != null) {
            response += ", opaque=" + doubleQuote(opaque);
        }
         if (algorithm != null) {
            // sandrob this makes server returns 407 again... 
            // response += ", algorithm=" +  algorithm;
        }
        if (QOP        != null) {
            response += ", qop=" + QOP + ", nc=" + nc + ", cnonce=" + doubleQuote(cnonce);
        }
        return response;
    }
}

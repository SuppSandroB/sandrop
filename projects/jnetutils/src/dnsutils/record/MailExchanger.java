/*
 * Java Network Programming, Second Edition
 * Merlin Hughes, Michael Shoffner, Derek Hamner
 * Manning Publications Company; ISBN 188477749X
 *
 * http://nitric.com/jnp/
 *
 * Copyright (c) 1997-1999 Merlin Hughes, Michael Shoffner, Derek Hamner;
 * all rights reserved; see license.txt for details.
 */

package dnsutils.record;

import java.io.*;

import dnsutils.DNSInputStream;
import dnsutils.DNSRR;


public class MailExchanger extends DNSRR {
  private int preference;
  private String mx;

  protected void decode (DNSInputStream dnsIn) throws IOException {
    preference = dnsIn.readShort ();
    mx = dnsIn.readDomainName ();
  }
  
  public String getMX () {
    return mx;
  }

  public int getPreference () {
    return preference;
  }

  public String toString () {
    return getRRName () + "\tpreference = " + preference +
      ", mail exchanger = "+ mx;
  }
}

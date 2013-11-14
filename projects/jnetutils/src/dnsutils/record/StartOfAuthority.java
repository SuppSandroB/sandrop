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


public class StartOfAuthority extends DNSRR {
  private String origin, mailAddress;
  private long serial, refresh, retry, expire, ttl;

  protected void decode (DNSInputStream dnsIn) throws IOException {
    origin = dnsIn.readDomainName ();
    mailAddress = dnsIn.readDomainName ();
    serial = dnsIn.readInt ();
    refresh = dnsIn.readInt ();
    retry = dnsIn.readInt ();
    expire = dnsIn.readInt ();
    ttl = dnsIn.readInt ();
  }

  public String getOrigin () {
    return origin;
  }

  public String getMailAddress () {
    return mailAddress;
  }

  public long getSerial () {
    return serial;
  }

  public long getRefresh () {
    return refresh;
  }

  public long getRetry () {
    return retry;
  }

  public long getExpire () {
    return expire;
  }

  public long getTTL () {
    return ttl;
  }

  public String toString () {
    return getRRName () + "\tstart of authority\n\torigin = " + origin +
      "\n\tmail address = " + mailAddress + "\n\tserial = " + serial +
      "\n\trefresh = " + refresh + "\n\tretry = " + retry +
      "\n\texpire = " + expire + "\n\tminimum TTL = " + ttl;
  }
}

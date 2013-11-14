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


public class HostInfo extends DNSRR {
  private String cpu, os;

  protected void decode (DNSInputStream dnsIn) throws IOException {
    cpu = dnsIn.readString ();
    os = dnsIn.readString ();
  }

  public String getCPUInfo () {
    return cpu;
  }

  public String getOSInfo () {
    return os;
  }

  public String toString () {
    return getRRName () + "\tOS = " + os + ", CPU = " + cpu;
  }
}

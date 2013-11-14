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


public class Null extends DNSRR {
  private byte[] data;
  private String text;

  protected void decode (DNSInputStream dnsIn) throws IOException {
    data = new byte[dnsIn.available ()];
    dnsIn.read (data);
    text = new String (data, "latin1");
  }

  public byte[] getNullData () {
    byte[] copy = new byte[data.length];
    System.arraycopy (data, 0, copy, 0, data.length);
    return copy;
  }

  public String toString () {
    return getRRName () + "\tnull data = [" + text + ']';
  }
}

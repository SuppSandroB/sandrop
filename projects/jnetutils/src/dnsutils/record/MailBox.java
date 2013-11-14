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


public class MailBox extends DNSRR {
  private String mailBox;

  protected void decode (DNSInputStream dnsIn) throws IOException {
    mailBox = dnsIn.readDomainName ();
  }

  public String getMailbox () {
    return mailBox;
  }

  public String toString () {
    return getRRName () + "\tmailbox = " + mailBox;
  }
}

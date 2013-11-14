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


public class MailDomain extends DNSRR {
  private String mailDestination;

  protected void decode (DNSInputStream dnsIn) throws IOException {
    mailDestination = dnsIn.readDomainName ();
  }

  public String getMailDestination () {
    return mailDestination;
  }

  public String toString () {
    return getRRName () + "\tmail destination = " + mailDestination;
  }
}

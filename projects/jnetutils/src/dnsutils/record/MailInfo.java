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


public class MailInfo extends DNSRR {
  private String rBox, eBox;

  protected void decode (DNSInputStream dnsIn) throws IOException {
    rBox = dnsIn.readDomainName ();
    eBox = dnsIn.readDomainName ();
  }

  public String getResponsibleMailbox () {
    return rBox;
  }

  public String getErrorMailbox () {
    return eBox;
  }

  public String toString () {
    return getRRName () + "\tresponsible mailbox = " + rBox +
      ", error mailbox = " + eBox;
  }
}

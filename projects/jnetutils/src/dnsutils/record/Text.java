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
import java.util.*;

import dnsutils.DNSInputStream;
import dnsutils.DNSRR;


public class Text extends DNSRR {
  private Vector<String> texts = new Vector<String> ();

  protected void decode (DNSInputStream dnsIn) throws IOException {
    String s;
    while ((s = dnsIn.readString ()) != null)
      texts.addElement (s);
  }

  public Enumeration<String> getTexts () {
    return texts.elements ();
  }

  public String toString () {
    StringBuffer result = new StringBuffer ();
    for (int i = 0; i < texts.size (); ++ i) {
      if (i > 0)
	result.append ("\n\t\t");
      result.append (texts.elementAt (i));
    }
    return getRRName () + "\ttext = " + result;
  }
}

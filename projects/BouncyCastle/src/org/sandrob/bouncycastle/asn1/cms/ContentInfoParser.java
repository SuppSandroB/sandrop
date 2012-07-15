package org.sandrob.bouncycastle.asn1.cms;

import java.io.IOException;

import org.sandrob.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.sandrob.bouncycastle.asn1.ASN1SequenceParser;
import org.sandrob.bouncycastle.asn1.ASN1TaggedObjectParser;
import org.sandrob.bouncycastle.asn1.DEREncodable;

/**
 * Produce an object suitable for an ASN1OutputStream.
 * <pre>
 * ContentInfo ::= SEQUENCE {
 *          contentType ContentType,
 *          content
 *          [0] EXPLICIT ANY DEFINED BY contentType OPTIONAL }
 * </pre>
 */
public class ContentInfoParser
{
    private ASN1ObjectIdentifier contentType;
    private ASN1TaggedObjectParser content;

    public ContentInfoParser(
        ASN1SequenceParser seq)
        throws IOException
    {
        contentType = (ASN1ObjectIdentifier)seq.readObject();
        content = (ASN1TaggedObjectParser)seq.readObject();
    }

    public ASN1ObjectIdentifier getContentType()
    {
        return contentType;
    }

    public DEREncodable getContent(
        int  tag)
        throws IOException
    {
        if (content != null)
        {
            return content.getObjectParser(tag, true);
        }

        return null;
    }
}

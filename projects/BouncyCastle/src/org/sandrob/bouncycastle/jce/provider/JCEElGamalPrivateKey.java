package org.sandrob.bouncycastle.jce.provider;

import org.sandrob.bouncycastle.asn1.ASN1Sequence;
import org.sandrob.bouncycastle.asn1.DEREncodable;
import org.sandrob.bouncycastle.asn1.DERInteger;
import org.sandrob.bouncycastle.asn1.DERObjectIdentifier;
import org.sandrob.bouncycastle.asn1.oiw.ElGamalParameter;
import org.sandrob.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.sandrob.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.sandrob.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.sandrob.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
import org.sandrob.bouncycastle.jce.interfaces.ElGamalPrivateKey;
import org.sandrob.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.sandrob.bouncycastle.jce.spec.ElGamalParameterSpec;
import org.sandrob.bouncycastle.jce.spec.ElGamalPrivateKeySpec;

import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Enumeration;

public class JCEElGamalPrivateKey
    implements ElGamalPrivateKey, DHPrivateKey, PKCS12BagAttributeCarrier
{
    static final long serialVersionUID = 4819350091141529678L;
        
    BigInteger      x;

    ElGamalParameterSpec   elSpec;

    private PKCS12BagAttributeCarrierImpl   attrCarrier = new PKCS12BagAttributeCarrierImpl();

    protected JCEElGamalPrivateKey()
    {
    }

    JCEElGamalPrivateKey(
        ElGamalPrivateKey    key)
    {
        this.x = key.getX();
        this.elSpec = key.getParameters();
    }

    JCEElGamalPrivateKey(
        DHPrivateKey    key)
    {
        this.x = key.getX();
        this.elSpec = new ElGamalParameterSpec(key.getParams().getP(), key.getParams().getG());
    }
    
    JCEElGamalPrivateKey(
        ElGamalPrivateKeySpec    spec)
    {
        this.x = spec.getX();
        this.elSpec = new ElGamalParameterSpec(spec.getParams().getP(), spec.getParams().getG());
    }

    JCEElGamalPrivateKey(
        DHPrivateKeySpec    spec)
    {
        this.x = spec.getX();
        this.elSpec = new ElGamalParameterSpec(spec.getP(), spec.getG());
    }
    
    JCEElGamalPrivateKey(
        PrivateKeyInfo  info)
    {
        ElGamalParameter     params = new ElGamalParameter((ASN1Sequence)info.getAlgorithmId().getParameters());
        DERInteger      derX = (DERInteger)info.getPrivateKey();

        this.x = derX.getValue();
        this.elSpec = new ElGamalParameterSpec(params.getP(), params.getG());
    }

    JCEElGamalPrivateKey(
        ElGamalPrivateKeyParameters  params)
    {
        this.x = params.getX();
        this.elSpec = new ElGamalParameterSpec(params.getParameters().getP(), params.getParameters().getG());
    }

    public String getAlgorithm()
    {
        return "ElGamal";
    }

    /**
     * return the encoding format we produce in getEncoded().
     *
     * @return the string "PKCS#8"
     */
    public String getFormat()
    {
        return "PKCS#8";
    }

    /**
     * Return a PKCS8 representation of the key. The sequence returned
     * represents a full PrivateKeyInfo object.
     *
     * @return a PKCS8 representation of the key.
     */
    public byte[] getEncoded()
    {
        PrivateKeyInfo          info = new PrivateKeyInfo(new AlgorithmIdentifier(OIWObjectIdentifiers.elGamalAlgorithm, new ElGamalParameter(elSpec.getP(), elSpec.getG()).getDERObject()), new DERInteger(getX()));

        return info.getDEREncoded();
    }

    public ElGamalParameterSpec getParameters()
    {
        return elSpec;
    }

    public DHParameterSpec getParams()
    {
        return new DHParameterSpec(elSpec.getP(), elSpec.getG());
    }
    
    public BigInteger getX()
    {
        return x;
    }

    private void readObject(
        ObjectInputStream   in)
        throws IOException, ClassNotFoundException
    {
        x = (BigInteger)in.readObject();

        this.elSpec = new ElGamalParameterSpec((BigInteger)in.readObject(), (BigInteger)in.readObject());
    }

    private void writeObject(
        ObjectOutputStream  out)
        throws IOException
    {
        out.writeObject(this.getX());
        out.writeObject(elSpec.getP());
        out.writeObject(elSpec.getG());
    }

    public void setBagAttribute(
        DERObjectIdentifier oid,
        DEREncodable        attribute)
    {
        attrCarrier.setBagAttribute(oid, attribute);
    }

    public DEREncodable getBagAttribute(
        DERObjectIdentifier oid)
    {
        return attrCarrier.getBagAttribute(oid);
    }

    public Enumeration getBagAttributeKeys()
    {
        return attrCarrier.getBagAttributeKeys();
    }
}

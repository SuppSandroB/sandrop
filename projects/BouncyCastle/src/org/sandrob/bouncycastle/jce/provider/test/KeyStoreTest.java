package org.sandrob.bouncycastle.jce.provider.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import org.sandrob.bouncycastle.jce.X509Principal;
import org.sandrob.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sandrob.bouncycastle.jce.spec.ECParameterSpec;
import org.sandrob.bouncycastle.math.ec.ECCurve;
import org.sandrob.bouncycastle.util.encoders.Hex;
import org.sandrob.bouncycastle.util.test.SimpleTest;
import org.sandrob.bouncycastle.x509.X509V3CertificateGenerator;

/**
 * Exercise the various key stores, making sure we at least get back what we put in!
 * <p>
 * This tests both the BKS, and the UBER key store.
 */
public class KeyStoreTest
    extends SimpleTest
{
    static char[]   passwd = { 'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd' };

    public void ecStoreTest(
        String  storeName)
        throws Exception
    {
        ECCurve curve = new ECCurve.Fp(
                                new BigInteger("883423532389192164791648750360308885314476597252960362792450860609699839"), // q
                                new BigInteger("7fffffffffffffffffffffff7fffffffffff8000000000007ffffffffffc", 16), // a
                                new BigInteger("6b016c3bdcf18941d0d654921475ca71a9db2fb27d1d37796185c2942c0a", 16)); // b

        ECParameterSpec ecSpec = new ECParameterSpec(
                                curve,
                                curve.decodePoint(Hex.decode("020ffa963cdca8816ccc33b8642bedf905c3d358573d3f27fbbd3b3cb9aaaf")), // G
                                new BigInteger("883423532389192164791648750360308884807550341691627752275345424702807307")); // n

        KeyPairGenerator    g = KeyPairGenerator.getInstance("ECDSA", "BC");

        g.initialize(ecSpec, new SecureRandom());

        KeyPair     keyPair = g.generateKeyPair();

        PublicKey   pubKey = keyPair.getPublic();
        PrivateKey  privKey = keyPair.getPrivate();

        //
        // distinguished name table.
        //
        Hashtable                 attrs = new Hashtable();
        Vector                    order = new Vector();

        attrs.put(X509Principal.C, "AU");
        attrs.put(X509Principal.O, "The Legion of the Bouncy Castle");
        attrs.put(X509Principal.L, "Melbourne");
        attrs.put(X509Principal.ST, "Victoria");
        attrs.put(X509Principal.E, "feedback-crypto@bouncycastle.org");

        order.addElement(X509Principal.C);
        order.addElement(X509Principal.O);
        order.addElement(X509Principal.L);
        order.addElement(X509Principal.ST);
        order.addElement(X509Principal.E);

        //
        // create the certificate - version 3
        //
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();

        certGen.setSerialNumber(BigInteger.valueOf(1));
        certGen.setIssuerDN(new X509Principal(order, attrs));
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 50000));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 50000));
        certGen.setSubjectDN(new X509Principal(order, attrs));
        certGen.setPublicKey(pubKey);
        certGen.setSignatureAlgorithm("ECDSAwithSHA1");

        Certificate[]    chain = new Certificate[1];

        try
        {
            X509Certificate cert = certGen.generate(privKey);

            cert.checkValidity(new Date());

            cert.verify(pubKey);

            ByteArrayInputStream    bIn = new ByteArrayInputStream(cert.getEncoded());
            CertificateFactory      fact = CertificateFactory.getInstance("X.509", "BC");

            cert = (X509Certificate)fact.generateCertificate(bIn);

            chain[0] = cert;
        }
        catch (Exception e)
        {
            fail("error generating cert - " + e.toString());
        }

        KeyStore store = KeyStore.getInstance(storeName, "BC");

        store.load(null, null);

        store.setKeyEntry("private", privKey, passwd, chain);

        //
        // write out and read back store
        //
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();

        store.store(bOut, passwd);

        ByteArrayInputStream    bIn = new ByteArrayInputStream(bOut.toByteArray());

        //
        // start with a new key store
        //
        store = KeyStore.getInstance(storeName, "BC");

        store.load(bIn, passwd);

        //
        // load the private key
        //
        privKey = (PrivateKey)store.getKey("private", passwd);

        //
        // double public key encoding test
        //
        byte[]              pubEnc = pubKey.getEncoded();
        KeyFactory          keyFac = KeyFactory.getInstance(pubKey.getAlgorithm(), "BC");
        X509EncodedKeySpec  pubX509 = new X509EncodedKeySpec(pubEnc);

        pubKey = (PublicKey)keyFac.generatePublic(pubX509);

        pubEnc = pubKey.getEncoded();
        keyFac = KeyFactory.getInstance(pubKey.getAlgorithm(), "BC");
        pubX509 = new X509EncodedKeySpec(pubEnc);

        pubKey = (PublicKey)keyFac.generatePublic(pubX509);

        //
        // double private key encoding test
        //
        byte[]              privEnc = privKey.getEncoded();

        keyFac = KeyFactory.getInstance(privKey.getAlgorithm(), "BC");

        PKCS8EncodedKeySpec privPKCS8 = new PKCS8EncodedKeySpec(privEnc);
        privKey = (PrivateKey)keyFac.generatePrivate(privPKCS8);

        keyFac = KeyFactory.getInstance(privKey.getAlgorithm(), "BC");
        privPKCS8 = new PKCS8EncodedKeySpec(privEnc);
        privKey = (PrivateKey)keyFac.generatePrivate(privPKCS8);
    }

    public void keyStoreTest(
        String    storeName)
        throws Exception
    {
        KeyStore store = KeyStore.getInstance(storeName, "BC");

        store.load(null, null);

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "BC");

        gen.initialize(1024, new SecureRandom());

        KeyPair         pair = gen.generateKeyPair();
        RSAPrivateKey   privKey = (RSAPrivateKey)pair.getPrivate();
        RSAPublicKey    pubKey = (RSAPublicKey)pair.getPublic();
        BigInteger      modulus = privKey.getModulus();
        BigInteger      privateExponent = privKey.getPrivateExponent();


        //
        // distinguished name table.
        //
        Hashtable                   attrs = new Hashtable();
        Vector                      order = new Vector();

        attrs.put(X509Principal.C, "AU");
        attrs.put(X509Principal.O, "The Legion of the Bouncy Castle");
        attrs.put(X509Principal.L, "Melbourne");
        attrs.put(X509Principal.ST, "Victoria");
        attrs.put(X509Principal.EmailAddress, "feedback-crypto@bouncycastle.org");

        order.addElement(X509Principal.C);
        order.addElement(X509Principal.O);
        order.addElement(X509Principal.L);
        order.addElement(X509Principal.ST);
        order.addElement(X509Principal.EmailAddress);

        //
        // extensions
        //

        //
        // create the certificate.
        //
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();

        certGen.setSerialNumber(BigInteger.valueOf(1));
        certGen.setIssuerDN(new X509Principal(order, attrs));
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 50000));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 50000));
        certGen.setSubjectDN(new X509Principal(order, attrs));
        certGen.setPublicKey(pubKey);
        certGen.setSignatureAlgorithm("MD5WithRSAEncryption");

        Certificate[]   chain = new Certificate[1];

        try
        {
            X509Certificate cert = certGen.generate(privKey);

            cert.checkValidity(new Date());

            cert.verify(pubKey);

            ByteArrayInputStream    bIn = new ByteArrayInputStream(cert.getEncoded());
            CertificateFactory      fact = CertificateFactory.getInstance("X.509", "BC");

            cert = (X509Certificate)fact.generateCertificate(bIn);

            chain[0] = cert;
        }
        catch (Exception e)
        {
            fail("error generating cert - " + e.toString());
        }

        store.setKeyEntry("private", privKey, passwd, chain);

        //
        // write out and read back store
        //
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();

        store.store(bOut, passwd);

        ByteArrayInputStream    bIn = new ByteArrayInputStream(bOut.toByteArray());

        //
        // start with a new key store
        //
        store = KeyStore.getInstance(storeName, "BC");

        store.load(bIn, passwd);

        //
        // verify public key
        //
        privKey = (RSAPrivateKey)store.getKey("private", passwd);

        if (!privKey.getModulus().equals(modulus))
        {
            fail("private key modulus wrong");
        }
        else if (!privKey.getPrivateExponent().equals(privateExponent))
        {
            fail("private key exponent wrong");
        }

        //
        // verify certificate
        //
        Certificate cert = store.getCertificateChain("private")[0];

        cert.verify(pubKey);
    }

    public String getName()
    {
        return "KeyStore";
    }

    public void performTest()
        throws Exception
    {
        keyStoreTest("BKS");
        keyStoreTest("UBER");
        ecStoreTest("BKS");
    }

    public static void main(
        String[]    args)
    {
        Security.addProvider(new BouncyCastleProvider());

        runTest(new KeyStoreTest());
    }
}

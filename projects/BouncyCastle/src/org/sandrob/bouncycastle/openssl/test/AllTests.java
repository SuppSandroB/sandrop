package org.sandrob.bouncycastle.openssl.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sandrob.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sandrob.bouncycastle.openssl.PEMReader;
import org.sandrob.bouncycastle.openssl.PEMWriter;
import org.sandrob.bouncycastle.openssl.PKCS8Generator;
import org.sandrob.bouncycastle.openssl.PasswordFinder;
import org.sandrob.bouncycastle.util.test.SimpleTestResult;

public class
    AllTests
    extends TestCase
{
    public void testOpenSSL()
    {   
        Security.addProvider(new BouncyCastleProvider());
        
        org.sandrob.bouncycastle.util.test.Test[] tests = new org.sandrob.bouncycastle.util.test.Test[]
        {
            new ReaderTest(),
            new WriterTest()
        };

        for (int i = 0; i != tests.length; i++)
        {
            SimpleTestResult  result = (SimpleTestResult)tests[i].perform();
            
            if (!result.isSuccessful())
            {
                fail(result.toString());
            }
        }
    }

    public void testPKCS8Encrypted()
        throws Exception
    {
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

        kpGen.initialize(1024);

        PrivateKey key = kpGen.generateKeyPair().getPrivate();

        encryptedTest(key, PKCS8Generator.AES_256_CBC);
        encryptedTest(key, PKCS8Generator.DES3_CBC);
        encryptedTest(key, PKCS8Generator.PBE_SHA1_3DES);
    }

    private void encryptedTest(PrivateKey key, String algorithm)
        throws NoSuchProviderException, NoSuchAlgorithmException, IOException
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pWrt = new PEMWriter(new OutputStreamWriter(bOut), "BC");
        PKCS8Generator pkcs8 = new PKCS8Generator(key, algorithm, "BC");

        pkcs8.setPassword("hello".toCharArray());
        
        pWrt.writeObject(pkcs8);

        pWrt.close();

        PEMReader pRd = new PEMReader(new InputStreamReader(new ByteArrayInputStream(bOut.toByteArray())), new PasswordFinder()
        {
            public char[] getPassword()
            {
                return "hello".toCharArray();
            }
        });

        PrivateKey rdKey = (PrivateKey)pRd.readObject();

        assertEquals(key, rdKey);
    }

    public void testPKCS8Plain()
        throws Exception
    {
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

        kpGen.initialize(1024);

        PrivateKey key = kpGen.generateKeyPair().getPrivate();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pWrt = new PEMWriter(new OutputStreamWriter(bOut));
        PKCS8Generator pkcs8 = new PKCS8Generator(key);

        pWrt.writeObject(pkcs8);

        pWrt.close();

        PEMReader pRd = new PEMReader(new InputStreamReader(new ByteArrayInputStream(bOut.toByteArray())), new PasswordFinder()
        {
            public char[] getPassword()
            {
                return "hello".toCharArray();
            }
        });

        PrivateKey rdKey = (PrivateKey)pRd.readObject();

        assertEquals(key, rdKey);
    }

    public static void main (String[] args)
    {
        Security.addProvider(new BouncyCastleProvider());
        
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite("OpenSSL Tests");
        
        suite.addTestSuite(AllTests.class);
        
        return suite;
    }
}

package org.sandrob.bouncycastle.jce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;

import javax.crypto.spec.IvParameterSpec;

import org.sandrob.bouncycastle.crypto.CipherKeyGenerator;
import org.sandrob.bouncycastle.crypto.engines.NoekeonEngine;
import org.sandrob.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sandrob.bouncycastle.jce.provider.JCEBlockCipher;
import org.sandrob.bouncycastle.jce.provider.JCEKeyGenerator;
import org.sandrob.bouncycastle.jce.provider.JDKAlgorithmParameterGenerator;
import org.sandrob.bouncycastle.jce.provider.JDKAlgorithmParameters;

public final class Noekeon
{
    private Noekeon()
    {
    }

    public static class ECB
        extends JCEBlockCipher
    {
        public ECB()
        {
            super(new NoekeonEngine());
        }
    }

    public static class KeyGen
        extends JCEKeyGenerator
    {
        public KeyGen()
        {
            super("Noekeon", 128, new CipherKeyGenerator());
        }
    }

    public static class AlgParamGen
        extends JDKAlgorithmParameterGenerator
    {
        protected void engineInit(
            AlgorithmParameterSpec genParamSpec,
            SecureRandom random)
            throws InvalidAlgorithmParameterException
        {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for Noekeon parameter generation.");
        }

        protected AlgorithmParameters engineGenerateParameters()
        {
            byte[] iv = new byte[16];

            if (random == null)
            {
                random = new SecureRandom();
            }

            random.nextBytes(iv);

            AlgorithmParameters params;

            try
            {
                params = AlgorithmParameters.getInstance("Noekeon", BouncyCastleProvider.PROVIDER_NAME);
                params.init(new IvParameterSpec(iv));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.getMessage());
            }

            return params;
        }
    }

    public static class AlgParams
        extends JDKAlgorithmParameters.IVAlgorithmParameters
    {
        protected String engineToString()
        {
            return "Noekeon IV";
        }
    }

    public static class Mappings
        extends HashMap
    {
        public Mappings()
        {
            put("AlgorithmParameters.NOEKEON", "org.sandrob.bouncycastle.jce.provider.symmetric.Noekeon$AlgParams");

            put("AlgorithmParameterGenerator.NOEKEON", "org.sandrob.bouncycastle.jce.provider.symmetric.Noekeon$AlgParamGen");

            put("Cipher.NOEKEON", "org.sandrob.bouncycastle.jce.provider.symmetric.Noekeon$ECB");

            put("KeyGenerator.NOEKEON", "org.sandrob.bouncycastle.jce.provider.symmetric.Noekeon$KeyGen");
        }
    }
}

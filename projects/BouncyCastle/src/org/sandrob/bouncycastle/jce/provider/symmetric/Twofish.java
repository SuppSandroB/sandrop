package org.sandrob.bouncycastle.jce.provider.symmetric;

import java.util.HashMap;

import org.sandrob.bouncycastle.crypto.CipherKeyGenerator;
import org.sandrob.bouncycastle.crypto.engines.TwofishEngine;
import org.sandrob.bouncycastle.jce.provider.JCEBlockCipher;
import org.sandrob.bouncycastle.jce.provider.JCEKeyGenerator;
import org.sandrob.bouncycastle.jce.provider.JDKAlgorithmParameters;

public final class Twofish
{
    private Twofish()
    {
    }
    
    public static class ECB
        extends JCEBlockCipher
    {
        public ECB()
        {
            super(new TwofishEngine());
        }
    }

    public static class KeyGen
        extends JCEKeyGenerator
    {
        public KeyGen()
        {
            super("Twofish", 256, new CipherKeyGenerator());
        }
    }

    public static class AlgParams
        extends JDKAlgorithmParameters.IVAlgorithmParameters
    {
        protected String engineToString()
        {
            return "Twofish IV";
        }
    }

    public static class Mappings
        extends HashMap
    {
        public Mappings()
        {
            put("Cipher.Twofish", "org.sandrob.bouncycastle.jce.provider.symmetric.Twofish$ECB");
            put("KeyGenerator.Twofish", "org.sandrob.bouncycastle.jce.provider.symmetric.Twofish$KeyGen");
            put("AlgorithmParameters.Twofish", "org.sandrob.bouncycastle.jce.provider.symmetric.Twofish$AlgParams");
        }
    }
}

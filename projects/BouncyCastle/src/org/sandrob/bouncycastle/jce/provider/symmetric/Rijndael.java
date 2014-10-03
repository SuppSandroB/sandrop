package org.sandrob.bouncycastle.jce.provider.symmetric;

import java.util.HashMap;

import org.sandrob.bouncycastle.crypto.CipherKeyGenerator;
import org.sandrob.bouncycastle.crypto.engines.RijndaelEngine;
import org.sandrob.bouncycastle.jce.provider.JCEBlockCipher;
import org.sandrob.bouncycastle.jce.provider.JCEKeyGenerator;
import org.sandrob.bouncycastle.jce.provider.JDKAlgorithmParameters;

public final class Rijndael
{
    private Rijndael()
    {
    }
    
    public static class ECB
        extends JCEBlockCipher
    {
        public ECB()
        {
            super(new RijndaelEngine());
        }
    }

    public static class KeyGen
        extends JCEKeyGenerator
    {
        public KeyGen()
        {
            super("Rijndael", 192, new CipherKeyGenerator());
        }
    }

    public static class AlgParams
        extends JDKAlgorithmParameters.IVAlgorithmParameters
    {
        protected String engineToString()
        {
            return "Rijndael IV";
        }
    }

    public static class Mappings
        extends HashMap
    {
        public Mappings()
        {
            put("Cipher.RIJNDAEL", "org.sandrob.bouncycastle.jce.provider.symmetric.Rijndael$ECB");
            put("KeyGenerator.RIJNDAEL", "org.sandrob.bouncycastle.jce.provider.symmetric.Rijndael$KeyGen");
            put("AlgorithmParameters.RIJNDAEL", "org.sandrob.bouncycastle.jce.provider.symmetric.Rijndael$AlgParams");
        }
    }
}

package org.sandrob.bouncycastle.jce.provider.symmetric;

import java.util.HashMap;

import org.sandrob.bouncycastle.crypto.CipherKeyGenerator;
import org.sandrob.bouncycastle.crypto.engines.CAST6Engine;
import org.sandrob.bouncycastle.jce.provider.JCEBlockCipher;
import org.sandrob.bouncycastle.jce.provider.JCEKeyGenerator;

public final class CAST6
{
    private CAST6()
    {
    }
    
    public static class ECB
        extends JCEBlockCipher
    {
        public ECB()
        {
            super(new CAST6Engine());
        }
    }

    public static class KeyGen
        extends JCEKeyGenerator
    {
        public KeyGen()
        {
            super("CAST6", 256, new CipherKeyGenerator());
        }
    }

    public static class Mappings
        extends HashMap
    {
        public Mappings()
        {
            put("Cipher.CAST6", "org.sandrob.bouncycastle.jce.provider.symmetric.CAST6$ECB");
            put("KeyGenerator.CAST6", "org.sandrob.bouncycastle.jce.provider.symmetric.CAST6$KeyGen");
        }
    }
}

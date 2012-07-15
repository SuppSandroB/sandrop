package org.sandrob.bouncycastle.jce.provider.symmetric;

import java.util.HashMap;

import org.sandrob.bouncycastle.crypto.CipherKeyGenerator;
import org.sandrob.bouncycastle.crypto.engines.Grainv1Engine;
import org.sandrob.bouncycastle.jce.provider.JCEKeyGenerator;
import org.sandrob.bouncycastle.jce.provider.JCEStreamCipher;

public final class Grainv1
{
    private Grainv1()
    {
    }
    
    public static class Base
        extends JCEStreamCipher
    {
        public Base()
        {
            super(new Grainv1Engine(), 8);
        }
    }

    public static class KeyGen
        extends JCEKeyGenerator
    {
        public KeyGen()
        {
            super("Grainv1", 80, new CipherKeyGenerator());
        }
    }

    public static class Mappings
        extends HashMap
    {
        public Mappings()
        {
            put("Cipher.Grainv1", "org.bouncycastle.jce.provider.symmetric.Grainv1$Base");
            put("KeyGenerator.Grainv1", "org.bouncycastle.jce.provider.symmetric.Grainv1$KeyGen");
        }
    }
}

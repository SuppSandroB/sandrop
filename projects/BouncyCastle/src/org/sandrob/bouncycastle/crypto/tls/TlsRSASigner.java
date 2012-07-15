package org.sandrob.bouncycastle.crypto.tls;

import java.security.SecureRandom;

import org.sandrob.bouncycastle.crypto.CryptoException;
import org.sandrob.bouncycastle.crypto.Signer;
import org.sandrob.bouncycastle.crypto.digests.NullDigest;
import org.sandrob.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.sandrob.bouncycastle.crypto.engines.RSABlindedEngine;
import org.sandrob.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.sandrob.bouncycastle.crypto.params.ParametersWithRandom;
import org.sandrob.bouncycastle.crypto.params.RSAKeyParameters;
import org.sandrob.bouncycastle.crypto.signers.GenericSigner;

class TlsRSASigner implements TlsSigner
{
    public byte[] calculateRawSignature(SecureRandom random, AsymmetricKeyParameter privateKey, byte[] md5andsha1)
        throws CryptoException
    {
        Signer sig = new GenericSigner(new PKCS1Encoding(new RSABlindedEngine()), new NullDigest());
        sig.init(true, new ParametersWithRandom(privateKey, random));
        sig.update(md5andsha1, 0, md5andsha1.length);
        return sig.generateSignature();
    }

    public Signer createVerifyer(AsymmetricKeyParameter publicKey)
    {
        Signer s = new GenericSigner(new PKCS1Encoding(new RSABlindedEngine()), new CombinedHash());
        s.init(false, publicKey);
        return s;
    }

    public boolean isValidPublicKey(AsymmetricKeyParameter publicKey)
    {
        return publicKey instanceof RSAKeyParameters && !publicKey.isPrivate();
    }
}

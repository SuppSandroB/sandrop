package org.sandrob.bouncycastle.crypto.tls;

import java.security.SecureRandom;

import org.sandrob.bouncycastle.crypto.CryptoException;
import org.sandrob.bouncycastle.crypto.Signer;
import org.sandrob.bouncycastle.crypto.params.AsymmetricKeyParameter;

interface TlsSigner
{
    byte[] calculateRawSignature(SecureRandom random, AsymmetricKeyParameter privateKey, byte[] md5andsha1)
        throws CryptoException;

    Signer createVerifyer(AsymmetricKeyParameter publicKey);

    boolean isValidPublicKey(AsymmetricKeyParameter publicKey);
}

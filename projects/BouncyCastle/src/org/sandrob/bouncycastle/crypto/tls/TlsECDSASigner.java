package org.sandrob.bouncycastle.crypto.tls;

import org.sandrob.bouncycastle.crypto.DSA;
import org.sandrob.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.sandrob.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.sandrob.bouncycastle.crypto.signers.ECDSASigner;

class TlsECDSASigner extends TlsDSASigner
{
    public boolean isValidPublicKey(AsymmetricKeyParameter publicKey)
    {
        return publicKey instanceof ECPublicKeyParameters;
    }

    protected DSA createDSAImpl()
    {
        return new ECDSASigner();
    }
}

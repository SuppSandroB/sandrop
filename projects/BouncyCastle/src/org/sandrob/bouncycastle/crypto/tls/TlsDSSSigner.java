package org.sandrob.bouncycastle.crypto.tls;

import org.sandrob.bouncycastle.crypto.DSA;
import org.sandrob.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.sandrob.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.sandrob.bouncycastle.crypto.signers.DSASigner;

class TlsDSSSigner extends TlsDSASigner
{
    public boolean isValidPublicKey(AsymmetricKeyParameter publicKey)
    {
        return publicKey instanceof DSAPublicKeyParameters;
    }

    protected DSA createDSAImpl()
    {
        return new DSASigner();
    }
}

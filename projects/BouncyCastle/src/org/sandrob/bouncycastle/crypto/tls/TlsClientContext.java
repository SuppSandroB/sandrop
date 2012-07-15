package org.sandrob.bouncycastle.crypto.tls;

import java.security.SecureRandom;

public interface TlsClientContext
{
    SecureRandom getSecureRandom();

    SecurityParameters getSecurityParameters();

    Object getUserObject();

    void setUserObject(Object userObject);
}

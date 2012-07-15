package org.sandrob.bouncycastle.crypto.generators;

import java.math.BigInteger;

import org.sandrob.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.sandrob.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.sandrob.bouncycastle.crypto.KeyGenerationParameters;
import org.sandrob.bouncycastle.crypto.params.DHParameters;
import org.sandrob.bouncycastle.crypto.params.ElGamalKeyGenerationParameters;
import org.sandrob.bouncycastle.crypto.params.ElGamalParameters;
import org.sandrob.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
import org.sandrob.bouncycastle.crypto.params.ElGamalPublicKeyParameters;

/**
 * a ElGamal key pair generator.
 * <p>
 * This generates keys consistent for use with ElGamal as described in
 * page 164 of "Handbook of Applied Cryptography".
 */
public class ElGamalKeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator
{
    private ElGamalKeyGenerationParameters param;

    public void init(
        KeyGenerationParameters param)
    {
        this.param = (ElGamalKeyGenerationParameters)param;
    }

    public AsymmetricCipherKeyPair generateKeyPair()
    {
        DHKeyGeneratorHelper helper = DHKeyGeneratorHelper.INSTANCE;
        ElGamalParameters egp = param.getParameters();
        DHParameters dhp = new DHParameters(egp.getP(), egp.getG(), null, egp.getL());  

        BigInteger x = helper.calculatePrivate(dhp, param.getRandom()); 
        BigInteger y = helper.calculatePublic(dhp, x);

        return new AsymmetricCipherKeyPair(
            new ElGamalPublicKeyParameters(y, egp),
            new ElGamalPrivateKeyParameters(x, egp));
    }
}

package cz.alis.dss.compatility;

import java.security.GeneralSecurityException;
import java.security.Signature;

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.SignatureAlgorithm;
import eu.europa.esig.dss.SignatureValue;
import eu.europa.esig.dss.ToBeSigned;

/**
 * Třída doplněna pro zpětnou kompatibilitu s verzí DSS 5.5, kde tato třída byla odebrána v commitu: 6c40cabb39606384707c0e5fe6e43512b6b88db7
 * 
 * @author coufal
 *
 */
public final class TestUtils {

	private TestUtils(){
	}

	public static SignatureValue sign(final SignatureAlgorithm signatureAlgorithm, final MockPrivateKeyEntry privateKey, ToBeSigned bytes) {
		try {
			final Signature signature = Signature.getInstance(signatureAlgorithm.getJCEId());
			signature.initSign(privateKey.getPrivateKey());
			signature.update(bytes.getBytes());
			final byte[] signatureValue = signature.sign();
			return new SignatureValue(signatureAlgorithm, signatureValue);
		} catch (GeneralSecurityException e) {
			throw new DSSException(e);
		}
	}

}

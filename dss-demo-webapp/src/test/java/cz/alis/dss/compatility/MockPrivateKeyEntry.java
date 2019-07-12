package cz.alis.dss.compatility;

import java.security.PrivateKey;

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.x509.CertificateToken;

/**
 * Třída doplněna pro zpětnou kompatibilitu s verzí DSS 5.5, kde tato třída byla odebrána v commitu: 6c40cabb39606384707c0e5fe6e43512b6b88db7
 * 
 * @author coufal
 *
 */
public class MockPrivateKeyEntry implements DSSPrivateKeyEntry {

	private final EncryptionAlgorithm encryptionAlgo;
	private final CertificateToken certificate;
	private final CertificateToken[] certificateChain;
	private final PrivateKey privateKey;

	public MockPrivateKeyEntry(EncryptionAlgorithm encryptionAlgo, CertificateToken certificate, PrivateKey privateKey) {
		this.encryptionAlgo = encryptionAlgo;
		this.certificate = certificate;
		this.privateKey = privateKey;
		this.certificateChain = null;
	}

	public MockPrivateKeyEntry(EncryptionAlgorithm encryptionAlgo, CertificateToken certificate, CertificateToken[] certificateChain,
			PrivateKey privateKey) {
		this.encryptionAlgo = encryptionAlgo;
		this.certificate = certificate;
		this.certificateChain = certificateChain;
		this.privateKey = privateKey;
	}

	@Override
	public CertificateToken getCertificate() {
		return certificate;
	}

	@Override
	public CertificateToken[] getCertificateChain() {
		return certificateChain;
	}

	@Override
	public EncryptionAlgorithm getEncryptionAlgorithm() throws DSSException {
		return encryptionAlgo;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

}

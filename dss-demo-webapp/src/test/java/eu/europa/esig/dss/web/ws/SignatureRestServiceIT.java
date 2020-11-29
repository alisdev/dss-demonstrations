package eu.europa.esig.dss.web.ws;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore.PasswordProtection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.junit.Before;
import org.junit.Test;

import eu.europa.esig.dss.asic.cades.ASiCWithCAdESContainerExtractor;
import eu.europa.esig.dss.asic.cades.validation.ASiCEWithCAdESManifestParser;
import eu.europa.esig.dss.asic.cades.validation.ASiCEWithCAdESManifestValidator;
import eu.europa.esig.dss.asic.common.ASiCExtractResult;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.enumerations.TimestampContainerForm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.ManifestEntry;
import eu.europa.esig.dss.validation.ManifestFile;
import eu.europa.esig.dss.web.config.CXFConfig;
import eu.europa.esig.dss.ws.converter.DTOConverter;
import eu.europa.esig.dss.ws.converter.RemoteDocumentConverter;
import eu.europa.esig.dss.ws.dto.RemoteCertificate;
import eu.europa.esig.dss.ws.dto.RemoteDocument;
import eu.europa.esig.dss.ws.dto.SignatureValueDTO;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;
import eu.europa.esig.dss.ws.signature.dto.DataToSignMultipleDocumentsDTO;
import eu.europa.esig.dss.ws.signature.dto.DataToSignOneDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.ExtendDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.SignMultipleDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.SignOneDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.TimestampMultipleDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.TimestampOneDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteSignatureParameters;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteTimestampParameters;
import eu.europa.esig.dss.ws.signature.rest.client.RestDocumentSignatureService;
import eu.europa.esig.dss.ws.signature.rest.client.RestMultipleDocumentSignatureService;

public class SignatureRestServiceIT extends AbstractRestIT {

	private RestDocumentSignatureService restClient;
	private RestMultipleDocumentSignatureService restMultiDocsClient;

	@Before
	public void init() {
		JAXRSClientFactoryBean factory = new JAXRSClientFactoryBean();

		factory.setAddress(getBaseCxf() + CXFConfig.REST_SIGNATURE_ONE_DOCUMENT);
		factory.setServiceClass(RestDocumentSignatureService.class);
		factory.setProviders(Arrays.asList(jacksonJsonProvider()));

		LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
		factory.getInInterceptors().add(loggingInInterceptor);
		factory.getInFaultInterceptors().add(loggingInInterceptor);

		LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
		factory.getOutInterceptors().add(loggingOutInterceptor);
		factory.getOutFaultInterceptors().add(loggingOutInterceptor);

		restClient = factory.create(RestDocumentSignatureService.class);

		factory = new JAXRSClientFactoryBean();

		factory.setAddress(getBaseCxf() + CXFConfig.REST_SIGNATURE_MULTIPLE_DOCUMENTS);
		factory.setServiceClass(RestMultipleDocumentSignatureService.class);
		factory.setProviders(Arrays.asList(jacksonJsonProvider()));

		factory.getInInterceptors().add(loggingInInterceptor);
		factory.getInFaultInterceptors().add(loggingInInterceptor);

		factory.getOutInterceptors().add(loggingOutInterceptor);
		factory.getOutFaultInterceptors().add(loggingOutInterceptor);

		restMultiDocsClient = factory.create(RestMultipleDocumentSignatureService.class);
	}

	@Test
	public void testSigningAndExtension() throws Exception {
		try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(new FileInputStream("src/test/resources/user_a_rsa.p12"),
				new PasswordProtection("password".toCharArray()))) {

			List<DSSPrivateKeyEntry> keys = token.getKeys();
			DSSPrivateKeyEntry dssPrivateKeyEntry = keys.get(0);

			RemoteSignatureParameters parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
			parameters.setSigningCertificate(new RemoteCertificate(dssPrivateKeyEntry.getCertificate().getCertificate().getEncoded()));
			parameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
			parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);

			FileDocument fileToSign = new FileDocument(new File("src/test/resources/sample.xml"));
			RemoteDocument toSignDocument = new RemoteDocument(Utils.toByteArray(fileToSign.openStream()), fileToSign.getName());
			ToBeSignedDTO dataToSign = restClient.getDataToSign(new DataToSignOneDocumentDTO(toSignDocument, parameters));
			assertNotNull(dataToSign);

			SignatureValue signatureValue = token.sign(DTOConverter.toToBeSigned(dataToSign), DigestAlgorithm.SHA256, dssPrivateKeyEntry);
			SignOneDocumentDTO signDocument = new SignOneDocumentDTO(toSignDocument, parameters,
					new SignatureValueDTO(signatureValue.getAlgorithm(), signatureValue.getValue()));
			RemoteDocument signedDocument = restClient.signDocument(signDocument);

			assertNotNull(signedDocument);

			parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_T);

			RemoteDocument extendedDocument = restClient.extendDocument(new ExtendDocumentDTO(signedDocument, parameters));

			assertNotNull(extendedDocument);

			InMemoryDocument iMD = new InMemoryDocument(extendedDocument.getBytes());
			// iMD.save("target/test.xml");
			assertNotNull(iMD);
		}
	}

	@Test
	public void testSigningAndExtensionDigestDocument() throws Exception {
		try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(new FileInputStream("src/test/resources/user_a_rsa.p12"),
				new PasswordProtection("password".toCharArray()))) {

			List<DSSPrivateKeyEntry> keys = token.getKeys();
			DSSPrivateKeyEntry dssPrivateKeyEntry = keys.get(0);

			RemoteSignatureParameters parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
			parameters.setSigningCertificate(new RemoteCertificate(dssPrivateKeyEntry.getCertificate().getCertificate().getEncoded()));
			parameters.setSignaturePackaging(SignaturePackaging.DETACHED);
			parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);

			FileDocument fileToSign = new FileDocument(new File("src/test/resources/sample.xml"));
			RemoteDocument toSignDocument = new RemoteDocument(DSSUtils.digest(DigestAlgorithm.SHA256, fileToSign), DigestAlgorithm.SHA256,
					fileToSign.getName());

			ToBeSignedDTO dataToSign = restClient.getDataToSign(new DataToSignOneDocumentDTO(toSignDocument, parameters));
			assertNotNull(dataToSign);

			SignatureValue signatureValue = token.sign(DTOConverter.toToBeSigned(dataToSign), DigestAlgorithm.SHA256, dssPrivateKeyEntry);
			SignOneDocumentDTO signDocument = new SignOneDocumentDTO(toSignDocument, parameters,
					new SignatureValueDTO(signatureValue.getAlgorithm(), signatureValue.getValue()));
			RemoteDocument signedDocument = restClient.signDocument(signDocument);

			assertNotNull(signedDocument);

			parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_T);
			parameters.setDetachedContents(Arrays.asList(toSignDocument));

			RemoteDocument extendedDocument = restClient.extendDocument(new ExtendDocumentDTO(signedDocument, parameters));

			assertNotNull(extendedDocument);

			InMemoryDocument iMD = new InMemoryDocument(extendedDocument.getBytes());
			// iMD.save("target/test-digest.xml");
			assertNotNull(iMD);
		}
	}

	@Test
	public void testSigningAndExtensionMultiDocuments() throws Exception {
		try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(new FileInputStream("src/test/resources/user_a_rsa.p12"),
				new PasswordProtection("password".toCharArray()))) {

			List<DSSPrivateKeyEntry> keys = token.getKeys();
			DSSPrivateKeyEntry dssPrivateKeyEntry = keys.get(0);

			RemoteSignatureParameters parameters = new RemoteSignatureParameters();
			parameters.setAsicContainerType(ASiCContainerType.ASiC_E);
			parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
			parameters.setSigningCertificate(new RemoteCertificate(dssPrivateKeyEntry.getCertificate().getCertificate().getEncoded()));
			parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);

			FileDocument fileToSign = new FileDocument(new File("src/test/resources/sample.xml"));
			RemoteDocument toSignDocument = new RemoteDocument(DSSUtils.toByteArray(fileToSign), fileToSign.getName());
			RemoteDocument toSignDoc2 = new RemoteDocument("Hello world!".getBytes("UTF-8"), "test.bin");
			List<RemoteDocument> toSignDocuments = new ArrayList<RemoteDocument>();
			toSignDocuments.add(toSignDocument);
			toSignDocuments.add(toSignDoc2);
			ToBeSignedDTO dataToSign = restMultiDocsClient.getDataToSign(new DataToSignMultipleDocumentsDTO(toSignDocuments, parameters));
			assertNotNull(dataToSign);

			SignatureValue signatureValue = token.sign(DTOConverter.toToBeSigned(dataToSign), DigestAlgorithm.SHA256, dssPrivateKeyEntry);
			SignMultipleDocumentDTO signDocument = new SignMultipleDocumentDTO(toSignDocuments, parameters,
					new SignatureValueDTO(signatureValue.getAlgorithm(), signatureValue.getValue()));
			RemoteDocument signedDocument = restMultiDocsClient.signDocument(signDocument);

			assertNotNull(signedDocument);

			parameters = new RemoteSignatureParameters();
			parameters.setAsicContainerType(ASiCContainerType.ASiC_E);
			parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_T);

			RemoteDocument extendedDocument = restMultiDocsClient.extendDocument(new ExtendDocumentDTO(signedDocument, parameters));

			assertNotNull(extendedDocument);

			InMemoryDocument iMD = new InMemoryDocument(extendedDocument.getBytes());
			// iMD.save("target/test.asice");
			assertNotNull(iMD);
		}
	}
	
	@Test
	public void testTimestamping() throws Exception {
		RemoteTimestampParameters timestampParameters = new RemoteTimestampParameters(TimestampContainerForm.PDF, DigestAlgorithm.SHA512);
		
		FileDocument fileToTimestamp = new FileDocument(new File("src/test/resources/sample.pdf"));
		RemoteDocument remoteDocument = RemoteDocumentConverter.toRemoteDocument(fileToTimestamp);
		
		TimestampOneDocumentDTO timestampOneDocumentDTO = new TimestampOneDocumentDTO(remoteDocument, timestampParameters);
		RemoteDocument timestampedDocument = restClient.timestampDocument(timestampOneDocumentDTO);

		assertNotNull(timestampedDocument);

		InMemoryDocument iMD = new InMemoryDocument(timestampedDocument.getBytes());
		// iMD.save("target/testSigned.pdf");
		assertNotNull(iMD);
	}
	
	@Test
	public void timestampMultipleDocumentsTest() throws Exception {
		RemoteTimestampParameters timestampParameters = new RemoteTimestampParameters(TimestampContainerForm.ASiC_E, DigestAlgorithm.SHA512);
		
		List<DSSDocument> documentsToSign = new ArrayList<DSSDocument>(Arrays.asList(
				new DSSDocument[] {new FileDocument(new File("src/test/resources/sample.xml")), new FileDocument(new File("src/test/resources/sample.pdf"))}));
		
		List<RemoteDocument> remoteDocuments = RemoteDocumentConverter.toRemoteDocuments(documentsToSign);
		
		TimestampMultipleDocumentDTO timestampMultipleDocumentDTO = new TimestampMultipleDocumentDTO(remoteDocuments, timestampParameters);
		RemoteDocument timestampedDocument = restMultiDocsClient.timestampDocuments(timestampMultipleDocumentDTO);
		
		assertNotNull(timestampedDocument);

		InMemoryDocument iMD = new InMemoryDocument(timestampedDocument.getBytes());
		// iMD.save("target/testSigned.asice");
		assertNotNull(iMD);
		
		ASiCWithCAdESContainerExtractor extractor = new ASiCWithCAdESContainerExtractor(iMD);
		ASiCExtractResult extractedResult = extractor.extract();
		
		assertEquals(1, extractedResult.getTimestampDocuments().size());
		DSSDocument timestamp = extractedResult.getTimestampDocuments().get(0);
		
		DSSDocument timestampManifest = ASiCEWithCAdESManifestParser.getLinkedManifest(extractedResult.getManifestDocuments(), timestamp.getName());
		ManifestFile manifestFile = ASiCEWithCAdESManifestParser.getManifestFile(timestampManifest);
		
		ASiCEWithCAdESManifestValidator manifestValidator = new ASiCEWithCAdESManifestValidator(manifestFile, extractedResult.getSignedDocuments());
		List<ManifestEntry> manifestEntries = manifestValidator.validateEntries();
		
		assertEquals(2, manifestEntries.size());
		
		for (ManifestEntry manifestEntry : manifestEntries) {
			boolean signedDocFound = false;
			for (DSSDocument document : documentsToSign) {
				if (manifestEntry.getFileName().equals(document.getName())) {
					signedDocFound = true;
				}
			}
			assertTrue(signedDocFound);
		}
	}
	
	@Test
	public void timestampASiCSTest() throws Exception {
		RemoteTimestampParameters timestampParameters = new RemoteTimestampParameters(TimestampContainerForm.ASiC_S, DigestAlgorithm.SHA512);
		
		List<DSSDocument> documentsToSign = new ArrayList<DSSDocument>(Arrays.asList(
				new DSSDocument[] {new FileDocument(new File("src/test/resources/sample.xml")), new FileDocument(new File("src/test/resources/sample.pdf"))}));
		
		List<RemoteDocument> remoteDocuments = RemoteDocumentConverter.toRemoteDocuments(documentsToSign);
		
		TimestampMultipleDocumentDTO timestampMultipleDocumentDTO = new TimestampMultipleDocumentDTO(remoteDocuments, timestampParameters);
		RemoteDocument timestampedDocument = restMultiDocsClient.timestampDocuments(timestampMultipleDocumentDTO);
		
		assertNotNull(timestampedDocument);

		InMemoryDocument iMD = new InMemoryDocument(timestampedDocument.getBytes());
		// iMD.save("target/testSigned.asics");
		assertNotNull(iMD);
		
		ASiCWithCAdESContainerExtractor extractor = new ASiCWithCAdESContainerExtractor(iMD);
		ASiCExtractResult extractedResult = extractor.extract();
		
		assertEquals(1, extractedResult.getTimestampDocuments().size());
		
		List<DSSDocument> signedDocuments = extractedResult.getSignedDocuments();
		assertEquals(1, signedDocuments.size()); // ZIP container
		
		List<DSSDocument> containerDocuments = extractedResult.getContainerDocuments();
		assertEquals(2, containerDocuments.size()); // Zip Container content - original docs
		
		for (DSSDocument conteinerContent : containerDocuments) {
			boolean signedDocFound = false;
			for (DSSDocument document : documentsToSign) {
				if (conteinerContent.getName().equals(document.getName())) {
					signedDocFound = true;
				}
			}
			assertTrue(signedDocFound);
		}
	}

}

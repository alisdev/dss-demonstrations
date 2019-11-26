package eu.europa.esig.dss.web.ws;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore.PasswordProtection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.junit.Before;
import org.junit.Test;

import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.web.config.CXFConfig;
import eu.europa.esig.dss.ws.converter.DTOConverter;
import eu.europa.esig.dss.ws.dto.RemoteCertificate;
import eu.europa.esig.dss.ws.dto.RemoteDocument;
import eu.europa.esig.dss.ws.dto.SignatureValueDTO;
import eu.europa.esig.dss.ws.dto.ToBeSignedDTO;
import eu.europa.esig.dss.ws.signature.dto.DataToSignMultipleDocumentsDTO;
import eu.europa.esig.dss.ws.signature.dto.DataToSignOneDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.ExtendDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.SignMultipleDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.SignOneDocumentDTO;
import eu.europa.esig.dss.ws.signature.dto.parameters.RemoteSignatureParameters;
import eu.europa.esig.dss.ws.signature.soap.client.DateAdapter;
import eu.europa.esig.dss.ws.signature.soap.client.SoapDocumentSignatureService;
import eu.europa.esig.dss.ws.signature.soap.client.SoapMultipleDocumentsSignatureService;

public class SignatureSoapServiceIT extends AbstractIT {

	private SoapDocumentSignatureService soapClient;
	private SoapMultipleDocumentsSignatureService soapMultiDocsClient;

	@Before
	public void init() {

		JAXBDataBinding dataBinding = new JAXBDataBinding();
		dataBinding.getConfiguredXmlAdapters().add(new DateAdapter());

		Map<String, Object> props = new HashMap<String, Object>();
		props.put("mtom-enabled", Boolean.TRUE);

		JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
		factory.setServiceClass(SoapDocumentSignatureService.class);
		factory.setProperties(props);
		factory.setDataBinding(dataBinding);
		factory.setAddress(getBaseCxf() + CXFConfig.SOAP_SIGNATURE_ONE_DOCUMENT);

		LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
		factory.getInInterceptors().add(loggingInInterceptor);
		factory.getInFaultInterceptors().add(loggingInInterceptor);

		LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
		factory.getOutInterceptors().add(loggingOutInterceptor);
		factory.getOutFaultInterceptors().add(loggingOutInterceptor);

		soapClient = factory.create(SoapDocumentSignatureService.class);

		dataBinding = new JAXBDataBinding();
		dataBinding.getConfiguredXmlAdapters().add(new DateAdapter());

		factory = new JaxWsProxyFactoryBean();
		factory.setServiceClass(SoapMultipleDocumentsSignatureService.class);
		factory.setProperties(props);
		factory.setDataBinding(dataBinding);
		factory.setAddress(getBaseCxf() + CXFConfig.SOAP_SIGNATURE_MULTIPLE_DOCUMENTS);

		loggingInInterceptor = new LoggingInInterceptor();
		factory.getInInterceptors().add(loggingInInterceptor);
		factory.getInFaultInterceptors().add(loggingInInterceptor);

		loggingOutInterceptor = new LoggingOutInterceptor();
		factory.getOutInterceptors().add(loggingOutInterceptor);
		factory.getOutFaultInterceptors().add(loggingOutInterceptor);

		soapMultiDocsClient = factory.create(SoapMultipleDocumentsSignatureService.class);
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
			ToBeSignedDTO dataToSign = soapClient.getDataToSign(new DataToSignOneDocumentDTO(toSignDocument, parameters));
			assertNotNull(dataToSign);

			SignatureValue signatureValue = token.sign(DTOConverter.toToBeSigned(dataToSign), DigestAlgorithm.SHA256, dssPrivateKeyEntry);
			SignOneDocumentDTO signDocument = new SignOneDocumentDTO(toSignDocument, parameters, 
					new SignatureValueDTO(signatureValue.getAlgorithm(), signatureValue.getValue()));
			RemoteDocument signedDocument = soapClient.signDocument(signDocument);

			assertNotNull(signedDocument);

			parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_T);

			RemoteDocument extendedDocument = soapClient.extendDocument(new ExtendDocumentDTO(signedDocument, parameters));

			assertNotNull(extendedDocument);

			InMemoryDocument iMD = new InMemoryDocument(extendedDocument.getBytes());
			iMD.save("target/test.xml");
		}
	}

	@Test
	public void testSigningAndExtensionDigestDocument() throws Exception {
		try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(new FileInputStream("src/test/resources/user_a_rsa.p12"),
				new PasswordProtection("password".toCharArray()))) {

			List<DSSPrivateKeyEntry> keys = token.getKeys();
			DSSPrivateKeyEntry dssPrivateKeyEntry = keys.get(0);

			RemoteSignatureParameters parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);
			parameters.setSigningCertificate(new RemoteCertificate(dssPrivateKeyEntry.getCertificate().getCertificate().getEncoded()));
			parameters.setSignaturePackaging(SignaturePackaging.DETACHED);
			parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);

			FileDocument fileToSign = new FileDocument(new File("src/test/resources/dss-test.properties"));
			RemoteDocument toSignDocument = new RemoteDocument(DSSUtils.digest(DigestAlgorithm.SHA256, fileToSign), DigestAlgorithm.SHA256,
					fileToSign.getName());

			ToBeSignedDTO dataToSign = soapClient.getDataToSign(new DataToSignOneDocumentDTO(toSignDocument, parameters));
			assertNotNull(dataToSign);

			SignatureValue signatureValue = token.sign(DTOConverter.toToBeSigned(dataToSign), DigestAlgorithm.SHA256, dssPrivateKeyEntry);
			SignOneDocumentDTO signDocument = new SignOneDocumentDTO(toSignDocument, parameters, 
					new SignatureValueDTO(signatureValue.getAlgorithm(), signatureValue.getValue()));
			RemoteDocument signedDocument = soapClient.signDocument(signDocument);

			assertNotNull(signedDocument);

			parameters = new RemoteSignatureParameters();
			parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_T);
			parameters.setDetachedContents(Arrays.asList(toSignDocument));

			RemoteDocument extendedDocument = soapClient.extendDocument(new ExtendDocumentDTO(signedDocument, parameters));

			assertNotNull(extendedDocument);

			InMemoryDocument iMD = new InMemoryDocument(extendedDocument.getBytes());
			iMD.save("target/test-digest.xml");
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
			ToBeSignedDTO dataToSign = soapMultiDocsClient.getDataToSign(new DataToSignMultipleDocumentsDTO(toSignDocuments, parameters));
			assertNotNull(dataToSign);

			SignatureValue signatureValue = token.sign(DTOConverter.toToBeSigned(dataToSign), DigestAlgorithm.SHA256, dssPrivateKeyEntry);
			SignMultipleDocumentDTO signDocument = new SignMultipleDocumentDTO(toSignDocuments, parameters, 
					new SignatureValueDTO(signatureValue.getAlgorithm(), signatureValue.getValue()));
			RemoteDocument signedDocument = soapMultiDocsClient.signDocument(signDocument);

			assertNotNull(signedDocument);

			parameters = new RemoteSignatureParameters();
			parameters.setAsicContainerType(ASiCContainerType.ASiC_E);
			parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_T);

			RemoteDocument extendedDocument = soapMultiDocsClient.extendDocument(new ExtendDocumentDTO(signedDocument, parameters));

			assertNotNull(extendedDocument);

			InMemoryDocument iMD = new InMemoryDocument(extendedDocument.getBytes());
			iMD.save("target/test.asice");
		}
	}

}

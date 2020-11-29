package eu.europa.esig.dss.x509.tsp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.TimestampBinary;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;

public class AlisTSPSource implements TSPSource {

	private static final long serialVersionUID = 8405523139626486211L;

	private final Logger LOG = LoggerFactory.getLogger(AlisTSPSource.class);

	@Override
	public TimestampBinary getTimeStampResponse(DigestAlgorithm digestAlgorithm, byte[] digest) {
		try {
			TimeStampRequestGenerator requestGenerator = new TimeStampRequestGenerator();
			requestGenerator.setCertReq(true);
			TimeStampRequest request = requestGenerator.generate(new ASN1ObjectIdentifier(digestAlgorithm.getOid()),
					digest);

			URL url = new URL("https://www.postsignum.eu/TSS/TSS_user/"); // doplnit
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/timestamp-query");

			String authorization = "Basic "
					+ (new String(Base64.encode(("hlavackova@alis.cz" + ":" + "ZuzkaAja77").getBytes()))).toString(); // doplnit
																											// jemeno,
																											// heslo
			connection.setRequestProperty("Authorization", authorization);
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { new X509TrustManager() {

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}
			} }, null);

			SSLSocketFactory fact = sslContext.getSocketFactory();
			HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
			httpsConnection.setSSLSocketFactory(fact);
			httpsConnection.setHostnameVerifier(new HostnameVerifier() {

				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});

			OutputStream os = connection.getOutputStream();
			os.write(request.getEncoded());
			InputStream inputStream = connection.getInputStream();
			TimeStampResponse response = new TimeStampResponse(inputStream);
			response.validate(request);
			return new TimestampBinary(response.getTimeStampToken().getEncoded());
		} catch (Exception e) {
			throw new DSSException("Unable to generate a timestamp from the Mock", e);
		}
	}

}

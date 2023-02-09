package com.czertainly.ca.connector.ejbca.util;

import com.czertainly.api.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class CertificateUtil {

	private static final Logger logger = LoggerFactory.getLogger(CertificateUtil.class);

	private CertificateUtil() {
	}

	public static X509Certificate getX509Certificate(byte[] certInBytes) throws CertificateException {
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certInBytes));
		} catch (Exception e) {
			throw new CertificateException("Error when parsing certificate", e);
		}
	}

	public static String getDnFromX509Certificate(String certInBase64) throws CertificateException, NotFoundException {
		return getDnFromX509Certificate(getX509Certificate(certInBase64));
	}

	public static String getDnFromX509Certificate(X509Certificate cert) throws NotFoundException {
		Principal subjectDN = cert.getSubjectDN();
		if (subjectDN != null) {
			return subjectDN.getName();
		} else {
			throw new NotFoundException("Subject DN not found in certificate.");
		}
	}

	public static String getIssuerDnFromX509Certificate(X509Certificate cert) throws NotFoundException {
		Principal issuerDN = cert.getIssuerDN();
		if (issuerDN != null) {
			return issuerDN.getName();
		} else {
			throw new NotFoundException("Issuer DN not found in certificate.");
		}
	}

	public static String getSerialNumberFromX509Certificate(X509Certificate certificate) {
		return certificate.getSerialNumber().toString(16);
	}

	public static X509Certificate getX509Certificate(String certInBase64) throws CertificateException {
		return getX509Certificate(Base64.getDecoder().decode(certInBase64));
	}

	public static X509Certificate parseCertificate(String cert) throws CertificateException {
		cert = cert.replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "")
				.replace("\r", "").replace("\n", "");
		byte[] decoded = Base64.getDecoder().decode(cert);
		try {
			return (X509Certificate) CertificateFactory.getInstance("X.509")
					.generateCertificate(new ByteArrayInputStream(decoded));
		}catch (Exception e){
			return (X509Certificate) CertificateFactory.getInstance("X.509")
					.generateCertificates(new ByteArrayInputStream(decoded)).iterator().next();
		}
	}
}

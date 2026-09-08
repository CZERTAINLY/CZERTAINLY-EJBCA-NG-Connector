package com.otilm.ca.connector.ejbca.util;

import java.io.IOException;

import com.otilm.api.model.core.enums.CertificateRequestFormat;
import com.otilm.ca.connector.ejbca.request.CertificateRequest;
import com.otilm.ca.connector.ejbca.request.CrmfCertificateRequest;
import com.otilm.ca.connector.ejbca.request.Pkcs10CertificateRequest;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.StringJoiner;

public class CertificateRequestUtils {

    private static final Logger logger = LoggerFactory.getLogger(CertificateRequestUtils.class);

    private static final String SAN_SEPARATOR = ", ";
    private static final ASN1ObjectIdentifier OID_MS_UPN = new ASN1ObjectIdentifier("1.3.6.1.4.1.311.20.2.3");
    private static final ASN1ObjectIdentifier OID_XMPP_ADDR = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.8.5");
    private static final ASN1ObjectIdentifier OID_SRV_NAME = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.8.7");

    private CertificateRequestUtils() {
        // utility class
    }

    public static JcaPKCS10CertificationRequest csrStringToJcaObject(String csr) throws IOException {
        csr = csr.replace("-----BEGIN CERTIFICATE REQUEST-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END CERTIFICATE REQUEST-----", "");
        byte[] decoded = Base64.getDecoder().decode(csr);
        return new JcaPKCS10CertificationRequest(decoded);
    }

    public static List<String> extractSanFromCsr(JcaPKCS10CertificationRequest csr) {
        List<String> sans = new ArrayList<>();
        Attribute[] certAttributes = csr.getAttributes();
        for (Attribute attribute : certAttributes) {
            if (attribute.getAttrType().equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
                Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
                GeneralNames gns = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
                GeneralName[] names = gns.getNames();
                for (GeneralName name : names) {
                    logger.info("Type: " + name.getTagNo() + " | Name: " + name.getName());
                    String title = "";
                    if (name.getTagNo() == GeneralName.dNSName) {
                        title = "DNS";
                    } else if (name.getTagNo() == GeneralName.iPAddress) {
                        title = "IP Address";
                        // name.toASN1Primitive();
                    } else if (name.getTagNo() == GeneralName.otherName) {
                        title = "Other Name";
                    }
                    sans.add(title + ": " + name.getName());
                }
            }
        }
        return sans;
    }

    public static CertificateRequest createCertificateRequest(byte[] csr, CertificateRequestFormat format) {
        return switch (format) {
            case PKCS10 -> new Pkcs10CertificateRequest(csr);
            case CRMF -> new CrmfCertificateRequest(csr);
            default -> throw new IllegalArgumentException("Unsupported certificate request format: " + format);
        };
    }

    public static String getEjbcaSanExtension(CertificateRequest certificateRequest) throws IOException {
        if (certificateRequest == null || certificateRequest.getFormat() != CertificateRequestFormat.PKCS10) {
            return null;
        }
        final PKCS10CertificationRequest pkcs10CertificateRequest = new PKCS10CertificationRequest(certificateRequest.getEncoded());
        final GeneralNames generalNames = subjectAlternativeNames(pkcs10CertificateRequest);
        if (generalNames == null) {
            return null;
        }
        final List<String> parts = new ArrayList<>();
        for (GeneralName generalName : generalNames.getNames()) {
            final String part = toEjbcaSanEntry(generalName);
            if (part != null) {
                parts.add(part);
            } else {
                logger.warn("Skipping subject alternative name of unsupported type {}", generalName.getTagNo());
            }
        }
        return parts.isEmpty() ? null : String.join(SAN_SEPARATOR, parts);
    }

    private static GeneralNames subjectAlternativeNames(PKCS10CertificationRequest csr) {
        for (Attribute attribute : csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
            if (attribute.getAttrValues().size() == 0) {
                continue;
            }
            final Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
            final GeneralNames generalNames = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
            if (generalNames != null) {
                return generalNames;
            }
        }
        return null;
    }

    private static String toEjbcaSanEntry(GeneralName generalName) {
        return switch (generalName.getTagNo()) {
            case GeneralName.dNSName -> "dNSName=" + generalName.getName();
            case GeneralName.rfc822Name -> "rfc822name=" + generalName.getName();
            case GeneralName.uniformResourceIdentifier -> "UNIFORMRESOURCEIDENTIFIER=" + generalName.getName();
            case GeneralName.registeredID -> "registeredID=" + generalName.getName();
            case GeneralName.iPAddress -> "iPAddress=" + ipAddress(generalName);
            case GeneralName.directoryName -> "DIRECTORYNAME=" + generalName.getName().toString().replace(",", "\\,");
            case GeneralName.otherName -> otherName(generalName);
            default -> null;
        };
    }

    private static String ipAddress(GeneralName generalName) {
        final byte[] octets = DEROctetString.getInstance(generalName.getName()).getOctets();
        try {
            final InetAddress address = InetAddress.getByAddress(octets);
            if (address instanceof Inet4Address) {
                return address.getHostAddress();
            }
            final StringJoiner groups = new StringJoiner(":");
            for (int i = 0; i < octets.length; i += 2) {
                groups.add(Integer.toHexString(((octets[i] & 0xFF) << 8) | (octets[i + 1] & 0xFF)));
            }
            return groups.toString();
        } catch (UnknownHostException e) {
            logger.warn("Subject alternative name contains an IP address of unexpected length {}", octets.length);
            return null;
        }
    }

    private static String otherName(GeneralName generalName) {
        final ASN1Sequence sequence = ASN1Sequence.getInstance(generalName.getName());
        if (sequence.size() < 2) {
            return null;
        }
        final ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(sequence.getObjectAt(0));
        final String key;
        if (OID_MS_UPN.equals(oid)) {
            key = "UPN";
        } else if (OID_XMPP_ADDR.equals(oid)) {
            key = "XMPPADDR";
        } else if (OID_SRV_NAME.equals(oid)) {
            key = "SRVNAME";
        } else {
            return null;
        }
        final ASN1Primitive value = ASN1TaggedObject.getInstance(sequence.getObjectAt(1)).getBaseObject().toASN1Primitive();
        return value instanceof ASN1String asn1String ? key + "=" + asn1String.getString() : null;
    }
}

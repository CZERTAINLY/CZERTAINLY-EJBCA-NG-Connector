package com.czertainly.ca.connector.ejbca.util;

import java.io.IOException;

import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CsrUtil {

    private static final Logger logger = LoggerFactory.getLogger(CsrUtil.class);

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
}

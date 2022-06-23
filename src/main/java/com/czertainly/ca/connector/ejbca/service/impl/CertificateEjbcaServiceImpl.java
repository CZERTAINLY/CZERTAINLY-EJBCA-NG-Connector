package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.common.attribute.content.BaseAttributeContent;
import com.czertainly.api.model.connector.v2.CertRevocationDto;
import com.czertainly.api.model.connector.v2.CertificateDataResponseDto;
import com.czertainly.api.model.connector.v2.CertificateRenewRequestDto;
import com.czertainly.api.model.connector.v2.CertificateSignRequestDto;
import com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl;
import com.czertainly.ca.connector.ejbca.api.CertificateControllerImpl;
import com.czertainly.ca.connector.ejbca.enums.UsernameGenMethod;
import com.czertainly.ca.connector.ejbca.service.CertificateEjbcaService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.util.CertificateUtil;
import com.czertainly.ca.connector.ejbca.util.CsrUtil;
import com.czertainly.ca.connector.ejbca.util.MetaDefinitions;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class CertificateEjbcaServiceImpl implements CertificateEjbcaService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateEjbcaServiceImpl.class);

    private final String COMMON_NAME = "2.5.4.3";
    public final String META_EJBCA_USERNAME = "ejbcaUsername";
    public static final String META_EMAIL = "email";
    public static final String META_SAN = "san";
    public static final String META_EXTENSION = "extension";

    @Autowired
    public void setEjbcaService(EjbcaService ejbcaService) {
        this.ejbcaService = ejbcaService;
    }

    private EjbcaService ejbcaService;

    @Override
    public CertificateDataResponseDto issueCertificate(String uuid, CertificateSignRequestDto request) throws Exception {
        // generate username based on the request
        String usernameGenMethod = (String) AttributeDefinitionUtils.getAttributeContent(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_GEN_METHOD, request.getRaProfileAttributes(), BaseAttributeContent.class).getValue();
        String usernamePrefix = (String) AttributeDefinitionUtils.getAttributeContent(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_PREFIX, request.getRaProfileAttributes(), BaseAttributeContent.class).getValue();
        String usernamePostfix = (String) AttributeDefinitionUtils.getAttributeContent(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_POSTFIX, request.getRaProfileAttributes(), BaseAttributeContent.class).getValue();

        JcaPKCS10CertificationRequest csr = parseCsrToJcaObject(request.getPkcs10());

        String username = generateUsername(usernameGenMethod, usernamePrefix, usernamePostfix, csr);
        String subjectDn = csr.getSubject().toString();
        String password = username;

        // try to create end entity and issue certificate
        ejbcaService.createEndEntity(uuid, username, password, subjectDn, request.getRaProfileAttributes(), request.getAttributes());
        // issue certificate
        CertificateDataResponseDto certificate = ejbcaService.issueCertificate(uuid, username, password, Base64.getEncoder().encodeToString(csr.getEncoded()));

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put(META_EJBCA_USERNAME, username);
        meta.put(META_EMAIL, AttributeDefinitionUtils.getAttributeContent(CertificateControllerImpl.ATTRIBUTE_EMAIL, request.getRaProfileAttributes(), BaseAttributeContent.class).getValue());
        meta.put(META_SAN, AttributeDefinitionUtils.getAttributeContent(CertificateControllerImpl.ATTRIBUTE_SAN, request.getRaProfileAttributes(), BaseAttributeContent.class).getValue());
        meta.put(META_EXTENSION, AttributeDefinitionUtils.getAttributeContent(CertificateControllerImpl.ATTRIBUTE_EXTENSION, request.getRaProfileAttributes(), BaseAttributeContent.class).getValue());

        certificate.setMeta(MetaDefinitions.serialize(meta));

        return certificate;
    }

    @Override
    public CertificateDataResponseDto renewCertificate(String uuid, CertificateRenewRequestDto request) throws Exception {
        JcaPKCS10CertificationRequest csr = parseCsrToJcaObject(request.getPkcs10());

        Map<String, Object> metadata = MetaDefinitions.deserialize(request.getMeta());

        // check if we have the username in the metadata, and if not, generate username
        String username = null;
        if (!request.getMeta().isEmpty()) {
            username = (String) metadata.get(META_EJBCA_USERNAME);
        }
        if (StringUtils.isBlank(username)) {
            String usernameGenMethod = (String) AttributeDefinitionUtils.getAttributeContent(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_GEN_METHOD, request.getRaProfileAttributes(), BaseAttributeContent.class).getValue();
            String usernamePrefix = (String) AttributeDefinitionUtils.getAttributeContent(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_PREFIX, request.getRaProfileAttributes(), BaseAttributeContent.class).getValue();
            String usernamePostfix = (String) AttributeDefinitionUtils.getAttributeContent(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_POSTFIX, request.getRaProfileAttributes(), BaseAttributeContent.class).getValue();
            username = generateUsername(usernameGenMethod, usernamePrefix, usernamePostfix, csr);
        }

        String password = username;

        try {
            // update end entity
            ejbcaService.renewEndEntity(uuid, username, password);
        } catch (NotFoundException e) {
            String subjectDn = csr.getSubject().toString();
            ejbcaService.createEndEntity(uuid, username, password, subjectDn, request.getRaProfileAttributes(), metadata);
        }

        // issue certificate
        CertificateDataResponseDto certificate = ejbcaService.issueCertificate(uuid, username, password, Base64.getEncoder().encodeToString(csr.getEncoded()));

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.putAll(metadata);
        meta.put(META_EJBCA_USERNAME, username); // the username must be as a last one, in order to not overwrite the metadata value

        certificate.setMeta(MetaDefinitions.serialize(meta));

        return certificate;
    }

    private JcaPKCS10CertificationRequest parseCsrToJcaObject(String pkcs10) throws IOException {
        JcaPKCS10CertificationRequest csr;
        try {
            csr = CsrUtil.csrStringToJcaObject(pkcs10);
        } catch (IOException e) {
            logger.debug("Failed to parse CSR, will decode and try again...");
            String decodedPkcs10 = new String(Base64.getDecoder().decode(pkcs10));
            csr = CsrUtil.csrStringToJcaObject(decodedPkcs10);
        }
        return csr;
    }

    private String generateUsername(String usernameGenMethod, String usernamePrefix, String usernamePostfix, JcaPKCS10CertificationRequest csr) throws Exception {
        // the csr comes Base64 encoded
        String username;
        if (usernameGenMethod.equals(UsernameGenMethod.RANDOM.name())) {
            SecureRandom random = new SecureRandom();
            byte[] r = new byte[8];
            random.nextBytes(r);
            username = Base64.getEncoder().encodeToString(r);
        } else if (usernameGenMethod.equals(UsernameGenMethod.CN.name())) {
            if (csr == null) {
                throw new IOException("CSR failed to be decoded!");
            }
            username = getX500Field(COMMON_NAME, csr.getSubject());
        } else {
            String message = "Unsupported username generation method: " + usernameGenMethod;
            logger.debug(message);
            throw new Exception(message);
        }
        if (StringUtils.isNotBlank(username)) {
            if (StringUtils.isNotBlank(usernamePrefix)) {
                username = usernamePrefix + username;
            }
            if (StringUtils.isNotBlank(usernamePostfix)) {
                username = username + usernamePostfix;
            }
        } else {
            String message = "Username is null or empty, username was not properly generated";
            logger.debug(message);
            throw new Exception(message);
        }
        return username;
    }

    @Override
    public void revokeCertificate(String uuid, CertRevocationDto request) throws NotFoundException, AccessDeniedException {
        try {
            X509Certificate certificate = CertificateUtil.parseCertificate(request.getCertificate());
            String issuerDn = CertificateUtil.getIssuerDnFromX509Certificate(certificate);
            String serialNumber = CertificateUtil.getSerialNumberFromX509Certificate(certificate);
            int revocationCode = request.getReason().getCode();
            ejbcaService.revokeCertificate(uuid, issuerDn, serialNumber, revocationCode);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String getX500Field(String asn1ObjectIdentifier, X500Name x500Name) {
        RDN[] rdnArray = x500Name.getRDNs(new ASN1ObjectIdentifier(asn1ObjectIdentifier));
        String retVal = null;
        for (RDN item : rdnArray) {
            retVal = item.getFirst().getValue().toString();
        }
        return retVal;
    }
}

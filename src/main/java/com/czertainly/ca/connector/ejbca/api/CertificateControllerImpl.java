package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.exception.CertificateOperationException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.interfaces.connector.v2.CertificateController;
import com.czertainly.api.model.client.attribute.RequestAttributeDto;
import com.czertainly.api.model.common.attribute.v2.AttributeType;
import com.czertainly.api.model.common.attribute.v2.BaseAttribute;
import com.czertainly.api.model.common.attribute.v2.DataAttribute;
import com.czertainly.api.model.common.attribute.v2.content.AttributeContentType;
import com.czertainly.api.model.common.attribute.v2.properties.DataAttributeProperties;
import com.czertainly.api.model.connector.v2.CertRevocationDto;
import com.czertainly.api.model.connector.v2.CertificateDataResponseDto;
import com.czertainly.api.model.connector.v2.CertificateRenewRequestDto;
import com.czertainly.api.model.connector.v2.CertificateSignRequestDto;
import com.czertainly.ca.connector.ejbca.service.CertificateEjbcaService;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class CertificateControllerImpl implements CertificateController {

    public static final String ATTRIBUTE_EMAIL = "email";
    public static final String ATTRIBUTE_SAN = "san";
    public static final String ATTRIBUTE_EXTENSION = "extension";

    public static final String ATTRIBUTE_EMAIL_LABEL = "Email";
    public static final String ATTRIBUTE_SAN_LABEL = "Subject Alternative Name";
    public static final String ATTRIBUTE_EXTENSION_LABEL = "Extension Data";
    private CertificateEjbcaService certificateEjbcaService;

    @Autowired
    public void setCertificateEjbcaService(CertificateEjbcaService certificateEjbcaService) {
        this.certificateEjbcaService = certificateEjbcaService;
    }

    @Override
    public List<BaseAttribute> listIssueCertificateAttributes(String uuid) {
        List<BaseAttribute> attrs = new ArrayList<>();

        DataAttribute email = new DataAttribute();
        email.setUuid("0b378474-ebe9-4a17-9d3d-0577eb16aa34");
        email.setName(ATTRIBUTE_EMAIL);
        email.setDescription("End Entity email address");
        email.setType(AttributeType.DATA);
        email.setContentType(AttributeContentType.STRING);
        DataAttributeProperties emailProperties = new DataAttributeProperties();
        emailProperties.setLabel(ATTRIBUTE_EMAIL_LABEL);
        emailProperties.setRequired(false);
        emailProperties.setReadOnly(false);
        emailProperties.setVisible(true);
        emailProperties.setList(false);
        emailProperties.setMultiSelect(false);
        email.setProperties(emailProperties);
        attrs.add(email);

        DataAttribute san = new DataAttribute();
        san.setUuid("2cfd8c1a-e867-42f1-ab6c-67fb1964e163");
        san.setName(ATTRIBUTE_SAN);
        san.setDescription("Comma separated Subject Alternative Names");
        san.setType(AttributeType.DATA);
        san.setContentType(AttributeContentType.STRING);
        DataAttributeProperties sanProperties = new DataAttributeProperties();
        sanProperties.setLabel(ATTRIBUTE_SAN_LABEL);
        sanProperties.setRequired(false);
        sanProperties.setReadOnly(false);
        sanProperties.setVisible(true);
        sanProperties.setList(false);
        sanProperties.setMultiSelect(false);
        san.setProperties(sanProperties);
        attrs.add(san);

        DataAttribute extension = new DataAttribute();
        extension.setUuid("72324d22-12cb-47ee-a02e-0b1da2013eee");
        extension.setName(ATTRIBUTE_EXTENSION);
        extension.setDescription("Comma separated Extension Data");
        extension.setType(AttributeType.DATA);
        extension.setContentType(AttributeContentType.STRING);
        DataAttributeProperties extensionProperties = new DataAttributeProperties();
        extensionProperties.setLabel(ATTRIBUTE_EXTENSION_LABEL);
        extensionProperties.setRequired(false);
        extensionProperties.setReadOnly(false);
        extensionProperties.setVisible(true);
        extensionProperties.setList(false);
        extensionProperties.setMultiSelect(false);
        extension.setProperties(extensionProperties);
        attrs.add(extension);

        return attrs;
    }

    @Override
    public void validateIssueCertificateAttributes(String uuid, List<RequestAttributeDto> attributes) throws ValidationException {
        AttributeDefinitionUtils.validateAttributes(
                listIssueCertificateAttributes(uuid),
                attributes);
    }

    @Override
    public CertificateDataResponseDto issueCertificate(String uuid, CertificateSignRequestDto request) throws CertificateOperationException {
        try {
            return certificateEjbcaService.issueCertificate(uuid, request);
        } catch (Exception e) {
            throw new CertificateOperationException(e.getMessage());
        }
    }

    @Override
    public CertificateDataResponseDto renewCertificate(String uuid, CertificateRenewRequestDto request) throws CertificateOperationException {
        try {
            return certificateEjbcaService.renewCertificate(uuid, request);
        } catch (Exception e) {
            throw new CertificateOperationException(e.getMessage());
        }
    }

    @Override
    public List<BaseAttribute> listRevokeCertificateAttributes(String uuid) {
        return List.of();
    }

    @Override
    public void validateRevokeCertificateAttributes(String uuid, List<RequestAttributeDto> attributes) throws ValidationException {
        AttributeDefinitionUtils.validateAttributes(
                listRevokeCertificateAttributes(uuid),
                attributes);
    }

    @Override
    public void revokeCertificate(String uuid, CertRevocationDto request) throws NotFoundException, AccessDeniedException {
        certificateEjbcaService.revokeCertificate(uuid, request);
    }
}

package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.interfaces.connector.v2.CertificateController;
import com.czertainly.api.model.common.attribute.AttributeDefinition;
import com.czertainly.api.model.common.attribute.AttributeType;
import com.czertainly.api.model.common.attribute.RequestAttributeDto;
import com.czertainly.api.model.connector.v2.CertRevocationDto;
import com.czertainly.api.model.connector.v2.CertificateDataResponseDto;
import com.czertainly.api.model.connector.v2.CertificateRenewRequestDto;
import com.czertainly.api.model.connector.v2.CertificateSignRequestDto;
import com.czertainly.ca.connector.ejbca.service.CertificateEjbcaService;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    public void setCertificateEjbcaService(CertificateEjbcaService certificateEjbcaService) {
        this.certificateEjbcaService = certificateEjbcaService;
    }

    private CertificateEjbcaService certificateEjbcaService;

    @Override
    public List<AttributeDefinition> listIssueCertificateAttributes(String uuid) {
        List<AttributeDefinition> attrs = new ArrayList<>();

        AttributeDefinition email = new AttributeDefinition();
        email.setUuid("0b378474-ebe9-4a17-9d3d-0577eb16aa34");
        email.setName(ATTRIBUTE_EMAIL);
        email.setLabel(ATTRIBUTE_EMAIL_LABEL);
        email.setDescription("End Entity email address");
        email.setType(AttributeType.STRING);
        email.setRequired(false);
        email.setReadOnly(false);
        email.setVisible(true);
        email.setList(false);
        email.setMultiSelect(false);
        attrs.add(email);

        AttributeDefinition san = new AttributeDefinition();
        san.setUuid("2cfd8c1a-e867-42f1-ab6c-67fb1964e163");
        san.setName(ATTRIBUTE_SAN);
        san.setLabel(ATTRIBUTE_SAN_LABEL);
        san.setDescription("Comma separated Subject Alternative Names");
        san.setType(AttributeType.STRING);
        san.setRequired(false);
        san.setReadOnly(false);
        san.setVisible(true);
        san.setList(false);
        san.setMultiSelect(false);
        attrs.add(san);

        AttributeDefinition extension = new AttributeDefinition();
        extension.setUuid("72324d22-12cb-47ee-a02e-0b1da2013eee");
        extension.setName(ATTRIBUTE_EXTENSION);
        extension.setLabel(ATTRIBUTE_EXTENSION_LABEL);
        extension.setDescription("Comma separated Extension Data");
        extension.setType(AttributeType.STRING);
        extension.setRequired(false);
        extension.setReadOnly(false);
        extension.setVisible(true);
        extension.setList(false);
        extension.setMultiSelect(false);
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
    public CertificateDataResponseDto issueCertificate(String uuid, CertificateSignRequestDto request) {
        try {
            return certificateEjbcaService.issueCertificate(uuid, request);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Override
    public CertificateDataResponseDto renewCertificate(String uuid, CertificateRenewRequestDto request) {
        try {
            return certificateEjbcaService.renewCertificate(uuid, request);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Override
    public List<AttributeDefinition> listRevokeCertificateAttributes(String uuid) {
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

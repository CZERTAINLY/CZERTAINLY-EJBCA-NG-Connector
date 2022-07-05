package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.ValidationError;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.common.attribute.*;
import com.czertainly.api.model.common.attribute.content.BaseAttributeContent;
import com.czertainly.api.model.common.attribute.content.JsonAttributeContent;
import com.czertainly.ca.connector.ejbca.dao.AuthorityInstanceRepository;
import com.czertainly.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.czertainly.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificateCriteriaRestRequest;
import com.czertainly.ca.connector.ejbca.enums.DiscoveryKind;
import com.czertainly.ca.connector.ejbca.service.DiscoveryAttributeService;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DiscoveryAttributeServiceImpl implements DiscoveryAttributeService {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryAttributeServiceImpl.class);

    public static final String ATTRIBUTE_EJBCA_INSTANCE = "ejbcaInstance";
    public static final String ATTRIBUTE_END_ENTITY_PROFILE = "endEntityProfile";
    public static final String ATTRIBUTE_EJBCA_RESTAPI_URL = "ejbcaRestApiUrl";
    public static final String ATTRIBUTE_EJBCA_CA = "ca";
    public static final String ATTRIBUTE_EJBCA_STATUS = "status";
    public static final String ATTRIBUTE_EJBCA_ISSUED_AFTER = "issuedAfter";
    public static final String ATTRIBUTE_ISSUED_DAYS_BEFORE = "issuedDaysBefore";


    public static final String ATTRIBUTE_EJBCA_INSTANCE_LABEL = "EJBCA instance";
    public static final String ATTRIBUTE_END_ENTITY_PROFILE_LABEL = "End Entity Profile";
    public static final String ATTRIBUTE_EJBCA_RESTAPI_URL_LABEL = "EJBCA REST API base URL";
    public static final String ATTRIBUTE_EJBCA_CA_LABEL = "Certification Authority";
    public static final String ATTRIBUTE_EJBCA_STATUS_LABEL = "Certificate status";
    public static final String ATTRIBUTE_EJBCA_ISSUED_AFTER_LABEL = "Certificates issued after";
    public static final String ATTRIBUTE_ISSUED_DAYS_BEFORE_LABEL = "Number of days";


    @Autowired
    public void setAuthorityInstanceRepository(AuthorityInstanceRepository authorityInstanceRepository) {
        this.authorityInstanceRepository = authorityInstanceRepository;
    }

    private AuthorityInstanceRepository authorityInstanceRepository;

    @Override
    public List<AttributeDefinition> getAttributes(String kind) {
        if (!kind.equals(DiscoveryKind.EJBCA.name()) && !kind.equals(DiscoveryKind.EJBCA_SCHEDULE.name())) {
            throw new ValidationException("Unsupported kind: " + kind, new ValidationError("Unsupported kind: " + kind));
        }
        logger.info("Listing discovery attributes for {}", kind);

        List<AttributeDefinition> attributes = new ArrayList<>();
        attributes.add(prepareEjbcaInstanceAttribute());
        attributes.add(ejbcaRestApiUrl());
        attributes.add(listCas());
        attributes.add(listEndEntityProfiles());
        attributes.add(listStatus());

        if (kind.equals(DiscoveryKind.EJBCA.name())) {
            attributes.add(issuedAfter());
        }

        if (kind.equals(DiscoveryKind.EJBCA_SCHEDULE.name())) {
            attributes.add(issuedDaysBefore());
        }

        return attributes;
    }

    @Override
    public boolean validateAttributes(String kind, List<RequestAttributeDto> attributes) {
        if (attributes == null) {
            return false;
        }

        if (!kind.equals(DiscoveryKind.EJBCA.name()) && !kind.equals(DiscoveryKind.EJBCA_SCHEDULE.name())) {
            throw new ValidationException("Unsupported kind: " + kind);
        }

        AttributeDefinitionUtils.validateAttributes(getAttributes(kind), attributes);

        return true;
    }

    private AttributeDefinition prepareEjbcaInstanceAttribute() {
        List<AuthorityInstanceNameAndUuidDto> instanceNames = authorityInstanceRepository.findAll().stream().map(AuthorityInstance::mapToNameAndUuidDto).collect(Collectors.toList());
        List<JsonAttributeContent> contentList = new ArrayList<>();
        for (AuthorityInstanceNameAndUuidDto instance : instanceNames) {
            JsonAttributeContent content = new JsonAttributeContent(instance.getName(), instance);
            contentList.add(content);
        }

        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setUuid("dce22e96-3335-4181-b90c-c7f887d8d109");
        attribute.setName(ATTRIBUTE_EJBCA_INSTANCE);
        attribute.setLabel(ATTRIBUTE_EJBCA_INSTANCE_LABEL);
        attribute.setDescription("The EJBCA instance where Discovery process should search for Certificates");
        attribute.setType(AttributeType.JSON);
        attribute.setRequired(true);
        attribute.setReadOnly(false);
        attribute.setVisible(true);
        attribute.setList(true);
        attribute.setMultiSelect(false);
        attribute.setContent(contentList);

        return attribute;
    }

    private AttributeDefinition listCas() {
        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setUuid("ffe7c27a-48e4-41fa-93de-8ddac65fec46");
        attribute.setName(ATTRIBUTE_EJBCA_CA);
        attribute.setLabel(ATTRIBUTE_EJBCA_CA_LABEL);
        attribute.setDescription("Available certification authorities for Discovery");
        attribute.setType(AttributeType.JSON);
        attribute.setRequired(false);
        attribute.setReadOnly(false);
        attribute.setVisible(true);
        attribute.setList(true);
        attribute.setMultiSelect(true);
        attribute.setContent(List.of());

        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(ATTRIBUTE_EJBCA_INSTANCE+".data.uuid", "ejbcaInstanceUuid", AttributeValueTarget.PATH_VARIABLE));

        AttributeCallback attributeCallback = new AttributeCallback();
        attributeCallback.setCallbackContext("/v1/discoveryProvider/{ejbcaInstanceUuid}/listCas");
        attributeCallback.setCallbackMethod("POST");
        attributeCallback.setMappings(mappings);

        attribute.setAttributeCallback(attributeCallback);

        return attribute;
    }

    private AttributeDefinition listEndEntityProfiles() {
        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setUuid("bbf2d142-f35a-437f-81c7-35c128881fc0");
        attribute.setName(ATTRIBUTE_END_ENTITY_PROFILE);
        attribute.setLabel(ATTRIBUTE_END_ENTITY_PROFILE_LABEL);
        attribute.setDescription("The End Entity Profile where Discovery process should search for Certificates");
        attribute.setType(AttributeType.JSON);
        attribute.setRequired(false);
        attribute.setReadOnly(false);
        attribute.setVisible(true);
        attribute.setList(true);
        attribute.setMultiSelect(true);
        attribute.setContent(List.of());

        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(ATTRIBUTE_EJBCA_INSTANCE+".data.uuid", "ejbcaInstanceUuid", AttributeValueTarget.PATH_VARIABLE));

        AttributeCallback attributeCallback = new AttributeCallback();
        attributeCallback.setCallbackContext("/v1/discoveryProvider/{ejbcaInstanceUuid}/listEndEntityProfiles");
        attributeCallback.setCallbackMethod("POST");
        attributeCallback.setMappings(mappings);

        attribute.setAttributeCallback(attributeCallback);

        return attribute;
    }

    private AttributeDefinition listStatus() {
        List<BaseAttributeContent<String>> statuses = new ArrayList<>();
        for (SearchCertificateCriteriaRestRequest.CertificateStatus status : SearchCertificateCriteriaRestRequest.CertificateStatus.values()) {
            BaseAttributeContent<String> content = new BaseAttributeContent<>(status.name());
            statuses.add(content);
        }

        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setUuid("2aba1544-abdf-46a0-ab56-ac79f9163018");
        attribute.setName(ATTRIBUTE_EJBCA_STATUS);
        attribute.setLabel(ATTRIBUTE_EJBCA_STATUS_LABEL);
        attribute.setDescription("Filter certificate status");
        attribute.setType(AttributeType.JSON);
        attribute.setRequired(false);
        attribute.setReadOnly(false);
        attribute.setVisible(true);
        attribute.setList(true);
        attribute.setMultiSelect(true);
        attribute.setContent(statuses);

        return attribute;
    }

    private AttributeDefinition ejbcaRestApiUrl() {
        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setUuid("c5b974dd-e00a-44b6-b9bc-0946e79730a2");
        attribute.setName(ATTRIBUTE_EJBCA_RESTAPI_URL);
        attribute.setLabel(ATTRIBUTE_EJBCA_RESTAPI_URL_LABEL);
        attribute.setDescription("Base URL of the EJBCA REST API to be used");
        attribute.setType(AttributeType.STRING);
        attribute.setRequired(true);
        attribute.setReadOnly(false);
        attribute.setVisible(true);
        attribute.setList(false);
        attribute.setMultiSelect(false);

        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(ATTRIBUTE_EJBCA_INSTANCE+".data.uuid", "ejbcaInstanceUuid", AttributeValueTarget.PATH_VARIABLE));

        AttributeCallback attributeCallback = new AttributeCallback();
        attributeCallback.setCallbackContext("/v1/discoveryProvider/{ejbcaInstanceUuid}/ejbcaRestApi");
        attributeCallback.setCallbackMethod("POST");
        attributeCallback.setMappings(mappings);

        attribute.setAttributeCallback(attributeCallback);

        return attribute;
    }

    private AttributeDefinition issuedAfter() {
        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setUuid("4954adc0-47f0-442d-9347-f270d9ac0074");
        attribute.setName(ATTRIBUTE_EJBCA_ISSUED_AFTER);
        attribute.setLabel(ATTRIBUTE_EJBCA_ISSUED_AFTER_LABEL);
        attribute.setDescription("The date after the certificates were issued");
        attribute.setType(AttributeType.DATETIME);
        attribute.setRequired(false);
        attribute.setReadOnly(false);
        attribute.setVisible(true);
        attribute.setList(false);
        attribute.setMultiSelect(false);

        return attribute;
    }

    private AttributeDefinition issuedDaysBefore() {
        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setUuid("4a92a6c5-38c0-4ebf-8297-594d39572c9c");
        attribute.setName(ATTRIBUTE_ISSUED_DAYS_BEFORE);
        attribute.setLabel(ATTRIBUTE_ISSUED_DAYS_BEFORE_LABEL);
        attribute.setDescription("Maximum number of days before the certificate was issued, from running the discovery");
        attribute.setType(AttributeType.INTEGER);
        attribute.setRequired(true);
        attribute.setReadOnly(false);
        attribute.setVisible(true);
        attribute.setList(false);
        attribute.setMultiSelect(false);
        attribute.setContent(new BaseAttributeContent<>(5));

        return attribute;
    }
}

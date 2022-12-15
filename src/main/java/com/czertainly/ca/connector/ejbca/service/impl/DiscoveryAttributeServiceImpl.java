package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.ValidationError;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.client.attribute.RequestAttributeDto;
import com.czertainly.api.model.common.attribute.v2.AttributeType;
import com.czertainly.api.model.common.attribute.v2.BaseAttribute;
import com.czertainly.api.model.common.attribute.v2.DataAttribute;
import com.czertainly.api.model.common.attribute.v2.InfoAttribute;
import com.czertainly.api.model.common.attribute.v2.callback.AttributeCallback;
import com.czertainly.api.model.common.attribute.v2.callback.AttributeCallbackMapping;
import com.czertainly.api.model.common.attribute.v2.callback.AttributeValueTarget;
import com.czertainly.api.model.common.attribute.v2.content.*;
import com.czertainly.api.model.common.attribute.v2.properties.DataAttributeProperties;
import com.czertainly.api.model.common.attribute.v2.properties.InfoAttributeProperties;
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
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryAttributeServiceImpl.class);
    private AuthorityInstanceRepository authorityInstanceRepository;

    @Autowired
    public void setAuthorityInstanceRepository(AuthorityInstanceRepository authorityInstanceRepository) {
        this.authorityInstanceRepository = authorityInstanceRepository;
    }

    @Override
    public List<BaseAttribute> getAttributes(String kind) {
        if (!kind.equals(DiscoveryKind.EJBCA.name()) && !kind.equals(DiscoveryKind.EJBCA_SCHEDULE.name())) {
            throw new ValidationException("Unsupported kind: " + kind, new ValidationError("Unsupported kind: " + kind));
        }
        logger.info("Listing discovery attributes for {}", kind);

        List<BaseAttribute> attributes = new ArrayList<>();
        attributes.add(infoDiscoveryDescription());
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

    private DataAttribute prepareEjbcaInstanceAttribute() {
        List<AuthorityInstanceNameAndUuidDto> instanceNames = authorityInstanceRepository.findAll().stream().map(AuthorityInstance::mapToNameAndUuidDto).collect(Collectors.toList());
        List<BaseAttributeContent> contentList = new ArrayList<>();
        for (AuthorityInstanceNameAndUuidDto instance : instanceNames) {
            ObjectAttributeContent content = new ObjectAttributeContent(instance.getName(), instance);
            contentList.add(content);
        }

        DataAttribute attribute = new DataAttribute();
        attribute.setUuid("dce22e96-3335-4181-b90c-c7f887d8d109");
        attribute.setName(ATTRIBUTE_EJBCA_INSTANCE);
        attribute.setContentType(AttributeContentType.OBJECT);
        attribute.setDescription("The EJBCA instance where Discovery process should search for Certificates");
        attribute.setType(AttributeType.DATA);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_EJBCA_INSTANCE_LABEL);
        attributeProperties.setRequired(true);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(true);
        attributeProperties.setMultiSelect(false);
        attribute.setProperties(attributeProperties);
        attribute.setContent(contentList);

        return attribute;
    }

    private DataAttribute listCas() {
        DataAttribute attribute = new DataAttribute();
        attribute.setUuid("ffe7c27a-48e4-41fa-93de-8ddac65fec46");
        attribute.setName(ATTRIBUTE_EJBCA_CA);
        attribute.setDescription("Available certification authorities for Discovery");
        attribute.setContentType(AttributeContentType.OBJECT);
        attribute.setType(AttributeType.DATA);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_EJBCA_CA_LABEL);
        attributeProperties.setRequired(false);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(true);
        attributeProperties.setMultiSelect(true);
        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of());

        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(ATTRIBUTE_EJBCA_INSTANCE + ".data.uuid", "ejbcaInstanceUuid", AttributeValueTarget.PATH_VARIABLE));

        AttributeCallback attributeCallback = new AttributeCallback();
        attributeCallback.setCallbackContext("/v1/discoveryProvider/{ejbcaInstanceUuid}/listCas");
        attributeCallback.setCallbackMethod("GET");
        attributeCallback.setMappings(mappings);

        attribute.setAttributeCallback(attributeCallback);

        return attribute;
    }

    private DataAttribute listEndEntityProfiles() {
        DataAttribute attribute = new DataAttribute();
        attribute.setUuid("bbf2d142-f35a-437f-81c7-35c128881fc0");
        attribute.setName(ATTRIBUTE_END_ENTITY_PROFILE);
        attribute.setDescription("The End Entity Profile where Discovery process should search for Certificates");
        attribute.setType(AttributeType.DATA);
        attribute.setContentType(AttributeContentType.OBJECT);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_END_ENTITY_PROFILE_LABEL);
        attributeProperties.setRequired(false);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(true);
        attributeProperties.setMultiSelect(true);
        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of());

        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(ATTRIBUTE_EJBCA_INSTANCE + ".data.uuid", "ejbcaInstanceUuid", AttributeValueTarget.PATH_VARIABLE));

        AttributeCallback attributeCallback = new AttributeCallback();
        attributeCallback.setCallbackContext("/v1/discoveryProvider/{ejbcaInstanceUuid}/listEndEntityProfiles");
        attributeCallback.setCallbackMethod("GET");
        attributeCallback.setMappings(mappings);

        attribute.setAttributeCallback(attributeCallback);

        return attribute;
    }

    private DataAttribute listStatus() {
        List<BaseAttributeContent> statuses = new ArrayList<>();
        for (SearchCertificateCriteriaRestRequest.CertificateStatus status : SearchCertificateCriteriaRestRequest.CertificateStatus.values()) {
            StringAttributeContent content = new StringAttributeContent(status.name());
            statuses.add(content);
        }

        DataAttribute attribute = new DataAttribute();
        attribute.setUuid("2aba1544-abdf-46a0-ab56-ac79f9163018");
        attribute.setName(ATTRIBUTE_EJBCA_STATUS);
        attribute.setDescription("Filter certificate status");
        attribute.setType(AttributeType.DATA);
        attribute.setContentType(AttributeContentType.OBJECT);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_EJBCA_STATUS_LABEL);
        attributeProperties.setRequired(false);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(true);
        attributeProperties.setMultiSelect(true);
        attribute.setProperties(attributeProperties);
        attribute.setContent(statuses);

        return attribute;
    }

    private DataAttribute ejbcaRestApiUrl() {
        DataAttribute attribute = new DataAttribute();
        attribute.setUuid("c5b974dd-e00a-44b6-b9bc-0946e79730a2");
        attribute.setName(ATTRIBUTE_EJBCA_RESTAPI_URL);
        attribute.setDescription("Base URL of the EJBCA REST API to be used");
        attribute.setType(AttributeType.DATA);
        attribute.setContentType(AttributeContentType.STRING);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_EJBCA_RESTAPI_URL_LABEL);
        attributeProperties.setRequired(true);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(false);
        attributeProperties.setMultiSelect(false);
        attribute.setProperties(attributeProperties);

        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(ATTRIBUTE_EJBCA_INSTANCE + ".data.uuid", "ejbcaInstanceUuid", AttributeValueTarget.PATH_VARIABLE));

        AttributeCallback attributeCallback = new AttributeCallback();
        attributeCallback.setCallbackContext("/v1/discoveryProvider/{ejbcaInstanceUuid}/ejbcaRestApi");
        attributeCallback.setCallbackMethod("GET");
        attributeCallback.setMappings(mappings);

        attribute.setAttributeCallback(attributeCallback);

        return attribute;
    }

    private DataAttribute issuedAfter() {
        DataAttribute attribute = new DataAttribute();
        attribute.setUuid("4954adc0-47f0-442d-9347-f270d9ac0074");
        attribute.setName(ATTRIBUTE_EJBCA_ISSUED_AFTER);
        attribute.setDescription("The date after the certificates were issued");
        attribute.setType(AttributeType.DATA);
        attribute.setContentType(AttributeContentType.DATETIME);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_EJBCA_ISSUED_AFTER_LABEL);
        attributeProperties.setRequired(false);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(false);
        attributeProperties.setMultiSelect(false);
        attribute.setProperties(attributeProperties);
        return attribute;
    }

    private DataAttribute issuedDaysBefore() {
        DataAttribute attribute = new DataAttribute();
        attribute.setUuid("4a92a6c5-38c0-4ebf-8297-594d39572c9c");
        attribute.setName(ATTRIBUTE_ISSUED_DAYS_BEFORE);
        attribute.setDescription("Maximum number of days before the certificate was issued, from running the discovery");
        attribute.setType(AttributeType.DATA);
        attribute.setContentType(AttributeContentType.INTEGER);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_ISSUED_DAYS_BEFORE_LABEL);
        attributeProperties.setRequired(true);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(false);
        attributeProperties.setMultiSelect(false);
        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of(new IntegerAttributeContent(5)));

        return attribute;
    }

    private InfoAttribute infoDiscoveryDescription() {
        InfoAttribute attribute = new InfoAttribute();
        attribute.setUuid("4a92a6c5-38c0-4ebf-8297-594d39572c9c");
        attribute.setName("info_discoveryProcess");
        attribute.setDescription("Discovery process information");
        attribute.setType(AttributeType.INFO);
        attribute.setContentType(AttributeContentType.TEXT);
        InfoAttributeProperties attributeProperties = new InfoAttributeProperties();
        attributeProperties.setLabel("Discovery process information");
        attributeProperties.setVisible(true);
        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of(new TextAttributeContent("## Overview<br>  " +
                "Select EJBCA instance where Discovery process should search for Certificates and then you can optionally select:<br>  " +
                "- Certification authority<br>  " +
                "- End Entity Profile<br>  " +
                "- Certificate status")));

        return attribute;
    }
}

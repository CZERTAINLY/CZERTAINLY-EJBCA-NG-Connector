package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.ValidationError;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.common.*;
import com.czertainly.ca.connector.ejbca.dao.AuthorityInstanceRepository;
import com.czertainly.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.czertainly.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.czertainly.ca.connector.ejbca.service.DiscoveryAttributeService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.util.EjbcaVersion;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.apache.commons.lang3.StringUtils;
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
    public static final String ATTRIBUTE_EJBCA_VERSION = "ejbcaVersion";
    public static final String ATTRIBUTE_END_ENTITY_PROFILE = "endEntityProfile";


    public static final String ATTRIBUTE_EJBCA_INSTANCE_LABEL = "EJBCA instance";
    public static final String ATTRIBUTE_EJBCA_VERSION_LABEL = "EJBCA version";
    public static final String ATTRIBUTE_END_ENTITY_PROFILE_LABEL = "End Entity Profile";


    @Autowired
    public void setAuthorityInstanceRepository(AuthorityInstanceRepository authorityInstanceRepository) {
        this.authorityInstanceRepository = authorityInstanceRepository;
    }
    @Autowired
    public void setEjbcaService(EjbcaService ejbcaService) {
        this.ejbcaService = ejbcaService;
    }

    private AuthorityInstanceRepository authorityInstanceRepository;
    private EjbcaService ejbcaService;

    @Override
    public List<AttributeDefinition> getAttributes(String kind) {
        List<AttributeDefinition> attributes = new ArrayList<>();

        attributes.add(prepareEjbcaInstanceAttribute());
        //attributes.add(getEjbcaVersionString());
        attributes.add(listEndEntityProfiles());

        return attributes;
    }

    @Override
    public boolean validateAttributes(String kind, List<RequestAttributeDto> attributes) {
        List<ValidationError> errors = new ArrayList<>();

        if (attributes == null) {
            return false;
        }

        if (!kind.equals("EJBCA")) {
            throw new ValidationException("Unsupported kind: " + kind);
        }

        AttributeDefinitionUtils.validateAttributes(getAttributes(kind), attributes);

//        String ejbcaVersion = AttributeDefinitionUtils.getAttributeValue(ATTRIBUTE_EJBCA_VERSION, attributes);
//        if (StringUtils.isBlank(ejbcaVersion)) {
//            errors.add(ValidationError.create("EJBCA version missing", ATTRIBUTE_EJBCA_VERSION));
//            throw new ValidationException("EJBCA version missing", errors);
//        }
//        EjbcaVersion version = new EjbcaVersion(ejbcaVersion);
//
//        if (version.getTechVersion() < 7 || version.getMajorVersion() < 9) {
//            logger.debug("{} does not supported discovery", ejbcaVersion);
//            errors.add(ValidationError.create("Unsupported EJBCA version", ATTRIBUTE_EJBCA_VERSION));
//            throw new ValidationException("Unsupported version: " + ejbcaVersion, errors);
//        }

        return true;
    }

    private AttributeDefinition prepareEjbcaInstanceAttribute() {
        List<AuthorityInstanceNameAndUuidDto> instanceNames = authorityInstanceRepository.findAll().stream().map(AuthorityInstance::mapToNameAndUuidDto).collect(Collectors.toList());

        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setUuid("dce22e96-3335-4181-b90c-c7f887d8d109");
        attribute.setName(ATTRIBUTE_EJBCA_INSTANCE);
        attribute.setLabel(ATTRIBUTE_EJBCA_INSTANCE_LABEL);
        attribute.setDescription("The EJBCA instance where Discovery process should search for Certificates");
        attribute.setType(BaseAttributeDefinitionTypes.LIST);
        attribute.setRequired(true);
        attribute.setReadOnly(false);
        attribute.setVisible(true);
        attribute.setValue(instanceNames.toArray());

        return attribute;
    }

    private AttributeDefinition listEndEntityProfiles() {
        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setUuid("bbf2d142-f35a-437f-81c7-35c128881fc0");
        attribute.setName(ATTRIBUTE_END_ENTITY_PROFILE);
        attribute.setLabel(ATTRIBUTE_END_ENTITY_PROFILE_LABEL);
        attribute.setDescription("The End Entity Profile where Discovery process should search for Certificates");
        attribute.setType(BaseAttributeDefinitionTypes.LIST);
        attribute.setRequired(true);
        attribute.setReadOnly(false);
        attribute.setVisible(true);
        attribute.setValue(List.of().toArray());

        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(ATTRIBUTE_EJBCA_INSTANCE, "selectedEjbcaInstance", AttributeValueTarget.BODY));

        AttributeCallback attributeCallback = new AttributeCallback();
        attributeCallback.setCallbackContext("/v1/discoveryProvider/listEndEntityProfiles");
        attributeCallback.setCallbackMethod("POST");
        attributeCallback.setMappings(mappings);

        attribute.setAttributeCallback(attributeCallback);

        return attribute;
    }

    private AttributeDefinition getEjbcaVersionString() {
        AttributeDefinition attribute = new AttributeDefinition();
        attribute.setUuid("eda7a9fc-a663-4275-88b2-9591b932f731");
        attribute.setName(ATTRIBUTE_EJBCA_VERSION);
        attribute.setLabel(ATTRIBUTE_EJBCA_VERSION_LABEL);
        attribute.setDescription("The version of EJBCA that is running on the instance");
        attribute.setType(BaseAttributeDefinitionTypes.STRING);
        attribute.setRequired(false);
        attribute.setReadOnly(true);
        attribute.setVisible(true);
        attribute.setValue(""); // empty string  and the value will be based on the callback

        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(ATTRIBUTE_EJBCA_INSTANCE, "ejbcaInstanceName", AttributeValueTarget.PATH_VARIABLE));

        AttributeCallback attributeCallback = new AttributeCallback();
        attributeCallback.setCallbackContext("/v1/discoveryProvider/{ejbcaInstanceName}/ejbcaVersion");
        attributeCallback.setCallbackMethod("GET");
        attributeCallback.setMappings(mappings);

        attribute.setAttributeCallback(attributeCallback);

        return attribute;
    }
}

package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.interfaces.connector.AttributesController;
import com.czertainly.api.model.common.*;
import com.czertainly.ca.connector.ejbca.service.AttributeService;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AttributeServiceImpl implements AttributeService {
	private static final Logger logger = LoggerFactory.getLogger(AttributesController.class);

	@Override
	public List<AttributeDefinition> getAttributes(String kind) {
		logger.info("Getting the attributes for {}", kind);
		List<AttributeDefinition> attrs = new ArrayList<>();

        AttributeDefinition url = new AttributeDefinition();
        url.setUuid("87e968ca-9404-4128-8b58-3ab5db2ba06e");
        url.setName("url");
        url.setLabel("EJBCA WS URL");
        url.setDescription("URL of EJBCA web services");
        url.setType(BaseAttributeDefinitionTypes.STRING);
        url.setRequired(true);
        url.setReadOnly(false);
        url.setVisible(true);
        attrs.add(url);
        
        AttributeDefinition credential = new AttributeDefinition();
        credential.setUuid("9379ca2c-aa51-42c8-8afd-2a2d16c99c57");
        credential.setName("credential");
        credential.setLabel("Credential");
        credential.setDescription("SoftKeyStore Credential representing EJBCA administrator for the communication");
        credential.setType(BaseAttributeDefinitionTypes.CREDENTIAL);
        credential.setRequired(true);
        credential.setReadOnly(false);
        credential.setVisible(true);
        
        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(
                "credentialKind",
                AttributeValueTarget.PATH_VARIABLE,
                "SoftKeyStore"));

        AttributeCallback listCredentialCallback = new AttributeCallback();
        listCredentialCallback.setCallbackContext("core/getCredentials");
        listCredentialCallback.setCallbackMethod("GET");
        listCredentialCallback.setMappings(mappings);
        credential.setAttributeCallback(listCredentialCallback);
        
        attrs.add(credential);

        return attrs;
	}

	@Override
	public boolean validateAttributes(String kind, List<RequestAttributeDto> attributes) {
        if (attributes == null) {
            return false;
        }

		AttributeDefinitionUtils.validateAttributes(getAttributes(kind), attributes);
        return true;
	}
}

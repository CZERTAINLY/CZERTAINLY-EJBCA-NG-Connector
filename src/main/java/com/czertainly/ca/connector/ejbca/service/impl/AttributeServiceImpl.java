package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.interfaces.connector.AttributesController;
import com.czertainly.api.model.client.attribute.RequestAttributeDto;
import com.czertainly.api.model.common.attribute.v2.AttributeProperties;
import com.czertainly.api.model.common.attribute.v2.AttributeType;
import com.czertainly.api.model.common.attribute.v2.BaseAttribute;
import com.czertainly.api.model.common.attribute.v2.DataAttribute;
import com.czertainly.api.model.common.attribute.v2.callback.AttributeCallback;
import com.czertainly.api.model.common.attribute.v2.callback.AttributeCallbackMapping;
import com.czertainly.api.model.common.attribute.v2.callback.AttributeValueTarget;
import com.czertainly.api.model.common.attribute.v2.content.AttributeContentType;
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
	public List<BaseAttribute> getAttributes(String kind) {
		logger.info("Getting the attributes for {}", kind);
		List<BaseAttribute> attrs = new ArrayList<>();

        DataAttribute url = new DataAttribute();
        url.setUuid("87e968ca-9404-4128-8b58-3ab5db2ba06e");
        url.setName("url");
        url.setDescription("URL of EJBCA web services");
        url.setType(AttributeType.DATA);
        url.setContentType(AttributeContentType.STRING);
        AttributeProperties urlProperties = new AttributeProperties();
        urlProperties.setLabel("EJBCA WS URL");
        urlProperties.setRequired(true);
        urlProperties.setReadOnly(false);
        urlProperties.setVisible(true);
        urlProperties.setList(false);
        urlProperties.setMulti(false);
        url.setProperties(urlProperties);
        attrs.add(url);

        DataAttribute credential = new DataAttribute();
        credential.setUuid("9379ca2c-aa51-42c8-8afd-2a2d16c99c57");
        credential.setName("credential");
        credential.setDescription("SoftKeyStore Credential representing EJBCA administrator for the communication");
        credential.setType(AttributeType.DATA);
        credential.setContentType(AttributeContentType.CREDENTIAL);
        AttributeProperties credentialProperties = new AttributeProperties();
        credentialProperties.setLabel("Credential");
        credentialProperties.setRequired(true);
        credentialProperties.setReadOnly(false);
        credentialProperties.setVisible(true);
        credentialProperties.setList(true);
        credentialProperties.setMulti(false);
        credential.setProperties(credentialProperties);

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

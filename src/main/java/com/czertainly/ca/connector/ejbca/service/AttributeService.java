package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.model.common.attribute.AttributeDefinition;
import com.czertainly.api.model.common.attribute.RequestAttributeDto;

import java.util.List;

public interface AttributeService {
	List<AttributeDefinition> getAttributes(String kind);
	
	boolean validateAttributes(String kind, List<RequestAttributeDto> attributes);
}

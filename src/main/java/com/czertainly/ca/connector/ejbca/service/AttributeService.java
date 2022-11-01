package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.model.client.attribute.RequestAttributeDto;
import com.czertainly.api.model.common.attribute.BaseAttribute;

import java.util.List;

public interface AttributeService {
    List<BaseAttribute> getAttributes(String kind);

    boolean validateAttributes(String kind, List<RequestAttributeDto> attributes);
}

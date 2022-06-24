package com.czertainly.ca.connector.ejbca.util;

import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.core.util.AttributeDefinitionUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class LocalAttributeUtil {

    private static final ObjectMapper ATTRIBUTES_OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static NameAndIdDto getNameAndIdData(String name, List<?> attributes) {
        Object content = AttributeDefinitionUtils.getAttributeContent(name, attributes);

        if (!(content instanceof Map)) {
            throw new IllegalArgumentException("Could not get NameAndId value. Attribute has wrong value: " + content);
        }

        Map<?, ?> valueMap = (Map<?, ?>) content;
        if (valueMap.containsKey("value") && valueMap.containsKey("data")) {
            Object nameAndIdData = valueMap.get("data");
            return ATTRIBUTES_OBJECT_MAPPER.convertValue(nameAndIdData, NameAndIdDto.class);
        } else {
            throw new IllegalArgumentException("Could not get NameAndId value. Attribute has wrong value: " + content);
        }
    }
}

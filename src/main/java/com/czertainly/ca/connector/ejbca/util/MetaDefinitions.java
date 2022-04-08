package com.czertainly.ca.connector.ejbca.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaDefinitions {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	public static String serialize(Map<String, Object> metaData) {
        try {
            return OBJECT_MAPPER.writeValueAsString(metaData);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Map<String, Object> deserialize(String metaJson) {
    	if(metaJson == null || metaJson.isEmpty()) {
    		return new HashMap<>();
    	}
        try {
            return OBJECT_MAPPER.readValue(metaJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static String serializeArrayString(List<String> metaData) {
        try {
            return OBJECT_MAPPER.writeValueAsString(metaData);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<String> deserializeArrayString(String metaJson) {
        try {
            return OBJECT_MAPPER.readValue(metaJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
	
}

package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.model.client.attribute.RequestAttributeDto;
import com.czertainly.api.model.common.attribute.v2.BaseAttribute;
import com.czertainly.api.model.common.attribute.v2.content.BaseAttributeContent;

import java.util.List;

public interface DiscoveryAttributeService {

    List<BaseAttribute> getAttributes(String kind);

    boolean validateAttributes(String kind, List<RequestAttributeDto> attributes);

    List<BaseAttribute> getInstanceAndKindAttributes(
            String kind,
            List<BaseAttributeContent> eeProfilesContent,
            List<BaseAttributeContent> casContent,
            List<BaseAttributeContent> urlContent
    );
}

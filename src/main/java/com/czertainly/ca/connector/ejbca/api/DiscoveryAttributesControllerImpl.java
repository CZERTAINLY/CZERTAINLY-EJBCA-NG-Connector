package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.interfaces.connector.AttributesController;
import com.czertainly.api.model.common.AttributeDefinition;
import com.czertainly.api.model.common.RequestAttributeDto;
import com.czertainly.ca.connector.ejbca.service.DiscoveryAttributeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/v1/discoveryProvider/{kind}/attributes")
public class DiscoveryAttributesControllerImpl implements AttributesController {

    @Autowired
    public void setDiscoveryAttributeService(DiscoveryAttributeService discoveryAttributeService) {
        this.discoveryAttributeService = discoveryAttributeService;
    }

    private DiscoveryAttributeService discoveryAttributeService;

    @Override
    public List<AttributeDefinition> listAttributeDefinitions(String kind) {
        return discoveryAttributeService.getAttributes(kind);
    }

    @Override
    public void validateAttributes(String kind, List<RequestAttributeDto> attributes) throws ValidationException {
        discoveryAttributeService.validateAttributes(kind, attributes);
    }
}

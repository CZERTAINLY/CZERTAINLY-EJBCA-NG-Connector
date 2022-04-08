package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.interfaces.connector.AttributesController;
import com.czertainly.api.model.common.AttributeDefinition;
import com.czertainly.api.model.common.BaseAttributeDefinitionTypes;
import com.czertainly.api.model.common.RequestAttributeDto;
import com.czertainly.ca.connector.ejbca.service.AttributeService;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/authorityProvider/{kind}/attributes")
public class AttributesControllerImpl implements AttributesController {

    @Autowired
    private AttributeService attributeService;

    @Override
    public List<AttributeDefinition> listAttributeDefinitions(@PathVariable String kind) {
        return attributeService.getAttributes(kind);
    }

    @Override
    public void validateAttributes(@PathVariable String kind, @RequestBody List<RequestAttributeDto> attributes) throws ValidationException {
        attributeService.validateAttributes(kind, attributes);
    }
}

package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.connector.discovery.DiscoveryDataRequestDto;
import com.czertainly.api.model.connector.discovery.DiscoveryProviderDto;
import com.czertainly.api.model.connector.discovery.DiscoveryRequestDto;
import com.czertainly.api.model.core.discovery.DiscoveryStatus;
import com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl;
import com.czertainly.ca.connector.ejbca.dao.entity.DiscoveryHistory;
import com.czertainly.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.czertainly.ca.connector.ejbca.service.DiscoveryService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.core.util.AttributeDefinitionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DiscoveryServiceImpl implements DiscoveryService {

    @Autowired
    public void setEjbcaService(EjbcaService ejbcaService) {
        this.ejbcaService = ejbcaService;
    }

    private EjbcaService ejbcaService;

    @Override
    public void discoverCertificate(DiscoveryRequestDto request, DiscoveryHistory history) throws IOException, NotFoundException {
        try {
            discoverCertificatesInternal(request, history);
        } catch (Exception e) {
            history.setStatus(DiscoveryStatus.FAILED);
            throw e;
        }
    }

    @Override
    public DiscoveryProviderDto getProviderDtoData(DiscoveryDataRequestDto request, DiscoveryHistory history) {
        return null;
    }

    private void discoverCertificatesInternal(DiscoveryRequestDto request, DiscoveryHistory history) throws NotFoundException {
        final AuthorityInstanceNameAndUuidDto instance = AttributeDefinitionUtils.getJsonAttributeContentData(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_INSTANCE, request.getAttributes(), AuthorityInstanceNameAndUuidDto.class);
        ejbcaService.searchCertificates(instance.getUuid(), "https://lab01.3key.company:8453/ejbca/ejbca-rest-api/v2/certificate/search");
    }
}

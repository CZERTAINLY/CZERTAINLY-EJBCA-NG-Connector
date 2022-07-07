package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.connector.discovery.DiscoveryDataRequestDto;
import com.czertainly.api.model.connector.discovery.DiscoveryProviderDto;
import com.czertainly.api.model.connector.discovery.DiscoveryRequestDto;
import com.czertainly.ca.connector.ejbca.dao.entity.DiscoveryHistory;

import java.io.IOException;

public interface DiscoveryService {

    void discoverCertificate(DiscoveryRequestDto request, DiscoveryHistory history) throws IOException, NotFoundException;

    DiscoveryProviderDto getProviderDtoData(DiscoveryDataRequestDto request, DiscoveryHistory history);
}

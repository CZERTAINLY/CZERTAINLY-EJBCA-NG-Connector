package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.connector.discovery.DiscoveryRequestDto;
import com.czertainly.ca.connector.ejbca.dao.entity.DiscoveryHistory;

public interface DiscoveryHistoryService {

    DiscoveryHistory addHistory(DiscoveryRequestDto request);

    DiscoveryHistory getHistoryById(Long id) throws NotFoundException;

    DiscoveryHistory getHistoryByUuid(String uuid) throws NotFoundException;

    void setHistory(DiscoveryHistory history);
}
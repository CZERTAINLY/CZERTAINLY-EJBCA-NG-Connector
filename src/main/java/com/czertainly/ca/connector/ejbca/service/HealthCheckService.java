package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.model.common.HealthDto;

public interface HealthCheckService {

    HealthDto checkHealth();
}

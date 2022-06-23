package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.connector.authority.AuthorityProviderInstanceDto;
import com.czertainly.api.model.connector.authority.AuthorityProviderInstanceRequestDto;
import com.czertainly.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.czertainly.ca.connector.ejbca.ws.EjbcaWS;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

public interface AuthorityInstanceService {

    List<AuthorityProviderInstanceDto> listAuthorityInstances();
    AuthorityProviderInstanceDto getAuthorityInstance(String uuid) throws NotFoundException;
    AuthorityProviderInstanceDto createAuthorityInstance(AuthorityProviderInstanceRequestDto request) throws AlreadyExistException;
    AuthorityProviderInstanceDto updateAuthorityInstance(String uuid, AuthorityProviderInstanceRequestDto request) throws NotFoundException;
    void removeAuthorityInstance(String uuid) throws NotFoundException;

    EjbcaWS getConnection(String uuid) throws NotFoundException;

    EjbcaWS getConnection(AuthorityInstance instance);

    WebClient getRestApiConnection(String uuid) throws NotFoundException;

    WebClient getRestApiConnection(AuthorityInstance instance);
}

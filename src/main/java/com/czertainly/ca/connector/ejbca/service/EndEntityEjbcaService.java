package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.core.authority.AddEndEntityRequestDto;
import com.czertainly.api.model.core.authority.EditEndEntityRequestDto;
import com.czertainly.api.model.core.authority.EndEntityDto;
import com.czertainly.ca.connector.ejbca.ws.EjbcaWS;
import com.czertainly.ca.connector.ejbca.ws.UserDataVOWS;

import java.util.List;

public interface EndEntityEjbcaService {

    List<EndEntityDto> listEntities(String uuid, String endEntityProfileName) throws NotFoundException;

    EndEntityDto getEndEntity(String uuid, String endEntityProfileName, String endEntityName) throws NotFoundException;

    void createEndEntity(String uuid, String endEntityProfileName, AddEndEntityRequestDto request) throws NotFoundException, AlreadyExistException;

    void updateEndEntity(String uuid, String endEntityProfileName, String endEntityName, EditEndEntityRequestDto request) throws NotFoundException;

    void revokeAndDeleteEndEntity(String uuid, String endEntityProfileName, String username) throws NotFoundException;

    void resetPassword(String uuid, String endEntityProfileName, String username) throws NotFoundException;

    UserDataVOWS getUser(EjbcaWS ejbcaWS, String username) throws NotFoundException;
}

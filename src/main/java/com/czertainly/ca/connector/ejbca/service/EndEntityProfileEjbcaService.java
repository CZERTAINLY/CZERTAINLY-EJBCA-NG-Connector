package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.common.NameAndIdDto;

import java.util.List;

public interface EndEntityProfileEjbcaService {

    List<NameAndIdDto> listEndEntityProfiles(String uuid) throws NotFoundException;

    List<NameAndIdDto> listCertificateProfiles(String uuid, int endEntityProfileId) throws NotFoundException;

    List<NameAndIdDto> listCAsInProfile(String uuid, int endEntityProfileId) throws NotFoundException;
}

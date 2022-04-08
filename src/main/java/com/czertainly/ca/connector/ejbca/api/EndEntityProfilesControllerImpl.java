package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.interfaces.connector.EndEntityProfilesController;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EndEntityProfilesControllerImpl implements EndEntityProfilesController {

    @Autowired
    private EndEntityProfileEjbcaService endEntityProfileEjbcaService;

    @Override
    public List<NameAndIdDto> listEntityProfiles(@PathVariable String uuid) throws NotFoundException {
        return endEntityProfileEjbcaService.listEndEntityProfiles(uuid);
    }

    @Override
    public List<NameAndIdDto> listCertificateProfiles(@PathVariable String uuid, @PathVariable Integer endEntityProfileId) throws NotFoundException {
        return endEntityProfileEjbcaService.listCertificateProfiles(uuid, endEntityProfileId);
    }

    @Override
    public List<NameAndIdDto> listCAsInProfile(@PathVariable String uuid, @PathVariable Integer endEntityProfileId) throws NotFoundException {
        return endEntityProfileEjbcaService.listCAsInProfile(uuid, endEntityProfileId);
    }
}

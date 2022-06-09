package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationError;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.czertainly.ca.connector.ejbca.dto.EjbcaVersionResponseDto;
import com.czertainly.ca.connector.ejbca.dto.SelectedEjbcaInstanceDto;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import com.czertainly.ca.connector.ejbca.util.EjbcaVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/discoveryProvider")
public class DiscoverySupportController {

    @Autowired
    public void setEjbcaService(EjbcaService ejbcaService) {
        this.ejbcaService = ejbcaService;
    }
    @Autowired
    public void setEndEntityProfileEjbcaService(EndEntityProfileEjbcaService endEntityProfileEjbcaService) {
        this.endEntityProfileEjbcaService = endEntityProfileEjbcaService;
    }

    private EjbcaService ejbcaService;
    private EndEntityProfileEjbcaService endEntityProfileEjbcaService;

    @RequestMapping(
            path = "/{ejbcaInstanceName}/ejbcaVersion",
            method = RequestMethod.GET,
            produces = "application/json"
    )
    public EjbcaVersionResponseDto getEjbcaVersion(@PathVariable String ejbcaInstanceName) throws NotFoundException, AlreadyExistException {
        EjbcaVersion ejbcaVersion = ejbcaService.getEjbcaVersion(ejbcaInstanceName);

        if (ejbcaVersion.getMajorVersion() < 9) {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(ValidationError.create("EJBCA version missing", ""));
            throw new ValidationException("EJBCA version is not supported", errors);
        }

        //List<String> list = new ArrayList<>();
        //list.add(ejbcaVersion.toString());
        //return ejbcaVersion.toString();

        return new EjbcaVersionResponseDto(ejbcaVersion.toString());
    }

    @RequestMapping(
            path = "/{ejbcaInstanceUuid}/listEndEntityProfiles",
            method = RequestMethod.GET,
            produces = "application/json"
    )
    public List<NameAndIdDto> listEndEntityProfiles(@PathVariable String ejbcaInstanceUuid) throws NotFoundException {
        checkEjbcaVersion(ejbcaInstanceUuid);
        return endEntityProfileEjbcaService.listEndEntityProfiles(ejbcaInstanceUuid);
    }

    @RequestMapping(
            path = "/listEndEntityProfiles",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json"
    )
    public List<NameAndIdDto> listEndEntityProfiles(@RequestBody SelectedEjbcaInstanceDto selectedEjbcaInstance) throws NotFoundException {
        checkEjbcaVersion(selectedEjbcaInstance.getSelectedEjbcaInstance().getUuid());
        return endEntityProfileEjbcaService.listEndEntityProfiles(selectedEjbcaInstance.getSelectedEjbcaInstance().getUuid());
    }

    private void checkEjbcaVersion(String ejbcaInstanceName) throws NotFoundException {
        EjbcaVersion ejbcaVersion = ejbcaService.getEjbcaVersion(ejbcaInstanceName);

        boolean supported = false;
        if ((ejbcaVersion.getTechVersion() >= 7 && ejbcaVersion.getMajorVersion() >= 8)) {
            supported = true;
        }

        if (!supported) {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(ValidationError.create("Unsupported version " + ejbcaVersion, ""));
            throw new ValidationException("EJBCA version is not supported", errors);
        }
    }
}

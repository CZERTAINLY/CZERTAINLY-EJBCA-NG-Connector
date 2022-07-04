package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationError;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.attribute.content.BaseAttributeContent;
import com.czertainly.api.model.common.attribute.content.JsonAttributeContent;
import com.czertainly.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.czertainly.ca.connector.ejbca.dto.EjbcaVersionResponseDto;
import com.czertainly.ca.connector.ejbca.dto.SelectedEjbcaInstanceDto;
import com.czertainly.ca.connector.ejbca.service.AuthorityInstanceService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import com.czertainly.ca.connector.ejbca.util.EjbcaVersion;
import com.czertainly.ca.connector.ejbca.util.LocalAttributeUtil;
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
    @Autowired
    public void setAuthorityInstanceService(AuthorityInstanceService authorityInstanceService) {
        this.authorityInstanceService = authorityInstanceService;
    }

    private EjbcaService ejbcaService;
    private EndEntityProfileEjbcaService endEntityProfileEjbcaService;
    private AuthorityInstanceService authorityInstanceService;

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
    public List<JsonAttributeContent> listEndEntityProfiles(@PathVariable String ejbcaInstanceUuid) throws NotFoundException {
        checkEjbcaVersion(ejbcaInstanceUuid);

        List<NameAndIdDto> endEntityProfiles = endEntityProfileEjbcaService.listEndEntityProfiles(ejbcaInstanceUuid);
        List<JsonAttributeContent> contentList = new ArrayList<>();
        for (NameAndIdDto endEntityProfile : endEntityProfiles) {
            JsonAttributeContent content = new JsonAttributeContent(endEntityProfile.getName(), endEntityProfile);
            contentList.add(content);
        }
        return contentList;
    }

    @RequestMapping(
            path = "/listEndEntityProfiles",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json"
    )
    public List<JsonAttributeContent> listEndEntityProfiles(@RequestBody SelectedEjbcaInstanceDto selectedEjbcaInstance) throws NotFoundException {
        checkEjbcaVersion(selectedEjbcaInstance.getSelectedEjbcaInstance().getUuid());

        List<NameAndIdDto> endEntityProfiles = endEntityProfileEjbcaService.listEndEntityProfiles(selectedEjbcaInstance.getSelectedEjbcaInstance().getUuid());
        List<JsonAttributeContent> contentList = new ArrayList<>();
        for (NameAndIdDto endEntityProfile : endEntityProfiles) {
            JsonAttributeContent content = new JsonAttributeContent(endEntityProfile.getName(), endEntityProfile);
            contentList.add(content);
        }
        return contentList;
    }

    @RequestMapping(
            path = "/listCas",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json"
    )
    public List<JsonAttributeContent> listCas(@RequestBody SelectedEjbcaInstanceDto selectedEjbcaInstance) throws NotFoundException {
        checkEjbcaVersion(selectedEjbcaInstance.getSelectedEjbcaInstance().getUuid());

        List<NameAndIdDto> cas = ejbcaService.getAvailableCas(selectedEjbcaInstance.getSelectedEjbcaInstance().getUuid());
        return LocalAttributeUtil.convertFromNameAndId(cas);
    }

    @RequestMapping(
            path = "/ejbcaRestApi",
            method = RequestMethod.POST,
            consumes = "application/json",
            produces = "application/json"
    )
    public BaseAttributeContent<String> ejbcaRestApi(@RequestBody SelectedEjbcaInstanceDto selectedEjbcaInstance) throws NotFoundException {
        checkEjbcaVersion(selectedEjbcaInstance.getSelectedEjbcaInstance().getUuid());

        String url = authorityInstanceService.getRestApiUrl(selectedEjbcaInstance.getSelectedEjbcaInstance().getUuid());
        return new BaseAttributeContent<>(url);
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

package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationError;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.attribute.v2.BaseAttribute;
import com.czertainly.api.model.common.attribute.v2.content.BaseAttributeContent;
import com.czertainly.api.model.common.attribute.v2.content.ObjectAttributeContent;
import com.czertainly.api.model.common.attribute.v2.content.StringAttributeContent;
import com.czertainly.ca.connector.ejbca.dto.EjbcaVersionResponseDto;
import com.czertainly.ca.connector.ejbca.service.AuthorityInstanceService;
import com.czertainly.ca.connector.ejbca.service.DiscoveryAttributeService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import com.czertainly.ca.connector.ejbca.util.EjbcaVersion;
import com.czertainly.ca.connector.ejbca.util.LocalAttributeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/discoveryProvider")
public class DiscoverySupportController {

    private EjbcaService ejbcaService;
    private EndEntityProfileEjbcaService endEntityProfileEjbcaService;
    private AuthorityInstanceService authorityInstanceService;
    private DiscoveryAttributeService discoveryAttributeService;

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

    @Autowired
    public void setDiscoveryAttributeService(DiscoveryAttributeService discoveryAttributeService) {
        this.discoveryAttributeService = discoveryAttributeService;
    }

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
    public List<ObjectAttributeContent> listEndEntityProfiles(@PathVariable String ejbcaInstanceUuid) throws NotFoundException {
        checkEjbcaVersion(ejbcaInstanceUuid);

        List<NameAndIdDto> endEntityProfiles = endEntityProfileEjbcaService.listEndEntityProfiles(ejbcaInstanceUuid);
        List<ObjectAttributeContent> contentList = new ArrayList<>();
        for (NameAndIdDto endEntityProfile : endEntityProfiles) {
            ObjectAttributeContent content = new ObjectAttributeContent(endEntityProfile.getName(), endEntityProfile);
            contentList.add(content);
        }
        return contentList;
    }

    @RequestMapping(
            path = "/{ejbcaInstanceUuid}/listCas",
            method = RequestMethod.GET,
            produces = "application/json"
    )
    public List<ObjectAttributeContent> listCas(@PathVariable String ejbcaInstanceUuid) throws NotFoundException {
        checkEjbcaVersion(ejbcaInstanceUuid);

        List<NameAndIdDto> cas = ejbcaService.getAvailableCas(ejbcaInstanceUuid);
        return LocalAttributeUtil.convertFromNameAndId(cas);
    }

    @RequestMapping(
            path = "/{ejbcaInstanceUuid}/ejbcaRestApi",
            method = RequestMethod.GET,
            produces = "application/json"
    )
    public BaseAttributeContent<String> ejbcaRestApi(@PathVariable String ejbcaInstanceUuid) throws NotFoundException {
        checkEjbcaVersion(ejbcaInstanceUuid);

        String url = authorityInstanceService.getRestApiUrl(ejbcaInstanceUuid);
        return new BaseAttributeContent<>(url);
    }

    @RequestMapping(
            path = "/{ejbcaInstanceUuid}/{kind}/configuration",
            method = RequestMethod.GET,
            produces = "application/json"
    )
    public List<BaseAttribute> configuration(
            @PathVariable String ejbcaInstanceUuid, @PathVariable String kind) throws NotFoundException {
        checkEjbcaVersion(ejbcaInstanceUuid);

        List<NameAndIdDto> endEntityProfiles = endEntityProfileEjbcaService.listEndEntityProfiles(ejbcaInstanceUuid);
        List<BaseAttributeContent> eeProfilesContent = new ArrayList<>();
        for (NameAndIdDto endEntityProfile : endEntityProfiles) {
            ObjectAttributeContent content = new ObjectAttributeContent(endEntityProfile.getName(), endEntityProfile);
            eeProfilesContent.add(content);
        }

        List<NameAndIdDto> cas = ejbcaService.getAvailableCas(ejbcaInstanceUuid);
        List<BaseAttributeContent> casContent = LocalAttributeUtil.convertFromNameAndIdToBase(cas);

        String url = authorityInstanceService.getRestApiUrl(ejbcaInstanceUuid);
        List<BaseAttributeContent> urlContent = new ArrayList<>();
        StringAttributeContent urlAttributeContent = new StringAttributeContent(url);
        urlContent.add(urlAttributeContent);

        return discoveryAttributeService.getInstanceAndKindAttributes(kind, eeProfilesContent, casContent, urlContent);
    }

    private void checkEjbcaVersion(String ejbcaInstanceName) throws NotFoundException {
        EjbcaVersion ejbcaVersion = ejbcaService.getEjbcaVersion(ejbcaInstanceName);

        boolean supported = ejbcaVersion.getTechVersion() >= 7 && ejbcaVersion.getMajorVersion() >= 8;

        if (!supported) {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(ValidationError.create("Unsupported version " + ejbcaVersion, ""));
            throw new ValidationException("EJBCA version is not supported", errors);
        }
    }
}

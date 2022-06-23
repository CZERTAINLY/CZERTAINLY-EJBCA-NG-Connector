package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.attribute.content.JsonAttributeContent;
import com.czertainly.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/authorityProvider/authorities/{uuid}/endEntityProfiles")
public class EndEntityProfilesControllerImpl {

    @Autowired
    private EndEntityProfileEjbcaService endEntityProfileEjbcaService;


    @RequestMapping(method = RequestMethod.GET, produces = {"application/json"})
    public List<JsonAttributeContent> listEntityProfiles(@PathVariable String uuid) throws NotFoundException {
        List<JsonAttributeContent> listJsonContent = new ArrayList<>();
        List<NameAndIdDto> dataList = endEntityProfileEjbcaService.listEndEntityProfiles(uuid);
        for (NameAndIdDto data : dataList) {
            JsonAttributeContent content = new JsonAttributeContent();
            content.setValue(data.getName());
            content.setData(data);
            listJsonContent.add(content);
        }
        return listJsonContent;
    }

    @RequestMapping(path = "/{endEntityProfileId}/certificateprofiles", method = RequestMethod.GET, produces = {"application/json"})
    public List<JsonAttributeContent> listCertificateProfiles(@PathVariable String uuid, @PathVariable Integer endEntityProfileId) throws NotFoundException {
        List<JsonAttributeContent> listJsonContent = new ArrayList<>();
        List<NameAndIdDto> dataList = endEntityProfileEjbcaService.listCertificateProfiles(uuid, endEntityProfileId);
        for (NameAndIdDto data : dataList) {
            JsonAttributeContent content = new JsonAttributeContent();
            content.setValue(data.getName());
            content.setData(data);
            listJsonContent.add(content);
        }
        return listJsonContent;
    }

    @RequestMapping(path = "/{endEntityProfileId}/cas", method = RequestMethod.GET, produces = {"application/json"})
    public List<JsonAttributeContent> listCAsInProfile(@PathVariable String uuid, @PathVariable Integer endEntityProfileId) throws NotFoundException {
        List<JsonAttributeContent> listJsonContent = new ArrayList<>();
        List<NameAndIdDto> dataList = endEntityProfileEjbcaService.listCAsInProfile(uuid, endEntityProfileId);
        for (NameAndIdDto data : dataList) {
            JsonAttributeContent content = new JsonAttributeContent();
            content.setValue(data.getName());
            content.setData(data);
            listJsonContent.add(content);
        }
        return listJsonContent;
    }
}

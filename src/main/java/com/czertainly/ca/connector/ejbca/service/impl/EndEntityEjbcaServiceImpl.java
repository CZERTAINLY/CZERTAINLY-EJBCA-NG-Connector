package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.ResponseAttributeDto;
import com.czertainly.api.model.core.authority.*;
import com.czertainly.ca.connector.ejbca.ws.*;
import com.czertainly.ca.connector.ejbca.service.AuthorityInstanceService;
import com.czertainly.ca.connector.ejbca.service.EndEntityEjbcaService;
import com.czertainly.ca.connector.ejbca.util.EjbcaUtils;
import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.*;

@Service
@Transactional
public class EndEntityEjbcaServiceImpl implements EndEntityEjbcaService {

    @Autowired
    private AuthorityInstanceService authorityInstanceService;

    @Override
    public List<EndEntityDto> listEntities(String uuid, String endEntityProfileName) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        List<UserDataVOWS> users = listUsers(ejbcaWS, endEntityProfileName);
        return users.stream().map(EjbcaUtils::mapToUserDetailDTO).collect(Collectors.toList());
    }

    @Override
    public EndEntityDto getEndEntity(String uuid, String endEntityProfileName, String endEntityName) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        UserDataVOWS user = getUser(ejbcaWS, endEntityName);
        if (user == null) {
            throw new NotFoundException("EndEntity", endEntityName);
        }
        return EjbcaUtils.mapToUserDetailDTO(user);
    }

    @Override
    public void createEndEntity(String uuid, String endEntityProfileName, AddEndEntityRequestDto request) throws NotFoundException, AlreadyExistException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        if (getUser(ejbcaWS, request.getUsername()) != null) {
            throw new AlreadyExistException("EndEntity", request.getUsername());
        }

        UserDataVOWS user = new UserDataVOWS();
        prepareEndEntity(user, request, request.getUsername());

        try {
            ejbcaWS.editUser(user);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA", user.getCaName());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void updateEndEntity(String uuid, String endEntityProfileName, String endEntityName, EditEndEntityRequestDto request) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        UserDataVOWS user = getUser(ejbcaWS, endEntityName);
        if (user == null) {
            throw new NotFoundException("EndEntity", endEntityName);
        }

        prepareEndEntity(user, request, endEntityName);

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus().getCode());
        }
        try {
            ejbcaWS.editUser(user);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA", user.getCaName());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void revokeAndDeleteEndEntity(String uuid, String endEntityProfileName, String endEntityName) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        UserDataVOWS user = getUser(ejbcaWS, endEntityName);
        if (user == null) {
            throw new NotFoundException("EndEntity", endEntityName);
        }

        try {
            ejbcaWS.revokeUser(endEntityName, RevocationReason.UNSPECIFIED.getCode(), true);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA for EndEntity", endEntityName);
        } catch (NotFoundException_Exception e) {
            throw new NotFoundException("EndEntity", endEntityName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void resetPassword(String uuid, String raProfileName, String endEntityName) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        UserDataVOWS user = getUser(ejbcaWS, endEntityName);
        if (user == null) {
            throw new NotFoundException("EndEntity", endEntityName);
        }

        user.setPassword("NEWPASSWORD"); // constant for resetting passwords

        try {
            ejbcaWS.editUser(user);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA for EndEntity", endEntityName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public UserDataVOWS getUser(EjbcaWS ejbcaWS, String username) throws NotFoundException {
        UserMatch userMatch = EjbcaUtils.prepareUsernameMatch(username);

        try {
            List<UserDataVOWS> users = ejbcaWS.findUser(userMatch);
            return (users != null && !users.isEmpty()) ? users.get(0) : null;
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (EndEntityProfileNotFoundException_Exception e) {
            throw new NotFoundException("EndEntity", username);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<UserDataVOWS> listUsers(EjbcaWS ejbcaWS, String endEntityProfileName) throws NotFoundException {
        UserMatch userMatch = EjbcaUtils.prepareEndEntityProfileMatch(endEntityProfileName);

        try {
            return ejbcaWS.findUser(userMatch);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (EndEntityProfileNotFoundException_Exception e) {
            throw new NotFoundException("EndEntityProfile", endEntityProfileName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void prepareEndEntity(UserDataVOWS user, BaseEndEntityRequestDto request, String username) {
        List<ResponseAttributeDto> raProfileAttrs = request.getRaProfile().getAttributes();

        //String tokenType = AttributeDefinitionUtils.getAttributeValue(ATTRIBUTE_TOKEN_TYPE, raProfileAttrs);
        //user.setTokenType(tokenType);

        NameAndIdDto endEntityProfile = AttributeDefinitionUtils.getAttributeValue(ATTRIBUTE_END_ENTITY_PROFILE, raProfileAttrs);
        user.setEndEntityProfileName(endEntityProfile.getName());

        NameAndIdDto certificateProfile = AttributeDefinitionUtils.getAttributeValue(ATTRIBUTE_CERTIFICATE_PROFILE, raProfileAttrs);
        user.setCertificateProfileName(certificateProfile.getName());

        NameAndIdDto ca = AttributeDefinitionUtils.getAttributeValue(ATTRIBUTE_CERTIFICATION_AUTHORITY, raProfileAttrs);
        user.setCaName(ca.getName());

        boolean sendNotifications = AttributeDefinitionUtils.getAttributeValue(ATTRIBUTE_SEND_NOTIFICATIONS, raProfileAttrs);
        user.setSendNotification(sendNotifications);

        boolean keyRecoverable = AttributeDefinitionUtils.getAttributeValue(ATTRIBUTE_KEY_RECOVERABLE, raProfileAttrs);
        user.setKeyRecoverable(keyRecoverable);


        if (StringUtils.isNotBlank(username)) {
            user.setUsername(username);
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.isNotBlank(request.getPassword())) {
            user.setPassword(request.getPassword());
        }
        if (StringUtils.isNotBlank(request.getSubjectAltName())) {
            user.setSubjectAltName(request.getSubjectAltName());
        }
        if (StringUtils.isNotBlank(request.getSubjectDN())) {
            user.setSubjectDN(request.getSubjectDN());
        }

        // include extended information of end entity if available
        if (request.getExtensionData() != null && !request.getExtensionData().isEmpty()) {
            List<ExtendedInformationWS> ei = new ArrayList<>();
            for (EndEntityExtendedInfoDto extendedInformation : request.getExtensionData()) {
                ExtendedInformationWS eiWs = new ExtendedInformationWS();
                eiWs.setName(extendedInformation.getName());
                eiWs.setValue(extendedInformation.getValue());
                ei.add(eiWs);
            }
            user.getExtendedInformation().addAll(ei);
        }
    }
}

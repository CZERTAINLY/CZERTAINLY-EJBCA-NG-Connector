package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.attribute.RequestAttributeDto;
import com.czertainly.api.model.common.attribute.content.BaseAttributeContent;
import com.czertainly.api.model.connector.authority.AuthorityProviderInstanceDto;
import com.czertainly.api.model.connector.v2.CertificateDataResponseDto;
import com.czertainly.ca.connector.ejbca.api.CertificateControllerImpl;
import com.czertainly.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.Pagination;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificateSortRestRequest;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificatesRestRequestV2;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import com.czertainly.ca.connector.ejbca.rest.EjbcaRestApiClient;
import com.czertainly.ca.connector.ejbca.service.AuthorityInstanceService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.util.EjbcaUtils;
import com.czertainly.ca.connector.ejbca.util.EjbcaVersion;
import com.czertainly.ca.connector.ejbca.ws.*;
import com.czertainly.core.util.AttributeDefinitionUtils;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.*;
import static com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_KEY_RECOVERABLE;

@Service
@Transactional
public class EjbcaServiceImpl implements EjbcaService {

    @Autowired
    public void setAuthorityInstanceService(AuthorityInstanceService authorityInstanceService) {
        this.authorityInstanceService = authorityInstanceService;
    }

    private AuthorityInstanceService authorityInstanceService;

    @Override
    public void createEndEntity(String authorityUuid, String username, String password, String subjectDn, List<RequestAttributeDto> raProfileAttributes, List<RequestAttributeDto> issueAttributes) throws NotFoundException, AlreadyExistException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityUuid);

        if (getUser(ejbcaWS, username) != null) {
            throw new AlreadyExistException("End Entity " + username + " already exists");
        }

        UserDataVOWS user = new UserDataVOWS();
        user.setUsername(username);
        user.setPassword(password);
        user.setSubjectDN(subjectDn);
        prepareEndEntity(user, raProfileAttributes, issueAttributes);

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
    public void createEndEntity(String authorityUuid, String username, String password, String subjectDn, List<RequestAttributeDto> raProfileAttributes, Map<String, Object> metadata) throws NotFoundException, AlreadyExistException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityUuid);

        if (getUser(ejbcaWS, username) != null) {
            throw new AlreadyExistException("End Entity " + username + " already exists");
        }

        UserDataVOWS user = new UserDataVOWS();
        user.setUsername(username);
        user.setPassword(password);
        user.setSubjectDN(subjectDn);
        prepareEndEntity(user, raProfileAttributes, metadata);

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

    public void renewEndEntity(String authorityUuid, String username, String password) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityUuid);

        UserDataVOWS user = getUser(ejbcaWS, username);
        if (user == null) {
            throw new NotFoundException("EndEntity", username);
        }

        user.setPassword(password);
        user.setStatus(EndEntityStatus.NEW.getCode());

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
    public CertificateDataResponseDto issueCertificate(String authorityUuid, String username, String password, String pkcs10) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityUuid);

        try {
            CertificateResponse certificateResponse = ejbcaWS.pkcs10Request(
                    username,
                    password,
                    pkcs10,
                    null,
                    "PKCS7WITHCHAIN"); // constant for PKCS7 with chain
            CertificateDataResponseDto response = new CertificateDataResponseDto();
            response.setCertificateData(new String(certificateResponse.getData(), StandardCharsets.UTF_8));
            return response;
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA", "N/A");
        } catch (NotFoundException_Exception e) {
            throw new NotFoundException("EndEntity", username);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void revokeCertificate(String uuid, String issuerDn, String serialNumber, int revocationReason) throws NotFoundException, AccessDeniedException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);
        try {
            ejbcaWS.revokeCert(issuerDn, serialNumber, revocationReason);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA " + e.getMessage());
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA of Certificate");
        } catch (NotFoundException_Exception e) {
            throw new NotFoundException("Certificate");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public EjbcaVersion getEjbcaVersion(String authorityInstanceUuid) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityInstanceUuid);
        String ejbcaVersion = ejbcaWS.getEjbcaVersion();
        return new EjbcaVersion(ejbcaVersion);
    }

    @Override
    public void searchCertificates(String authorityInstanceUuid, String restUrl) throws NotFoundException {
       WebClient ejbcaWC = authorityInstanceService.getRestApiConnection(authorityInstanceUuid);

       Pagination pagination = new Pagination();
       pagination.setPageSize(100);
       pagination.setCurrentPage(1);

       SearchCertificateSortRestRequest sort = new SearchCertificateSortRestRequest();

       SearchCertificatesRestRequestV2 request = new SearchCertificatesRestRequestV2();
       request.setPagination(pagination);

       SearchCertificatesRestResponseV2 response = ejbcaWC.post()
               .uri(restUrl + "/v2/certificate")
               .contentType(MediaType.APPLICATION_JSON)
               .bodyValue(request)
               .retrieve()
               .bodyToMono(SearchCertificatesRestResponseV2.class)
               .block();

       if (response.getPaginationSummary().getCurrentPage() < response.getPaginationSummary().getPageSize()) {
           throw new NotFoundException("Certificates");
       }
    }

    @Override
    public List<NameAndIdDto> getAvailableCas(String authorityInstanceUuid) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityInstanceUuid);
        try {
            List<NameAndId> cas = ejbcaWS.getAvailableCAs();
            if (cas == null || cas.isEmpty()) {
                throw new NotFoundException("CertificateProfile on ca", authorityInstanceUuid);
            }
            return cas.stream().map(p -> new NameAndIdDto(p.getId(), p.getName())).collect(Collectors.toList());
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (EjbcaException_Exception e) {
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

    private void prepareEndEntity(UserDataVOWS user, List<RequestAttributeDto> raProfileAttrs, List<RequestAttributeDto> issueAttrs) {
        setUserProfiles(user, raProfileAttrs);

        String email = AttributeDefinitionUtils.getAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_EMAIL, issueAttrs, BaseAttributeContent.class);
        if (StringUtils.isNotBlank(email)) {
            user.setEmail(email);
        }

        String san = AttributeDefinitionUtils.getAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_SAN, issueAttrs, BaseAttributeContent.class);
        if (StringUtils.isNotBlank(san)) {
            user.setSubjectAltName(san);
        }

        String extension = AttributeDefinitionUtils.getAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_EXTENSION, issueAttrs, BaseAttributeContent.class);
        setUserExtensions(user, extension);
    }

    private void prepareEndEntity(UserDataVOWS user, List<RequestAttributeDto> raProfileAttrs, Map<String, Object> metadata) {
        setUserProfiles(user, raProfileAttrs);

        String email = (String) metadata.get(CertificateEjbcaServiceImpl.META_EMAIL);
        if (StringUtils.isNotBlank(email)) {
            user.setEmail(email);
        }

        String san = (String) metadata.get(CertificateEjbcaServiceImpl.META_SAN);
        if (StringUtils.isNotBlank(san)) {
            user.setSubjectAltName(san);
        }

        String extension = (String) metadata.get(CertificateEjbcaServiceImpl.META_EXTENSION);
        setUserExtensions(user, extension);
    }

    private void setUserProfiles(UserDataVOWS user, List<RequestAttributeDto> raProfileAttrs) {
        //String tokenType = AttributeDefinitionUtils.getAttributeValue(ATTRIBUTE_TOKEN_TYPE, raProfileAttrs);
        //user.setTokenType(tokenType);
        user.setTokenType("USERGENERATED");

        NameAndIdDto endEntityProfile = AttributeDefinitionUtils.getNameAndIdData(ATTRIBUTE_END_ENTITY_PROFILE, raProfileAttrs);
        user.setEndEntityProfileName(endEntityProfile.getName());

        NameAndIdDto certificateProfile = AttributeDefinitionUtils.getNameAndIdData(ATTRIBUTE_CERTIFICATE_PROFILE, raProfileAttrs);
        user.setCertificateProfileName(certificateProfile.getName());

        NameAndIdDto ca = AttributeDefinitionUtils.getNameAndIdData(ATTRIBUTE_CERTIFICATION_AUTHORITY, raProfileAttrs);
        user.setCaName(ca.getName());

        boolean sendNotifications = false;
        Boolean value = AttributeDefinitionUtils.getAttributeContentValue(ATTRIBUTE_SEND_NOTIFICATIONS, raProfileAttrs, BaseAttributeContent.class);
        if (value != null) {
            sendNotifications = value;
        }
        user.setSendNotification(sendNotifications);

        boolean keyRecoverable = false;
        value = AttributeDefinitionUtils.getAttributeContentValue(ATTRIBUTE_KEY_RECOVERABLE, raProfileAttrs, BaseAttributeContent.class);
        if (value != null) {
            keyRecoverable = value;
        }
        user.setKeyRecoverable(keyRecoverable);
    }

    private void setUserExtensions(UserDataVOWS user, String extension) {
        if (StringUtils.isNotBlank(extension)) {
            List<ExtendedInformationWS> ei = new ArrayList<>();
            String[] extensions = extension.split(",[ ]*"); // remove spaces after the comma
            for (String data : extensions) {
                String[] extValue = data.split("=", 2); // split the string using = to 2 values
                // TODO: validation of the data
                ExtendedInformationWS eiWs = new ExtendedInformationWS();
                eiWs.setName(extValue[0]);
                eiWs.setValue(extValue[1]);
                ei.add(eiWs);
            }
            user.getExtendedInformation().addAll(ei);
        }
    }
}

package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.interfaces.connector.AuthorityInstanceController;
import com.czertainly.api.model.common.*;
import com.czertainly.api.model.common.attribute.*;
import com.czertainly.api.model.common.attribute.content.BaseAttributeContent;
import com.czertainly.api.model.common.attribute.content.JsonAttributeContent;
import com.czertainly.api.model.connector.authority.AuthorityProviderInstanceDto;
import com.czertainly.api.model.connector.authority.AuthorityProviderInstanceRequestDto;
import com.czertainly.ca.connector.ejbca.service.AuthorityInstanceService;
import com.czertainly.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class AuthorityInstanceControllerImpl implements AuthorityInstanceController {

    public static final String ATTRIBUTE_END_ENTITY_PROFILE = "endEntityProfile";
    public static final String ATTRIBUTE_CERTIFICATE_PROFILE = "certificateProfile";
    public static final String ATTRIBUTE_CERTIFICATION_AUTHORITY = "certificationAuthority";
    public static final String ATTRIBUTE_SEND_NOTIFICATIONS = "sendNotifications";
    public static final String ATTRIBUTE_KEY_RECOVERABLE = "keyRecoverable";
    public static final String ATTRIBUTE_USERNAME_GEN_METHOD = "usernameGenMethod";
    public static final String ATTRIBUTE_USERNAME_PREFIX = "usernamePrefix";
    public static final String ATTRIBUTE_USERNAME_POSTFIX = "usernamePostfix";

    public static final String ATTRIBUTE_END_ENTITY_PROFILE_LABEL = "End Entity Profile";
    public static final String ATTRIBUTE_CERTIFICATE_PROFILE_LABEL = "Certificate Profile";
    public static final String ATTRIBUTE_CERTIFICATION_AUTHORITY_LABEL = "Certification Authority";
    public static final String ATTRIBUTE_SEND_NOTIFICATIONS_LABEL = "Send Notifications";
    public static final String ATTRIBUTE_KEY_RECOVERABLE_LABEL = "Key Recoverable";
    public static final String ATTRIBUTE_USERNAME_GEN_METHOD_LABEL = "Username Generation Method";
    public static final String ATTRIBUTE_USERNAME_PREFIX_LABEL = "Username Prefix";
    public static final String ATTRIBUTE_USERNAME_POSTFIX_LABEL = "Username Postfix";

    @Autowired
    public void setAuthorityInstanceService(AuthorityInstanceService authorityInstanceService) {
        this.authorityInstanceService = authorityInstanceService;
    }
    @Autowired
    public void setEndEntityProfileEjbcaService(EndEntityProfileEjbcaService endEntityProfileEjbcaService) {
        this.endEntityProfileEjbcaService = endEntityProfileEjbcaService;
    }

    private AuthorityInstanceService authorityInstanceService;
    private EndEntityProfileEjbcaService endEntityProfileEjbcaService;

    @Override
    public List<AuthorityProviderInstanceDto> listAuthorityInstances() {
        return authorityInstanceService.listAuthorityInstances();
    }

    @Override
    public AuthorityProviderInstanceDto getAuthorityInstance(String uuid) throws NotFoundException {
        return authorityInstanceService.getAuthorityInstance(uuid);
    }

    @Override
    public AuthorityProviderInstanceDto createAuthorityInstance(AuthorityProviderInstanceRequestDto request) throws AlreadyExistException {
        return authorityInstanceService.createAuthorityInstance(request);
    }

    @Override
    public AuthorityProviderInstanceDto updateAuthorityInstance(String uuid, AuthorityProviderInstanceRequestDto request) throws NotFoundException {
        return authorityInstanceService.updateAuthorityInstance(uuid, request);
    }

    @Override
    public void removeAuthorityInstance(String uuid) throws NotFoundException {
        authorityInstanceService.removeAuthorityInstance(uuid);
    }

    @Override
    public void getConnection(String uuid) throws NotFoundException {
        authorityInstanceService.getConnection(uuid);
    }

    @Override
    public List<AttributeDefinition> listRAProfileAttributes(String uuid) throws NotFoundException {
        authorityInstanceService.getAuthorityInstance(uuid); // to validate that CA instance exists
        List<AttributeDefinition> attrs = new ArrayList<>();

        // transform objects to Attributes that can be selected
        List<JsonAttributeContent> endEntityProfilesContent = new ArrayList<>();
        ArrayList<NameAndIdDto> endEntityProfiles = new ArrayList<>(endEntityProfileEjbcaService.listEndEntityProfiles(uuid));
        for (NameAndIdDto endEntityProfile : endEntityProfiles) {
            JsonAttributeContent content = new JsonAttributeContent();
            content.setValue(endEntityProfile.getName());
            content.setData(endEntityProfile);
            endEntityProfilesContent.add(content);
        }

        AttributeDefinition endEntityProfile = new AttributeDefinition();
        endEntityProfile.setUuid("baf2d142-f35a-437f-81c7-35c128881fc0");
        endEntityProfile.setName(ATTRIBUTE_END_ENTITY_PROFILE);
        endEntityProfile.setLabel(ATTRIBUTE_END_ENTITY_PROFILE_LABEL);
        endEntityProfile.setDescription("Available end entity profiles");
        endEntityProfile.setType(AttributeType.JSON);
        endEntityProfile.setRequired(true);
        endEntityProfile.setReadOnly(false);
        endEntityProfile.setVisible(true);
        endEntityProfile.setList(true);
        endEntityProfile.setMultiSelect(false);
        endEntityProfile.setContent(endEntityProfilesContent);
        attrs.add(endEntityProfile);

        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping("authorityId", AttributeValueTarget.PATH_VARIABLE, uuid));
        mappings.add(new AttributeCallbackMapping(endEntityProfile.getName(), "endEntityProfileId.id", AttributeValueTarget.PATH_VARIABLE));

        AttributeDefinition certificateProfile = new AttributeDefinition();
        certificateProfile.setUuid("eb57a756-5a11-4d31-866b-e3f066f7a2b9");
        certificateProfile.setName(ATTRIBUTE_CERTIFICATE_PROFILE);
        certificateProfile.setLabel(ATTRIBUTE_CERTIFICATE_PROFILE_LABEL);
        certificateProfile.setDescription("Available certificate profiles for selected end entity profile");
        certificateProfile.setType(AttributeType.JSON);
        certificateProfile.setRequired(true);
        certificateProfile.setReadOnly(false);
        certificateProfile.setVisible(true);
        certificateProfile.setList(true);
        certificateProfile.setMultiSelect(false);

        AttributeCallback listCertificateProfilesCallback = new AttributeCallback();
        listCertificateProfilesCallback.setCallbackContext("/v1/authorityProvider/authorities/{authorityId}/endEntityProfiles/{endEntityProfileId}/certificateprofiles");
        listCertificateProfilesCallback.setCallbackMethod("GET");
        listCertificateProfilesCallback.setMappings(mappings);
        certificateProfile.setAttributeCallback(listCertificateProfilesCallback);

        attrs.add(certificateProfile);

        AttributeDefinition certificationAuthority = new AttributeDefinition();
        certificationAuthority.setUuid("edfd318a-8428-4fd1-b546-fd5238674f78");
        certificationAuthority.setName(ATTRIBUTE_CERTIFICATION_AUTHORITY);
        certificationAuthority.setLabel(ATTRIBUTE_CERTIFICATION_AUTHORITY_LABEL);
        certificationAuthority.setDescription("Available CAs for selected end entity profile");
        certificationAuthority.setType(AttributeType.JSON);
        certificationAuthority.setRequired(true);
        certificationAuthority.setReadOnly(false);
        certificationAuthority.setVisible(true);
        certificationAuthority.setList(true);
        certificationAuthority.setMultiSelect(false);

        AttributeCallback listCAsInProfileCallback = new AttributeCallback();
        listCAsInProfileCallback.setCallbackContext("/v1/authorityProvider/authorities/{authorityId}/endEntityProfiles/{endEntityProfileId}/cas");
        listCAsInProfileCallback.setCallbackMethod("GET");
        listCAsInProfileCallback.setMappings(mappings);
        certificationAuthority.setAttributeCallback(listCAsInProfileCallback);

        attrs.add(certificationAuthority);

        AttributeDefinition sendNotifications = new AttributeDefinition();
        sendNotifications.setUuid("e0ab3b4e-7681-4a9f-aec5-e025eb1a56a4");
        sendNotifications.setName(ATTRIBUTE_SEND_NOTIFICATIONS);
        sendNotifications.setLabel(ATTRIBUTE_SEND_NOTIFICATIONS_LABEL);
        sendNotifications.setDescription("Notifications to be send fot the end entity");
        sendNotifications.setType(AttributeType.BOOLEAN);
        sendNotifications.setRequired(false);
        sendNotifications.setReadOnly(false);
        sendNotifications.setVisible(true);
        sendNotifications.setList(false);
        sendNotifications.setMultiSelect(false);
        sendNotifications.setContent(new BaseAttributeContent<>(false));
        attrs.add(sendNotifications);

        AttributeDefinition keyRecoverable = new AttributeDefinition();
        keyRecoverable.setUuid("417077da-bb2b-4f35-a0f7-abf824e345ec");
        keyRecoverable.setName(ATTRIBUTE_KEY_RECOVERABLE);
        keyRecoverable.setLabel(ATTRIBUTE_KEY_RECOVERABLE_LABEL);
        keyRecoverable.setDescription("Recovery option for the private key");
        keyRecoverable.setType(AttributeType.BOOLEAN);
        keyRecoverable.setRequired(false);
        keyRecoverable.setReadOnly(false);
        keyRecoverable.setVisible(true);
        keyRecoverable.setList(false);
        keyRecoverable.setMultiSelect(false);
        keyRecoverable.setContent(new BaseAttributeContent<>(false));
        attrs.add(keyRecoverable);

        // available methods to generate the username in the EJBCA that should be unique
        // there is also option to prefix or postfix the generated username
        ArrayList<String> usernameGenMethods = new ArrayList<>();
        usernameGenMethods.add("RANDOM"); // this will generate random 16 byte Base64 encoded string
        usernameGenMethods.add("CN"); // CN from the request will be used to create username

        List<BaseAttributeContent<String>> usernameGenMethodsContent = new ArrayList<>();
        for (String usernameGenMethod : usernameGenMethods) {
            BaseAttributeContent<String> content = new BaseAttributeContent<>();
            content.setValue(usernameGenMethod);
            usernameGenMethodsContent.add(content);
        }

        // options to generate the username
        AttributeDefinition usernameGenMethod = new AttributeDefinition();
        usernameGenMethod.setUuid("3655e4f5-61d8-49c9-b116-f466a9f8c6b4");
        usernameGenMethod.setDescription("Method to generate username of the end entity");
        usernameGenMethod.setName(ATTRIBUTE_USERNAME_GEN_METHOD);
        usernameGenMethod.setLabel(ATTRIBUTE_USERNAME_GEN_METHOD_LABEL);
        usernameGenMethod.setType(AttributeType.STRING);
        usernameGenMethod.setRequired(true);
        usernameGenMethod.setReadOnly(false);
        usernameGenMethod.setVisible(true);
        usernameGenMethod.setList(true);
        usernameGenMethod.setMultiSelect(false);
        usernameGenMethod.setContent(usernameGenMethodsContent);
        attrs.add(usernameGenMethod);

        // prefix
        AttributeDefinition usernamePrefix = new AttributeDefinition();
        usernamePrefix.setUuid("c0c14dee-9319-4b03-af01-6a21bf30c1e3");
        usernamePrefix.setDescription("Optional prefix to be used when generating username");
        usernamePrefix.setName(ATTRIBUTE_USERNAME_PREFIX);
        usernamePrefix.setLabel(ATTRIBUTE_USERNAME_PREFIX_LABEL);
        usernamePrefix.setType(AttributeType.STRING);
        usernamePrefix.setRequired(false);
        usernamePrefix.setReadOnly(false);
        usernamePrefix.setVisible(true);
        usernamePrefix.setList(false);
        usernamePrefix.setMultiSelect(false);
        usernamePrefix.setContent(new BaseAttributeContent<>("czertainly-"));
        attrs.add(usernamePrefix);

        // postfix
        AttributeDefinition usernamePostfix = new AttributeDefinition();
        usernamePostfix.setUuid("5c94731f-621e-4851-b40d-b4f4897f0240");
        usernamePostfix.setDescription("Optional postfix to be used when generating username");
        usernamePostfix.setName(ATTRIBUTE_USERNAME_POSTFIX);
        usernamePostfix.setLabel(ATTRIBUTE_USERNAME_POSTFIX_LABEL);
        usernamePostfix.setType(AttributeType.STRING);
        usernamePostfix.setRequired(false);
        usernamePostfix.setReadOnly(false);
        usernamePostfix.setVisible(true);
        usernamePostfix.setList(false);
        usernamePostfix.setMultiSelect(false);
        usernamePrefix.setContent(new BaseAttributeContent<>("-generated"));
        attrs.add(usernamePostfix);

        return attrs;
    }

    @Override
    public void validateRAProfileAttributes(@PathVariable String uuid, @RequestBody List<RequestAttributeDto> attributes) throws ValidationException, NotFoundException {
        AttributeDefinitionUtils.validateAttributes(listRAProfileAttributes(uuid), attributes);
    }
}

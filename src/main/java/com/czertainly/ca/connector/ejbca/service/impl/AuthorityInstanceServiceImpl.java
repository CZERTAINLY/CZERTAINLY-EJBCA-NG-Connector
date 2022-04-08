package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationError;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.common.AttributeDefinition;
import com.czertainly.api.model.connector.authority.AuthorityProviderInstanceDto;
import com.czertainly.api.model.connector.authority.AuthorityProviderInstanceRequestDto;
import com.czertainly.api.model.core.credential.CredentialDto;
import com.czertainly.ca.connector.ejbca.config.ApplicationConfig;
import com.czertainly.ca.connector.ejbca.dao.AuthorityInstanceRepository;
import com.czertainly.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.czertainly.ca.connector.ejbca.service.AttributeService;
import com.czertainly.ca.connector.ejbca.service.AuthorityInstanceService;
import com.czertainly.ca.connector.ejbca.ws.EjbcaWS;
import com.czertainly.ca.connector.ejbca.ws.EjbcaWSService;
import com.czertainly.core.util.AttributeDefinitionUtils;
import com.czertainly.core.util.KeyStoreUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.*;
import javax.xml.ws.BindingProvider;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorityInstanceServiceImpl implements AuthorityInstanceService {
    private static final Logger logger = LoggerFactory.getLogger(AuthorityInstanceServiceImpl.class);

    private static final Map<Long, EjbcaWS> connectionsCache = new ConcurrentHashMap<>();

    @Value("${ejbca.timeout.connect:500}")
    private int connectionTimeout;

    @Value("${ejbca.timeout.request:1500}")
    private int requsetTimeout;

    @Autowired
    private AuthorityInstanceRepository authorityInstanceRepository;

    @Autowired
    private AttributeService attributeService;

    @Override
    public List<AuthorityProviderInstanceDto> listAuthorityInstances() {
        List<AuthorityInstance> authorities;
        authorities = authorityInstanceRepository.findAll();
        if (!authorities.isEmpty()) {
            return authorities
                    .stream().map(AuthorityInstance::mapToDto)
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public AuthorityProviderInstanceDto getAuthorityInstance(String uuid) throws NotFoundException {
        return authorityInstanceRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, uuid))
                .mapToDto();
    }

    @Override
    public AuthorityProviderInstanceDto createAuthorityInstance(AuthorityProviderInstanceRequestDto request) throws AlreadyExistException {
        if (authorityInstanceRepository.findByName(request.getName()).isPresent()) {
            throw new AlreadyExistException(AuthorityInstance.class, request.getName());
        }

        if (!attributeService.validateAttributes(
                request.getKind(), request.getAttributes())) {
            throw new ValidationException("Authority instance attributes validation failed.");
        }

        AuthorityInstance instance = new AuthorityInstance();
        instance.setName(request.getName());
        instance.setUrl(AttributeDefinitionUtils.getAttributeValue("url", request.getAttributes()));
        instance.setUuid(UUID.randomUUID().toString());
        CredentialDto credential = AttributeDefinitionUtils.getCredentialValue("credential", request.getAttributes());
        instance.setCredentialUuid(credential.getUuid());
        instance.setCredentialData(AttributeDefinitionUtils.serialize(AttributeDefinitionUtils.responseAttributeConverter(credential.getAttributes())));

        instance.setAttributes(AttributeDefinitionUtils.serialize(AttributeDefinitionUtils.mergeAttributes(attributeService.getAttributes(request.getKind()), request.getAttributes())));

        EjbcaWS connection;
        try {
            connection = createConnection(instance);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ValidationException(ValidationError.create(ExceptionUtils.getRootCauseMessage(e)));
        }

        authorityInstanceRepository.save(instance);

        try {
            connectionsCache.put(instance.getId(), connection);
        } catch (Exception e) {
            logger.error("Fail to cache connection to CA {} due to error {}", instance.getId(), e.getMessage(), e);
        }

        return instance.mapToDto();
    }

    @Override
    public AuthorityProviderInstanceDto updateAuthorityInstance(String uuid, AuthorityProviderInstanceRequestDto request) throws NotFoundException {
        AuthorityInstance instance = authorityInstanceRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, uuid));

        if (!attributeService.validateAttributes(
                request.getKind(), request.getAttributes())) {
            throw new ValidationException("Authority instance attributes validation failed.");
        }

        instance.setName(request.getName());
        instance.setUrl(AttributeDefinitionUtils.getAttributeValue("url", request.getAttributes()));

        CredentialDto credential = AttributeDefinitionUtils.getCredentialValue("credential", request.getAttributes());
        instance.setCredentialUuid(credential.getUuid());
        instance.setCredentialData(AttributeDefinitionUtils.serialize(AttributeDefinitionUtils.responseAttributeConverter(credential.getAttributes())));

        instance.setAttributes(AttributeDefinitionUtils.serialize(AttributeDefinitionUtils.mergeAttributes(attributeService.getAttributes(request.getKind()), request.getAttributes())));
        EjbcaWS connection;
        try {
            connection = createConnection(instance);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ValidationException(ValidationError.create(ExceptionUtils.getRootCauseMessage(e)));
        }

        authorityInstanceRepository.save(instance);

        try {
            connectionsCache.replace(instance.getId(), connection);
        } catch (Exception e) {
            logger.error("Fail to cache connection to CA {} due to error {}", instance.getId(), e.getMessage(), e);
        }

        return instance.mapToDto();
    }

    @Override
    public void removeAuthorityInstance(String uuid) throws NotFoundException {
        AuthorityInstance instance = authorityInstanceRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, uuid));

        authorityInstanceRepository.delete(instance);

        try {
            connectionsCache.remove(instance.getId());
        } catch (Exception e) {
            logger.error("Fail to cache connection to CA {} due to error {}", instance.getId(), e.getMessage(), e);
        }
    }

    @Override
    public EjbcaWS getConnection(String uuid) throws NotFoundException {
        AuthorityInstance instance = authorityInstanceRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, uuid));
        return getConnection(instance);
    }

    @Override
    public synchronized EjbcaWS getConnection(AuthorityInstance instance) {
        EjbcaWS port = connectionsCache.get(instance.getId());
        if (port != null) {
            return port;
        }

        port = createConnection(instance);

        try {
            connectionsCache.put(instance.getId(), port);
        } catch (Exception e) {
            logger.error("Fail to cache connection to CA {} due to error {}", instance.getId(), e.getMessage(), e);
        }

        return port;
    }

    private EjbcaWS createConnection(AuthorityInstance instance) {
        EjbcaWSService service = new EjbcaWSService(ApplicationConfig.WSDL_URL);
        EjbcaWS port = service.getEjbcaWSPort();
        final Map<String, Object> requestContext = ((BindingProvider) port).getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, instance.getUrl());
        requestContext.put(ApplicationConfig.CONNECT_TIMEOUT, connectionTimeout);
        requestContext.put(ApplicationConfig.REQUEST_TIMEOUT, 1500);
        requestContext.put(ApplicationConfig.REQUEST_SSL_SOCKET_FACTORY, createSSLSocketFactory(instance));

        logger.info("Connected to EJBCA {}", port.getEjbcaVersion());

        return port;
    }

    private SSLSocketFactory createSSLSocketFactory(AuthorityInstance instance) {
        try {
            List<AttributeDefinition> attributes = AttributeDefinitionUtils.deserialize(instance.getCredentialData());

            KeyManager[] km = null;
            String keyStoreData = AttributeDefinitionUtils.getAttributeValue("keyStore", attributes);
            if (keyStoreData != null && !keyStoreData.isEmpty()) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); //"SunX509"

                String keyStoreType = AttributeDefinitionUtils.getAttributeValue("keyStoreType", attributes);
                String keyStorePassword = AttributeDefinitionUtils.getAttributeValue("keyStorePassword", attributes);
                byte[] keyStoreBytes = Base64.getDecoder().decode(keyStoreData);

                kmf.init(KeyStoreUtils.bytes2KeyStore(keyStoreBytes, keyStorePassword, keyStoreType), keyStorePassword.toCharArray());
                km = kmf.getKeyManagers();
            }

            TrustManager[] tm = null;
            String trustStoreData = AttributeDefinitionUtils.getAttributeValue("trustStore", attributes);
            if (trustStoreData != null && !trustStoreData.isEmpty()) {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); //"SunX509"

                String trustStoreType = AttributeDefinitionUtils.getAttributeValue("trustStoreType", attributes);
                String trustStorePassword = AttributeDefinitionUtils.getAttributeValue("trustStorePassword", attributes);
                byte[] trustStoreBytes = Base64.getDecoder().decode(keyStoreData);

                tmf.init(KeyStoreUtils.bytes2KeyStore(trustStoreBytes, trustStorePassword, trustStoreType));
                tm = tmf.getTrustManagers();
            }

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(km, tm, new SecureRandom());

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SSLSocketFactory.", e);
        }
    }
}

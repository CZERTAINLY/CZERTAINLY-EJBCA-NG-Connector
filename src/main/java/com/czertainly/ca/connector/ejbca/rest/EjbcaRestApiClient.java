package com.czertainly.ca.connector.ejbca.rest;

import com.czertainly.api.clients.BaseApiClient;
import com.czertainly.api.exception.*;
import com.czertainly.api.model.common.attribute.AttributeDefinition;
import com.czertainly.api.model.common.attribute.content.BaseAttributeContent;
import com.czertainly.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.ExceptionErrorRestResponse;
import com.czertainly.core.util.AttributeDefinitionUtils;
import com.czertainly.core.util.KeyStoreUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class EjbcaRestApiClient {
    private static final Logger logger = LoggerFactory.getLogger(BaseApiClient.class);

    // Certificate attribute names
    public static final String ATTRIBUTE_KEYSTORE_TYPE = "keyStoreType";
    public static final String ATTRIBUTE_KEYSTORE = "keyStore";
    public static final String ATTRIBUTE_KEYSTORE_PASSWORD = "keyStorePassword";
    public static final String ATTRIBUTE_TRUSTSTORE_TYPE = "trustStoreType";
    public static final String ATTRIBUTE_TRUSTSTORE = "trustStore";
    public static final String ATTRIBUTE_TRUSTSTORE_PASSWORD = "trustStorePassword";

    protected WebClient webClient;

    public WebClient.RequestBodyUriSpec prepareRequest(HttpMethod method, List<AttributeDefinition> authAttributes) {

        WebClient.RequestBodySpec request;

        SslContext sslContext = createSslContext(authAttributes);
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        webClient.mutate().clientConnector(new ReactorClientHttpConnector(httpClient)).build();

        request = webClient.method(method);

        return (WebClient.RequestBodyUriSpec) request;
    }

    public static SslContext createSslContext(List<AttributeDefinition> attributes) {
        try {
            KeyManager km = null;
            String keyStoreData = AttributeDefinitionUtils.getAttributeContentValue(ATTRIBUTE_KEYSTORE, attributes, BaseAttributeContent.class);
            if (keyStoreData != null && !keyStoreData.isEmpty()) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); //"SunX509"

                String keyStoreType = AttributeDefinitionUtils.getAttributeContentValue(ATTRIBUTE_KEYSTORE_TYPE, attributes, BaseAttributeContent.class);
                String keyStorePassword = AttributeDefinitionUtils.getAttributeContentValue(ATTRIBUTE_KEYSTORE_PASSWORD, attributes, BaseAttributeContent.class);
                byte[] keyStoreBytes = Base64.getDecoder().decode(keyStoreData);

                kmf.init(KeyStoreUtils.bytes2KeyStore(keyStoreBytes, keyStorePassword, keyStoreType), keyStorePassword.toCharArray());
                km = kmf.getKeyManagers()[0];
            }

            TrustManager tm = null;
            String trustStoreData = AttributeDefinitionUtils.getAttributeContentValue(ATTRIBUTE_TRUSTSTORE, attributes, BaseAttributeContent.class);
            if (trustStoreData != null && !trustStoreData.isEmpty()) {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); //"SunX509"

                String trustStoreType = AttributeDefinitionUtils.getAttributeContentValue(ATTRIBUTE_TRUSTSTORE_TYPE, attributes, BaseAttributeContent.class);
                String trustStorePassword = AttributeDefinitionUtils.getAttributeContentValue(ATTRIBUTE_TRUSTSTORE_PASSWORD, attributes, BaseAttributeContent.class);
                byte[] trustStoreBytes = Base64.getDecoder().decode(trustStoreData);

                tmf.init(KeyStoreUtils.bytes2KeyStore(trustStoreBytes, trustStorePassword, trustStoreType));
                tm = tmf.getTrustManagers()[0];
            } else { // trust all
                tm = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                }[0];
            }

            return SslContextBuilder
                    .forClient()
                    .keyManager(km)
                    .trustManager(tm)
                    .protocols("TLSv1.2")
                    .build();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SslContext.", e);
        }
    }

    private static final ParameterizedTypeReference<List<String>> ERROR_LIST_TYPE_REF = new ParameterizedTypeReference<>() {
    };

    public static WebClient prepareWebClient() {
        return WebClient.builder()
                .filter(ExchangeFilterFunction.ofResponseProcessor(EjbcaRestApiClient::handleHttpExceptions))
                .build();
    }

    public static Mono<ClientResponse> handleHttpExceptions(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(ExceptionErrorRestResponse.class).flatMap(body ->
                Mono.error(new EjbcaRestApiException(body.getErrorMessage(), clientResponse.statusCode(), body)));
    }

    public static <T, R> R processRequest(Function<T, R> func, T request) {
        try {
            return func.apply(request);
        } catch (Exception e) {
            Throwable unwrapped = Exceptions.unwrap(e);
            logger.error(unwrapped.getMessage(), unwrapped);
        }
        return null;
    }

    public void searchCertificates(AuthorityInstance instance) {
        List<AttributeDefinition> attributes = AttributeDefinitionUtils.deserialize(instance.getCredentialData());
        WebClient.RequestBodyUriSpec request = prepareRequest(HttpMethod.POST, attributes);

        processRequest(r -> r
                .uri(getRestApiUrl(instance))
                .retrieve()
                .toEntity(Void.class)
                .block().getBody(),
                request);

    }

    private String getRestApiUrl(AuthorityInstance instance) {
        URL wsUrl = null;
        try {
            wsUrl = new URL(instance.getUrl());
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
        }

        return "https://" + wsUrl.getHost() + ":" + wsUrl.getPort() + "/ejbca/ejbca-rest-api/v2/certificate";
    }
}

package com.czertainly.ca.connector.ejbca.config;

import com.czertainly.api.model.common.attribute.v2.AttributeType;
import com.czertainly.api.model.common.attribute.v2.InfoAttribute;
import com.czertainly.api.model.common.attribute.v2.InfoAttributeProperties;
import com.czertainly.api.model.common.attribute.v2.content.AttributeContentType;
import com.czertainly.api.model.common.attribute.v2.content.StringAttributeContent;
import com.czertainly.api.model.core.discovery.DiscoveryStatus;
import com.czertainly.ca.connector.ejbca.dao.entity.DiscoveryHistory;
import com.czertainly.ca.connector.ejbca.service.DiscoveryHistoryService;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Configuration
public class CustomAsyncConfigurer implements AsyncConfigurer {

    private DiscoveryHistoryService discoveryHistoryService;

    @Autowired
    public void setDiscoveryHistoryService(DiscoveryHistoryService discoveryHistoryService) {
        this.discoveryHistoryService = discoveryHistoryService;
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("CertificateDiscovery-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            if (method.getName().equals("discoverCertificate")) {
                DiscoveryHistory history = (DiscoveryHistory) params[1];
                history.setStatus(DiscoveryStatus.FAILED);
                history.setMeta(AttributeDefinitionUtils.serialize(getReasonMeta(ex.getMessage())));
                discoveryHistoryService.setHistory(history);
            }
        };
    }

    private List<InfoAttribute> getReasonMeta(String exception) {
        List<InfoAttribute> attributes = new ArrayList<>();

        //Exception Reason
        InfoAttribute attribute = new InfoAttribute();
        attribute.setName("reason");
        attribute.setUuid("abc0412a-60f6-11ed-9b6a-0242ac120002");
        attribute.setContentType(AttributeContentType.STRING);
        attribute.setType(AttributeType.META);
        attribute.setDescription("Reason for failure");

        InfoAttributeProperties attributeProperties = new InfoAttributeProperties();
        attributeProperties.setLabel("Reason");
        attributeProperties.setVisible(true);

        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of(new StringAttributeContent(exception)));
        attributes.add(attribute);

        return attributes;
    }
}
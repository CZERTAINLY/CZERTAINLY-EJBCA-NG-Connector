package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.attribute.content.BaseAttributeContent;
import com.czertainly.api.model.common.attribute.content.DateTimeAttributeContent;
import com.czertainly.api.model.connector.discovery.DiscoveryDataRequestDto;
import com.czertainly.api.model.connector.discovery.DiscoveryProviderDto;
import com.czertainly.api.model.connector.discovery.DiscoveryRequestDto;
import com.czertainly.api.model.core.discovery.DiscoveryStatus;
import com.czertainly.ca.connector.ejbca.dao.CertificateRepository;
import com.czertainly.ca.connector.ejbca.dao.entity.Certificate;
import com.czertainly.ca.connector.ejbca.dao.entity.DiscoveryHistory;
import com.czertainly.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.Pagination;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificateCriteriaRestRequest;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificateSortRestRequest;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificatesRestRequestV2;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.CertificateRestResponseV2;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import com.czertainly.ca.connector.ejbca.service.DiscoveryHistoryService;
import com.czertainly.ca.connector.ejbca.service.DiscoveryService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.util.MetaDefinitions;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryServiceImpl.class);

    /**
     * This constant represents the number of certificate per page in searching
     */
    private static final int EJBCA_SEARCH_PAGE_SIZE = 1000;

    @Autowired
    public void setEjbcaService(EjbcaService ejbcaService) {
        this.ejbcaService = ejbcaService;
    }
    @Autowired
    public void setCertificateRepository(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }
    @Autowired
    public void setDiscoveryHistoryService(DiscoveryHistoryService discoveryHistoryService) {
        this.discoveryHistoryService = discoveryHistoryService;
    }

    private EjbcaService ejbcaService;
    private CertificateRepository certificateRepository;
    private DiscoveryHistoryService discoveryHistoryService;

    @Override
    @Async
    public void discoverCertificate(DiscoveryRequestDto request, DiscoveryHistory history) throws NotFoundException {
        try {
            discoverCertificatesInternal(request, history);
        } catch (Exception e) {
            history.setStatus(DiscoveryStatus.FAILED);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("reason", e.getMessage());
            history.setMeta(MetaDefinitions.serialize(meta));
            discoveryHistoryService.setHistory(history);
            throw e;
        }
    }

    @Override
    public DiscoveryProviderDto getProviderDtoData(DiscoveryDataRequestDto request, DiscoveryHistory history) {
        DiscoveryProviderDto dto = new DiscoveryProviderDto();
        dto.setUuid(history.getUuid());
        dto.setName(history.getName());
        dto.setStatus(history.getStatus());
        dto.setMeta(MetaDefinitions.deserialize(history.getMeta()));
        int totalCertificateSize = certificateRepository.findByDiscoveryId(history.getId()).size();
        dto.setTotalCertificatesDiscovered(totalCertificateSize);
        if (history.getStatus() == DiscoveryStatus.IN_PROGRESS) {
            dto.setCertificateData(new ArrayList<>());
            dto.setTotalCertificatesDiscovered(0);
        } else {
            Pageable page = PageRequest.of(request.getStartIndex(), request.getEndIndex());
            dto.setCertificateData(certificateRepository.findAllByDiscoveryId(history.getId(), page).stream().map(Certificate::mapToDto).collect(Collectors.toList()));
        }
        return dto;
    }

    private void discoverCertificatesInternal(DiscoveryRequestDto request, DiscoveryHistory history) throws NotFoundException {
        logger.info("Discovery initiated for the request with name {}", request.getName());
        Map<String, Object> meta = new LinkedHashMap<>();
        int certificatesFound = 0;

        final AuthorityInstanceNameAndUuidDto instance = AttributeDefinitionUtils.getJsonAttributeContentData(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_INSTANCE, request.getAttributes(), AuthorityInstanceNameAndUuidDto.class);
        final String restApiUrl = AttributeDefinitionUtils.getAttributeContentValue(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_RESTAPI_URL, request.getAttributes(), BaseAttributeContent.class);
        final List<NameAndIdDto> cas = AttributeDefinitionUtils.getJsonAttributeContentDataList(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_CA, request.getAttributes(), NameAndIdDto.class);
        final List<NameAndIdDto> eeProfiles = AttributeDefinitionUtils.getJsonAttributeContentDataList(DiscoveryAttributeServiceImpl.ATTRIBUTE_END_ENTITY_PROFILE, request.getAttributes(), NameAndIdDto.class);
        final List<String> statuses = AttributeDefinitionUtils.getAttributeContentValueList(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_STATUS, request.getAttributes(), BaseAttributeContent.class);
        final ZonedDateTime issuedAfter = AttributeDefinitionUtils.getAttributeContentValue(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_ISSUED_AFTER, request.getAttributes(), DateTimeAttributeContent.class);

        SearchCertificatesRestRequestV2 searchRequest = prepareSearchRequest(cas, eeProfiles, statuses, issuedAfter);
        SearchCertificatesRestResponseV2 searchResponse = null;
        while (searchResponse == null || searchResponse.getPaginationSummary().getTotalCerts() == null) {
            searchResponse = ejbcaService.searchCertificates(instance.getUuid(), restApiUrl, searchRequest);
            // set the next page
            searchRequest.getPagination().setCurrentPage(searchResponse.getPaginationSummary().getCurrentPage()+1);

            parseAndCreateCertificateEntry(searchResponse, history);
            certificatesFound = certificatesFound + searchResponse.getCertificates().size();
        }

        history.setStatus(DiscoveryStatus.COMPLETED);

        meta.put("totalCertificates", certificatesFound);

        history.setMeta(MetaDefinitions.serialize(meta));
        discoveryHistoryService.setHistory(history);
        logger.info("Discovery Completed. Name of the discovery is {}", request.getName());
    }

    private SearchCertificatesRestRequestV2 prepareSearchRequest(
            List<NameAndIdDto> cas, List<NameAndIdDto> eeProfiles,
            List<String> statuses, ZonedDateTime issuedAfter
    ) {
        SearchCertificatesRestRequestV2 request = new SearchCertificatesRestRequestV2();

        Pagination pagination = new Pagination();
        pagination.setPageSize(EJBCA_SEARCH_PAGE_SIZE);
        pagination.setCurrentPage(1);

        SearchCertificateSortRestRequest sort = new SearchCertificateSortRestRequest();
        sort.setOperation(SearchCertificateSortRestRequest.SortOperation.ASC.name());
        sort.setProperty(SearchCertificateSortRestRequest.SortProperty.USERNAME.name());

        List<SearchCertificateCriteriaRestRequest> criteria = new ArrayList<>();

        if (cas != null) {
            for (NameAndIdDto ca : cas) {
                SearchCertificateCriteriaRestRequest c = new SearchCertificateCriteriaRestRequest();
                c.setOperation(SearchCertificateCriteriaRestRequest.CriteriaOperation.EQUAL.name());
                c.setProperty(SearchCertificateCriteriaRestRequest.CriteriaProperty.CA.name());
                c.setValue(ca.getName());
                criteria.add(c);
            }
        }

        if (eeProfiles != null) {
            for (NameAndIdDto eeProfile : eeProfiles) {
                SearchCertificateCriteriaRestRequest c = new SearchCertificateCriteriaRestRequest();
                c.setOperation(SearchCertificateCriteriaRestRequest.CriteriaOperation.EQUAL.name());
                c.setProperty(SearchCertificateCriteriaRestRequest.CriteriaProperty.END_ENTITY_PROFILE.name());
                c.setValue(eeProfile.getName());
                criteria.add(c);
            }
        }

        if (statuses != null) {
            for (String status : statuses) {
                SearchCertificateCriteriaRestRequest c = new SearchCertificateCriteriaRestRequest();
                c.setOperation(SearchCertificateCriteriaRestRequest.CriteriaOperation.EQUAL.name());
                c.setProperty(SearchCertificateCriteriaRestRequest.CriteriaProperty.STATUS.name());
                c.setValue(status);
                criteria.add(c);
            }
        }

        SearchCertificateCriteriaRestRequest c = new SearchCertificateCriteriaRestRequest();
        c.setOperation(SearchCertificateCriteriaRestRequest.CriteriaOperation.AFTER.name());
        c.setProperty(SearchCertificateCriteriaRestRequest.CriteriaProperty.ISSUED_DATE.name());
        if (issuedAfter != null) {
            c.setValue(issuedAfter.toString());
        } else {
            c.setValue("2000-01-01T00:00:00Z"); // before the EJBCA was born
        }
        criteria.add(c);

        request.setPagination(pagination);
        request.setSort(sort);
        request.setCriteria(criteria);

        return request;
    }

    private void parseAndCreateCertificateEntry(SearchCertificatesRestResponseV2 searchResponse, DiscoveryHistory discoveryHistory) throws NullPointerException {
        logger.info("Parsing {} certificates from page {} in discovery {}",
                searchResponse.getCertificates().size(), searchResponse.getPaginationSummary().getCurrentPage(), discoveryHistory.getName());

        for (CertificateRestResponseV2 certificateRestResponseV2 : searchResponse.getCertificates()) {
            Certificate cert = new Certificate();

            cert.setUuid(UUID.randomUUID().toString());
            cert.setDiscoveryId(discoveryHistory.getId());
            cert.setBase64Content(Base64.getEncoder().encodeToString(certificateRestResponseV2.getCertificate()));

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("certificateProfileId", certificateRestResponseV2.getCertificateProfileId());
            meta.put("endEntityProfileId", certificateRestResponseV2.getEndEntityProfileId());
            meta.put("username", certificateRestResponseV2.getUsername());
            meta.put("discoverySource", "EJBCA-NG");
            meta.put("discoveryName", discoveryHistory.getName());

            cert.setMeta(MetaDefinitions.serialize(meta));

            certificateRepository.save(cert);
        }
    }
}
package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.connector.v2.CertRevocationDto;
import com.czertainly.api.model.connector.v2.CertificateDataResponseDto;
import com.czertainly.api.model.connector.v2.CertificateRenewRequestDto;
import com.czertainly.api.model.connector.v2.CertificateSignRequestDto;
import org.springframework.security.access.AccessDeniedException;

public interface CertificateEjbcaService {

    CertificateDataResponseDto issueCertificate(String uuid, CertificateSignRequestDto request) throws Exception;

    CertificateDataResponseDto renewCertificate(String uuid, CertificateRenewRequestDto request) throws Exception;

    void revokeCertificate(String uuid, CertRevocationDto request) throws NotFoundException, AccessDeniedException;
}

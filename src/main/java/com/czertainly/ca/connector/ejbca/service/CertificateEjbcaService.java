package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.connector.v2.*;
import org.springframework.security.access.AccessDeniedException;

public interface CertificateEjbcaService {

    CertificateDataResponseDto issueCertificate(String uuid, CertificateSignRequestDto request) throws Exception;

    CertificateDataResponseDto renewCertificate(String uuid, CertificateRenewRequestDto request) throws Exception;

    void revokeCertificate(String uuid, CertRevocationDto request) throws NotFoundException, AccessDeniedException;

    CertificateIdentificationResponseDto identifyCertificate(String uuid, CertificateIdentificationRequestDto request) throws NotFoundException, ValidationException;
}

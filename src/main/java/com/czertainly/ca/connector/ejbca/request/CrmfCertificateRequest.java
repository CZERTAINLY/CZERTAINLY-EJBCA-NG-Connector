package com.czertainly.ca.connector.ejbca.request;

import com.czertainly.api.model.core.enums.CertificateRequestFormat;
import com.czertainly.ca.connector.ejbca.exception.CertificateRequestException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.crmf.jcajce.JcaCertificateRequestMessage;

public class CrmfCertificateRequest implements CertificateRequest {

    private final byte[] encoded;
    private final JcaCertificateRequestMessage certificateRequestMessage;

    public CrmfCertificateRequest(byte[] crmfRequest) throws CertificateRequestException {
        this.encoded = crmfRequest;
        this.certificateRequestMessage = new JcaCertificateRequestMessage(crmfRequest);
    }

    @Override
    public byte[] getEncoded() {
        return encoded;
    }

    @Override
    public X500Name getSubject() {
        return certificateRequestMessage.getCertTemplate().getSubject();
    }

    @Override
    public CertificateRequestFormat getFormat() {
        return CertificateRequestFormat.CRMF;
    }

}

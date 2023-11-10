
package com.czertainly.ca.connector.ejbca.ws;

import jakarta.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 * 
 */
@WebFault(name = "CesecoreException", targetNamespace = "http://ws.protocol.core.ejbca.org/")
public class CesecoreException_Exception
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private CesecoreException faultInfo;

    /**
     * 
     * @param faultInfo
     * @param message
     */
    public CesecoreException_Exception(String message, CesecoreException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param faultInfo
     * @param cause
     * @param message
     */
    public CesecoreException_Exception(String message, CesecoreException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: com.czertainly.ca.connector.ejbca.ws.CesecoreException
     */
    public CesecoreException getFaultInfo() {
        return faultInfo;
    }

}

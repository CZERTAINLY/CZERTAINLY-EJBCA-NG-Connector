package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.interfaces.connector.InfoController;
import com.czertainly.api.model.client.connector.InfoResponse;
import com.czertainly.api.model.core.connector.FunctionGroupCode;
import com.czertainly.ca.connector.ejbca.EndpointsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class InfoControllerImpl implements InfoController {
    private static final Logger logger = LoggerFactory.getLogger(InfoControllerImpl.class);

    @Autowired
    private EndpointsListener endpointsListener;

    @Override
    public List<InfoResponse> listSupportedFunctions() {
        logger.info("Listing the end points for EJBCA NG connector");
        List<String> kinds = List.of("EJBCA");
        List<InfoResponse> functions = new ArrayList<>();
        functions.add(new InfoResponse(kinds, FunctionGroupCode.AUTHORITY_PROVIDER, endpointsListener.getEndpoints()));

        return functions;
    }
}

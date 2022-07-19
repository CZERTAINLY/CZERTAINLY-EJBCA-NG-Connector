package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.interfaces.connector.InfoController;
import com.czertainly.api.model.client.connector.InfoResponse;
import com.czertainly.api.model.core.connector.FunctionGroupCode;
import com.czertainly.ca.connector.ejbca.EndpointsListener;
import com.czertainly.ca.connector.ejbca.enums.DiscoveryKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class InfoControllerImpl implements InfoController {
    private static final Logger logger = LoggerFactory.getLogger(InfoControllerImpl.class);

    @Autowired
    public void setEndpointsListener(EndpointsListener endpointsListener) {
        this.endpointsListener = endpointsListener;
    }

    private EndpointsListener endpointsListener;

    @Override
    public List<InfoResponse> listSupportedFunctions() {
        logger.info("Listing the end points for EJBCA NG connector");
        List<String> kinds = List.of("EJBCA");
        EnumSet.allOf(DiscoveryKind.class);
        List<InfoResponse> functions = new ArrayList<>();
        functions.add(new InfoResponse(
                kinds,
                FunctionGroupCode.AUTHORITY_PROVIDER,
                endpointsListener.getEndpoints(FunctionGroupCode.AUTHORITY_PROVIDER))
        );
        functions.add(new InfoResponse(
                Stream.of(DiscoveryKind.values()).map(Enum::name).collect(Collectors.toList()),
                FunctionGroupCode.DISCOVERY_PROVIDER,
                endpointsListener.getEndpoints(FunctionGroupCode.DISCOVERY_PROVIDER))
        );

        return functions;
    }
}

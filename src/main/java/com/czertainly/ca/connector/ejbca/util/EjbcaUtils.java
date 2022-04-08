package com.czertainly.ca.connector.ejbca.util;

import com.czertainly.api.model.core.authority.EndEntityDto;
import com.czertainly.api.model.core.authority.EndEntityExtendedInfoDto;
import com.czertainly.api.model.core.authority.EndEntityStatus;
import com.czertainly.ca.connector.ejbca.ws.MatchType;
import com.czertainly.ca.connector.ejbca.ws.MatchWith;
import com.czertainly.ca.connector.ejbca.ws.UserDataVOWS;
import com.czertainly.ca.connector.ejbca.ws.UserMatch;

import java.util.List;
import java.util.stream.Collectors;

public class EjbcaUtils {

    public static UserMatch prepareUsernameMatch(String username) {
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(MatchWith.MATCH_WITH_USERNAME.getCode());
        usermatch.setMatchtype(MatchType.MATCH_TYPE_EQUALS.getCode());
        usermatch.setMatchvalue(username);
        return usermatch;
    }

    public static UserMatch prepareEndEntityProfileMatch(String endEntityProfileName) {
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(MatchWith.MATCH_WITH_ENDENTITYPROFILE.getCode());
        usermatch.setMatchtype(MatchType.MATCH_TYPE_EQUALS.getCode());
        usermatch.setMatchvalue(endEntityProfileName);
        return usermatch;
    }

    public static EndEntityDto mapToUserDetailDTO(UserDataVOWS userDataVOWS) {
        EndEntityDto userDetailDTO = new EndEntityDto();
        userDetailDTO.setUsername(userDataVOWS.getUsername());
        userDetailDTO.setSubjectDN(userDataVOWS.getSubjectDN());
        userDetailDTO.setEmail(userDataVOWS.getEmail());
        userDetailDTO.setSubjectAltName(userDataVOWS.getSubjectAltName());
        userDetailDTO.setStatus(EndEntityStatus.fromCode(userDataVOWS.getStatus()));
        if (userDataVOWS.getExtendedInformation() != null) {
            List<EndEntityExtendedInfoDto> extendedInformationList = userDataVOWS.getExtendedInformation()
                    .stream()
                    .map(i -> new EndEntityExtendedInfoDto(i.getName(), i.getValue()))
                    .collect(Collectors.toList());
            userDetailDTO.setExtensionData(extendedInformationList);
        }
        return userDetailDTO;
    }
}

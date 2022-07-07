package com.czertainly.ca.connector.ejbca.util;

import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.attribute.content.JsonAttributeContent;

import java.util.ArrayList;
import java.util.List;

public class LocalAttributeUtil {

    public static List<JsonAttributeContent> convertFromNameAndId(List<NameAndIdDto> data) {
        List<JsonAttributeContent> contentList = new ArrayList<>();
        for (NameAndIdDto x : data) {
            JsonAttributeContent content = new JsonAttributeContent(x.getName(), x);
            contentList.add(content);
        }
        return contentList;
    }
}

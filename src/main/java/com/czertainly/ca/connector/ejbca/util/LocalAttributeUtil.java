package com.czertainly.ca.connector.ejbca.util;

import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.attribute.content.ObjectAttributeContent;

import java.util.ArrayList;
import java.util.List;

public class LocalAttributeUtil {

    public static List<ObjectAttributeContent> convertFromNameAndId(List<NameAndIdDto> data) {
        List<ObjectAttributeContent> contentList = new ArrayList<>();
        for (NameAndIdDto x : data) {
            ObjectAttributeContent content = new ObjectAttributeContent(x.getName(), x);
            contentList.add(content);
        }
        return contentList;
    }
}

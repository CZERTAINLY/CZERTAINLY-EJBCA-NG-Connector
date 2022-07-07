package com.czertainly.ca.connector.ejbca.dto;

public class AuthorityInstanceNameAndUuidDto {

    private String name;
    private String uuid;

    public AuthorityInstanceNameAndUuidDto() {
    }

    public AuthorityInstanceNameAndUuidDto(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

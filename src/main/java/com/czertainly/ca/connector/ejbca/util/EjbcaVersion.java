package com.czertainly.ca.connector.ejbca.util;

public class EjbcaVersion {

    private int techVersion;
    private int majorVersion;
    private int minorVersion;
    private String version;

    public EjbcaVersion(String ejbcaVersion) {
        readVersionNumbers(ejbcaVersion);
    }

    private void readVersionNumbers(String ejbcaVersion) {
        String[] parts = ejbcaVersion.split(" ");
        String[] version = parts[1].split("\\.");

        this.techVersion = Integer.valueOf(version[0]);
        this.majorVersion = Integer.valueOf(version[1]);
        this.minorVersion = Integer.valueOf(version[2]);
        this.version = parts[2];
    }

    public int getTechVersion() {
        return techVersion;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "EJBCA " + techVersion + "." + majorVersion + "." + minorVersion + " " + version;
    }
}

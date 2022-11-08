package com.czertainly.ca.connector.ejbca.enums;

/**
 * Stores the checksum of a Java-based migration.
 */
public enum JavaMigrationChecksums {
    V202206231700__AttributeChanges(1236550637),
    V202211031300__AttributeV2Changes(-44253029);
    private final int checksum;

    JavaMigrationChecksums(int checksum) {
        this.checksum = checksum;
    }

    public int getChecksum() {
        return checksum;
    }
}
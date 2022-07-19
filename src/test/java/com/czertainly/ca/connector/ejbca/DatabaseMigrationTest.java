package com.czertainly.ca.connector.ejbca;

import com.czertainly.ca.connector.ejbca.enums.JavaMigrationChecksums;
import com.czertainly.core.util.DatabaseMigrationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Simple tests for calculating checksums and validating the migration scripts integrity.
 */
public class DatabaseMigrationTest {

    @Test
    public void testCalculateChecksum_V202206231700__AttributeChanges() {
        int checksum = DatabaseMigrationUtils.calculateChecksum("src/main/java/db/migration/V202206231700__AttributeChanges.java");

        Assertions.assertEquals(JavaMigrationChecksums.V202206231700__AttributeChanges.getChecksum(), checksum);
    }
}

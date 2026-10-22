/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */
package org.onap.so.etsi.nfvo.ns.lcm.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MariaDbBooleanColumnMigrationTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.7").withDatabaseName("nfvo")
            .withUsername("root").withPassword("test_password");

    private Flyway flyway(final String target) {
        return Flyway.configure().dataSource(MARIADB.getJdbcUrl(), "root", "test_password")
                .locations("classpath:db/migration").cleanDisabled(false).target(target).load();
    }

    private void insertOpOcc(final Connection connection, final String id, final String autoInvocation,
            final String cancelPending) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT IGNORE INTO NS_INST (NS_INST_ID, NAME, NSD_ID, NSD_INVARIANT_ID, STATUS,"
                    + " STATUS_UPDATED_TIME) VALUES ('ns-1', 'ns', 'nsd', 'inv', 'NOT_INSTANTIATED', NOW())");
            statement.executeUpdate("INSERT INTO NS_LCM_OP_OCCS (ID, OPERATION_STATE, NS_INST_ID, OPERATION,"
                    + " IS_AUTO_INVOCATION, OPERATION_PARAMS, IS_CANCEL_PENDING) VALUES ('" + id
                    + "', 'PROCESSING', 'ns-1', 'INSTANTIATE', '" + autoInvocation + "', '{}', '" + cancelPending
                    + "')");
        }
    }

    @Test
    void existingZeroOneRowsAreConvertedToBooleans() throws Exception {
        flyway("latest").clean();
        flyway("1.1").migrate();
        try (Connection connection = DriverManager.getConnection(MARIADB.getJdbcUrl(), "root", "test_password")) {
            insertOpOcc(connection, "op-true", "1", "0");
            insertOpOcc(connection, "op-false", "0", "1");

            flyway("latest").migrate();

            try (Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery("SELECT ID, IS_AUTO_INVOCATION, IS_CANCEL_PENDING"
                            + " FROM NS_LCM_OP_OCCS ORDER BY ID")) {
                rs.next();
                assertEquals("op-false", rs.getString(1));
                assertEquals(false, rs.getBoolean(2));
                assertEquals(true, rs.getBoolean(3));
                rs.next();
                assertEquals("op-true", rs.getString(1));
                assertEquals(true, rs.getBoolean(2));
                assertEquals(false, rs.getBoolean(3));
            }
            try (Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery("SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.COLUMNS"
                            + " WHERE TABLE_SCHEMA='nfvo' AND TABLE_NAME='NS_LCM_OP_OCCS'"
                            + " AND COLUMN_NAME IN ('IS_AUTO_INVOCATION','IS_CANCEL_PENDING') ORDER BY COLUMN_NAME")) {
                rs.next();
                assertEquals("IS_AUTO_INVOCATION", rs.getString(1));
                assertEquals("tinyint", rs.getString(2));
                rs.next();
                assertEquals("IS_CANCEL_PENDING", rs.getString(1));
                assertEquals("tinyint", rs.getString(2));
                assertFalse(rs.next());
            }
        }
    }

    @Test
    void unexpectedTextValuesAbortTheMigration() throws Exception {
        flyway("latest").clean();
        flyway("1.1").migrate();
        try (Connection connection = DriverManager.getConnection(MARIADB.getJdbcUrl(), "root", "test_password")) {
            insertOpOcc(connection, "op-text", "true", "false");
        }
        assertThrows(FlywayException.class, () -> flyway("latest").migrate());
    }
}

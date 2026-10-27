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
package org.onap.so.etsi.nfvo.ns.lcm.bpmn.flows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.onap.so.etsi.nfvo.ns.lcm.bpmn.flows.Constants.TENANT_ID;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = {CamundaDatabaseConfiguration.class, CamundaCustomConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableAutoConfiguration(exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class,
        FlywayAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class})
class MariaDbProcessEngineStartupTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.7")
            .withDatabaseName("camundabpmn")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.hikari.camunda.jdbcUrl", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.hikari.camunda.username", MARIADB::getUsername);
        registry.add("spring.datasource.hikari.camunda.password", MARIADB::getPassword);
        registry.add("spring.datasource.hikari.camunda.driver-class-name", () -> "org.mariadb.jdbc.Driver");
    }

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    @Qualifier("camundaBpmDataSource")
    private DataSource camundaDataSource;

    @Test
    void databaseUsesMariaDbDefaultIsolationLevel() throws SQLException {
        try (final Connection connection = camundaDataSource.getConnection()) {
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, connection.getTransactionIsolation());
        }
    }

    @Test
    void processEngineStartsAndDeploysWorkflows() {
        assertFalse(processEngine.getRepositoryService().createProcessDefinitionQuery().tenantIdIn(TENANT_ID).list()
                .isEmpty());
    }
}

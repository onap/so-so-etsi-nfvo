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
package org.onap.so.etsi.nfvo.ns.lcm.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.so.etsi.nfvo.ns.lcm.EtsiSoNsLcmManagerUrlProvider;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.CancelModeType;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.NfvoNsInst;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.NsLcmOpOcc;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.NsLcmOpType;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.OperationStateEnum;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.State;
import org.onap.so.etsi.nfvo.ns.lcm.database.service.DatabaseServiceProvider;
import org.onap.so.etsi.nfvo.ns.lcm.model.NsLcmOpOccsNsLcmOpOcc;

@RunWith(MockitoJUnitRunner.class)
public class NsLcmOperationOccurrenceManagerTest {

    private static final String NS_INST_ID = UUID.randomUUID().toString();
    private static final String NS_LCM_OP_OCC_ID = UUID.randomUUID().toString();
    private static final URI NS_LCM_OP_OCC_URI = URI.create(
            "http://so-etsi-nfvo-ns-lcm.onap:9095/so/so-etsi-nfvo-ns-lcm/v1/api/nslcm/v1/ns_lcm_op_occs/"
                    + NS_LCM_OP_OCC_ID);
    private static final URI NS_INSTANCE_URI = URI.create(
            "http://so-etsi-nfvo-ns-lcm.onap:9095/so/so-etsi-nfvo-ns-lcm/v1/api/nslcm/v1/ns_instances/"
                    + NS_INST_ID);

    @Mock
    private DatabaseServiceProvider databaseServiceProvider;

    @Mock
    private EtsiSoNsLcmManagerUrlProvider etsiSoNsLcmManagerUrlProvider;

    @InjectMocks
    private NsLcmOperationOccurrenceManager objUnderTest;

    @Test
    public void testGetNsLcmOperationOccurrence_idNotFoundInDatabase_returnsEmpty() {
        when(databaseServiceProvider.getNsLcmOpOcc(eq(NS_LCM_OP_OCC_ID))).thenReturn(Optional.empty());

        final Optional<NsLcmOpOccsNsLcmOpOcc> result =
                objUnderTest.getNsLcmOperationOccurrence(NS_LCM_OP_OCC_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetNsLcmOperationOccurrence_found_returnsConvertedModelWithAllFields() {
        final LocalDateTime now = LocalDateTime.now();
        final NfvoNsInst nsInst = new NfvoNsInst().nsInstId(NS_INST_ID).name("name").nsdId("nsdId")
                .status(State.INSTANTIATED).nsdInvariantId("inv").statusUpdatedTime(now);
        final NsLcmOpOcc nsLcmOpOcc = new NsLcmOpOcc().id(NS_LCM_OP_OCC_ID)
                .operationState(OperationStateEnum.COMPLETED).stateEnteredTime(now).startTime(now)
                .nfvoNsInst(nsInst).operation(NsLcmOpType.INSTANTIATE).isAutoInvocation(false)
                .isCancelPending(false).operationParams("{}");

        when(databaseServiceProvider.getNsLcmOpOcc(eq(NS_LCM_OP_OCC_ID))).thenReturn(Optional.of(nsLcmOpOcc));
        when(etsiSoNsLcmManagerUrlProvider.getNsLcmOpOccUri(NS_LCM_OP_OCC_ID)).thenReturn(NS_LCM_OP_OCC_URI);
        when(etsiSoNsLcmManagerUrlProvider.getCreatedNsResourceUri(NS_INST_ID)).thenReturn(NS_INSTANCE_URI);

        final Optional<NsLcmOpOccsNsLcmOpOcc> result =
                objUnderTest.getNsLcmOperationOccurrence(NS_LCM_OP_OCC_ID);

        assertTrue(result.isPresent());
        final NsLcmOpOccsNsLcmOpOcc occ = result.get();
        assertEquals(NS_LCM_OP_OCC_ID, occ.getId());
        assertEquals(NS_INST_ID, occ.getNsInstanceId());
        assertEquals(NsLcmOpOccsNsLcmOpOcc.OperationStateEnum.COMPLETED, occ.getOperationState());
        assertEquals(NsLcmOpOccsNsLcmOpOcc.LcmOperationTypeEnum.INSTANTIATE, occ.getLcmOperationType());
        assertEquals("{}", occ.getOperationParams());
        assertNotNull(occ.getLinks());
        assertNotNull(occ.getLinks().getSelf());
        assertEquals(NS_LCM_OP_OCC_URI.toString(), occ.getLinks().getSelf().getHref());
        assertNotNull(occ.getLinks().getNsInstance());
        assertEquals(NS_INSTANCE_URI.toString(), occ.getLinks().getNsInstance().getHref());
    }

    @Test
    public void testGetNsLcmOperationOccurrence_withCancelMode_cancelModeIsConvertedToApiModel() {
        final LocalDateTime now = LocalDateTime.now();
        final NsLcmOpOcc nsLcmOpOcc = new NsLcmOpOcc().id(NS_LCM_OP_OCC_ID)
                .operationState(OperationStateEnum.PROCESSING).stateEnteredTime(now).startTime(now)
                .operation(NsLcmOpType.TERMINATE).isAutoInvocation(false).isCancelPending(true)
                .cancelMode(CancelModeType.FORCEFUL).operationParams("{}");

        when(databaseServiceProvider.getNsLcmOpOcc(eq(NS_LCM_OP_OCC_ID))).thenReturn(Optional.of(nsLcmOpOcc));
        when(etsiSoNsLcmManagerUrlProvider.getNsLcmOpOccUri(NS_LCM_OP_OCC_ID)).thenReturn(NS_LCM_OP_OCC_URI);

        final Optional<NsLcmOpOccsNsLcmOpOcc> result =
                objUnderTest.getNsLcmOperationOccurrence(NS_LCM_OP_OCC_ID);

        assertTrue(result.isPresent());
        assertEquals(NsLcmOpOccsNsLcmOpOcc.CancelModeEnum.FORCEFUL, result.get().getCancelMode());
        assertEquals(NsLcmOpOccsNsLcmOpOcc.LcmOperationTypeEnum.TERMINATE, result.get().getLcmOperationType());
    }

    @Test
    public void testGetNsLcmOperationOccurrence_withoutNsInst_linksDoNotIncludeNsInstance() {
        final LocalDateTime now = LocalDateTime.now();
        final NsLcmOpOcc nsLcmOpOcc = new NsLcmOpOcc().id(NS_LCM_OP_OCC_ID)
                .operationState(OperationStateEnum.FAILED).stateEnteredTime(now).startTime(now)
                .operation(NsLcmOpType.INSTANTIATE).isAutoInvocation(false).isCancelPending(false)
                .operationParams("{}");

        when(databaseServiceProvider.getNsLcmOpOcc(eq(NS_LCM_OP_OCC_ID))).thenReturn(Optional.of(nsLcmOpOcc));
        when(etsiSoNsLcmManagerUrlProvider.getNsLcmOpOccUri(NS_LCM_OP_OCC_ID)).thenReturn(NS_LCM_OP_OCC_URI);

        final Optional<NsLcmOpOccsNsLcmOpOcc> result =
                objUnderTest.getNsLcmOperationOccurrence(NS_LCM_OP_OCC_ID);

        assertTrue(result.isPresent());
        final NsLcmOpOccsNsLcmOpOcc occ = result.get();
        assertNotNull(occ.getLinks());
        assertNotNull(occ.getLinks().getSelf());
        assertNull(occ.getLinks().getNsInstance());
        assertNull(occ.getNsInstanceId());
    }

    @Test
    public void testGetNsLcmOperationOccurrence_found_operationParamsPreservedFromDatabase() {
        final LocalDateTime now = LocalDateTime.now();
        final String operationParams = "{\"nsFlavourId\":\"default\"}";
        final NsLcmOpOcc nsLcmOpOcc = new NsLcmOpOcc().id(NS_LCM_OP_OCC_ID)
                .operationState(OperationStateEnum.PROCESSING).stateEnteredTime(now).startTime(now)
                .operation(NsLcmOpType.INSTANTIATE).isAutoInvocation(false).isCancelPending(false)
                .operationParams(operationParams);

        when(databaseServiceProvider.getNsLcmOpOcc(eq(NS_LCM_OP_OCC_ID))).thenReturn(Optional.of(nsLcmOpOcc));
        when(etsiSoNsLcmManagerUrlProvider.getNsLcmOpOccUri(NS_LCM_OP_OCC_ID)).thenReturn(NS_LCM_OP_OCC_URI);

        final Optional<NsLcmOpOccsNsLcmOpOcc> result =
                objUnderTest.getNsLcmOperationOccurrence(NS_LCM_OP_OCC_ID);

        assertTrue(result.isPresent());
        assertEquals(operationParams, result.get().getOperationParams());
    }
}

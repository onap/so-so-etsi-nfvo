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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.UUID;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.so.etsi.nfvo.ns.lcm.EtsiSoNsLcmManagerUrlProvider;
import org.onap.so.etsi.nfvo.ns.lcm.bpmn.flows.service.JobExecutorService;
import org.onap.so.etsi.nfvo.ns.lcm.model.CreateNsRequest;
import org.onap.so.etsi.nfvo.ns.lcm.model.InstantiateNsRequest;
import org.onap.so.etsi.nfvo.ns.lcm.model.NsInstancesNsInstance;
import org.onap.so.etsi.nfvo.ns.lcm.model.TerminateNsRequest;

@RunWith(MockitoJUnitRunner.class)
public class NsLifeCycleManagerTest {

    private static final String NS_INSTANCE_ID = UUID.randomUUID().toString();
    private static final String NS_LCM_OP_OCC_ID = UUID.randomUUID().toString();
    private static final String GLOBAL_CUSTOMER_ID = UUID.randomUUID().toString();
    private static final String SERVICE_TYPE = "NetworkService";
    private static final URI EXPECTED_NS_RESOURCE_URI = URI.create(
            "http://so-etsi-nfvo-ns-lcm.onap:9095/so/so-etsi-nfvo-ns-lcm/v1/api/nslcm/v1/ns_instances/"
                    + NS_INSTANCE_ID);
    private static final URI EXPECTED_NS_LCM_OP_OCC_URI = URI.create(
            "http://so-etsi-nfvo-ns-lcm.onap:9095/so/so-etsi-nfvo-ns-lcm/v1/api/nslcm/v1/ns_lcm_op_occs/"
                    + NS_LCM_OP_OCC_ID);

    @Mock
    private JobExecutorService jobExecutorService;

    @Mock
    private EtsiSoNsLcmManagerUrlProvider etsiSoNsLcmManagerUrlProvider;

    @InjectMocks
    private NsLifeCycleManager objUnderTest;

    @Before
    public void setUp() {
        when(etsiSoNsLcmManagerUrlProvider.getCreatedNsResourceUri(NS_INSTANCE_ID))
                .thenReturn(EXPECTED_NS_RESOURCE_URI);
        when(etsiSoNsLcmManagerUrlProvider.getNsLcmOpOccUri(NS_LCM_OP_OCC_ID))
                .thenReturn(EXPECTED_NS_LCM_OP_OCC_URI);
    }

    @Test
    public void testCreateNs_callsJobExecutorAndReturnsLocationAndNsInstance() {
        final CreateNsRequest createNsRequest = new CreateNsRequest().nsdId("nsdId").nsName("name");
        when(jobExecutorService.runCreateNsJob(eq(createNsRequest), eq(GLOBAL_CUSTOMER_ID), eq(SERVICE_TYPE)))
                .thenReturn(new NsInstancesNsInstance().id(NS_INSTANCE_ID));

        final ImmutablePair<URI, NsInstancesNsInstance> result =
                objUnderTest.createNs(createNsRequest, GLOBAL_CUSTOMER_ID, SERVICE_TYPE);

        assertEquals(EXPECTED_NS_RESOURCE_URI, result.getLeft());
        assertEquals(NS_INSTANCE_ID, result.getRight().getId());
        verify(jobExecutorService).runCreateNsJob(eq(createNsRequest), eq(GLOBAL_CUSTOMER_ID), eq(SERVICE_TYPE));
    }

    @Test
    public void testInstantiateNs_callsJobExecutorAndReturnsOpOccUri() {
        final InstantiateNsRequest instantiateNsRequest = new InstantiateNsRequest().nsFlavourId("default");
        when(jobExecutorService.runInstantiateNsJob(eq(NS_INSTANCE_ID), eq(instantiateNsRequest)))
                .thenReturn(NS_LCM_OP_OCC_ID);

        final URI result = objUnderTest.instantiateNs(NS_INSTANCE_ID, instantiateNsRequest);

        assertEquals(EXPECTED_NS_LCM_OP_OCC_URI, result);
        verify(jobExecutorService).runInstantiateNsJob(eq(NS_INSTANCE_ID), eq(instantiateNsRequest));
    }

    @Test
    public void testTerminateNs_callsJobExecutorAndReturnsOpOccUri() {
        final TerminateNsRequest terminateNsRequest = new TerminateNsRequest();
        when(jobExecutorService.runTerminateNsJob(eq(NS_INSTANCE_ID), eq(terminateNsRequest)))
                .thenReturn(NS_LCM_OP_OCC_ID);

        final URI result = objUnderTest.terminateNs(NS_INSTANCE_ID, terminateNsRequest);

        assertEquals(EXPECTED_NS_LCM_OP_OCC_URI, result);
        verify(jobExecutorService).runTerminateNsJob(eq(NS_INSTANCE_ID), eq(terminateNsRequest));
    }

    @Test
    public void testDeleteNs_callsJobExecutorForGivenNsInstanceId() {
        doNothing().when(jobExecutorService).runDeleteNsJob(eq(NS_INSTANCE_ID));

        objUnderTest.deleteNs(NS_INSTANCE_ID);

        verify(jobExecutorService).runDeleteNsJob(eq(NS_INSTANCE_ID));
    }
}

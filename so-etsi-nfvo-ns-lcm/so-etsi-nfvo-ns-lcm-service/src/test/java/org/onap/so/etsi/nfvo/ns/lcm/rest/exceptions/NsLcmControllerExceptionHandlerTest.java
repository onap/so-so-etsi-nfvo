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
package org.onap.so.etsi.nfvo.ns.lcm.rest.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.onap.so.etsi.nfvo.ns.lcm.bpmn.flows.exceptions.NsRequestProcessingException;
import org.onap.so.etsi.nfvo.ns.lcm.model.InlineResponse400;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class NsLcmControllerExceptionHandlerTest {

    private final NsLcmControllerExceptionHandler objUnderTest = new NsLcmControllerExceptionHandler();

    @Test
    public void testHandleNsRequestProcessingException_withProblemDetails_returnsProblemDetailsBody() {
        final String detail = "NS instance already exists";
        final InlineResponse400 problemDetails =
                new InlineResponse400().status(HttpStatus.CONFLICT.value()).detail(detail);
        final NsRequestProcessingException exception =
                new NsRequestProcessingException("conflict error", problemDetails);

        final ResponseEntity<InlineResponse400> response =
                objUnderTest.handleNsRequestProcessingException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(detail, response.getBody().getDetail());
        assertEquals(HttpStatus.CONFLICT.value(), (int) response.getBody().getStatus());
    }

    @Test
    public void testHandleNsRequestProcessingException_noProblemDetails_returnsMessageAsDetail() {
        final String message = "NS workflow failed";
        final NsRequestProcessingException exception = new NsRequestProcessingException(message);

        final ResponseEntity<InlineResponse400> response =
                objUnderTest.handleNsRequestProcessingException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(message, response.getBody().getDetail());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), (int) response.getBody().getStatus());
    }

    @Test
    public void testHandleGenericException_returnsMessageAsDetail() {
        final String message = "Unexpected error occurred";
        final Exception exception = new RuntimeException(message);

        final ResponseEntity<InlineResponse400> response =
                objUnderTest.handleNsRequestProcessingException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(message, response.getBody().getDetail());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), (int) response.getBody().getStatus());
    }

    @Test
    public void testHandleNsRequestProcessingException_problemDetailsWithNullDetail_returnsNullDetail() {
        final InlineResponse400 problemDetails = new InlineResponse400().status(HttpStatus.BAD_REQUEST.value());
        final NsRequestProcessingException exception =
                new NsRequestProcessingException("bad request", problemDetails);

        final ResponseEntity<InlineResponse400> response =
                objUnderTest.handleNsRequestProcessingException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), (int) response.getBody().getStatus());
    }
}

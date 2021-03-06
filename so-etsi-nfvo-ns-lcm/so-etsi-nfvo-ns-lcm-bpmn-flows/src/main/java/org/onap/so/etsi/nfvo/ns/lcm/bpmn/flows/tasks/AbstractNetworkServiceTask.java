/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation.
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
package org.onap.so.etsi.nfvo.ns.lcm.bpmn.flows.tasks;

import static org.onap.so.etsi.nfvo.ns.lcm.bpmn.flows.CamundaVariableNameConstants.JOB_ID_PARAM_NAME;
import static org.onap.so.etsi.nfvo.ns.lcm.bpmn.flows.CamundaVariableNameConstants.NS_INSTANCE_ID_PARAM_NAME;
import static org.onap.so.etsi.nfvo.ns.lcm.bpmn.flows.CamundaVariableNameConstants.NS_WORKFLOW_PROCESSING_EXCEPTION_PARAM_NAME;
import static org.onap.so.etsi.nfvo.ns.lcm.bpmn.flows.CamundaVariableNameConstants.OCC_ID_PARAM_NAME;
import java.time.LocalDateTime;
import java.util.Optional;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.JobStatusEnum;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.NfvoJob;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.NfvoJobStatus;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.NfvoNsInst;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.OperationStateEnum;
import org.onap.so.etsi.nfvo.ns.lcm.database.beans.State;
import org.onap.so.etsi.nfvo.ns.lcm.database.service.DatabaseServiceProvider;
import org.onap.so.etsi.nfvo.ns.lcm.model.InlineResponse400;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Waqas Ikram (waqas.ikram@est.tech)
 * @author Andrew Lamb (andrew.a.lamb@est.tech)
 *
 */
public abstract class AbstractNetworkServiceTask {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    protected final DatabaseServiceProvider databaseServiceProvider;

    protected AbstractNetworkServiceTask(final DatabaseServiceProvider jobServiceProvider) {
        this.databaseServiceProvider = jobServiceProvider;
    }

    public void addJobStatus(final DelegateExecution execution, final JobStatusEnum jobStatus,
            final String description) {
        final NfvoJobStatus nfvoJobStatus =
                new NfvoJobStatus().status(jobStatus).description(description).updatedTime(LocalDateTime.now());
        logger.info("Adding NfvoJobStatus {}", nfvoJobStatus);
        final NfvoJob nfvoJob = getNfvoJob(execution);
        nfvoJob.nfvoJobStatus(nfvoJobStatus);
        databaseServiceProvider.addJob(nfvoJob);
    }

    public void setJobStatus(final DelegateExecution execution, final JobStatusEnum jobStatus,
            final String description) {
        logger.info("Setting Job Status to {}", jobStatus);
        final NfvoJob nfvoJob = getNfvoJob(execution);
        nfvoJob.status(jobStatus);
        if (JobStatusEnum.STARTED.equals(jobStatus)) {
            nfvoJob.processInstanceId(execution.getProcessInstanceId());
        }

        if (JobStatusEnum.FINISHED.equals(jobStatus)) {
            nfvoJob.endTime(LocalDateTime.now());
        }

        nfvoJob.nfvoJobStatus(
                new NfvoJobStatus().status(jobStatus).description(description).updatedTime(LocalDateTime.now()));
        databaseServiceProvider.addJob(nfvoJob);

    }

    public void setJobStatusToError(final DelegateExecution execution, final String description) {
        logger.info("Setting Job Status to {}", JobStatusEnum.ERROR);

        final String jobId = (String) execution.getVariable(JOB_ID_PARAM_NAME);
        final Optional<NfvoJob> optional = databaseServiceProvider.getJob(jobId);
        if (optional.isPresent()) {
            final InlineResponse400 problemDetails =
                    (InlineResponse400) execution.getVariable(NS_WORKFLOW_PROCESSING_EXCEPTION_PARAM_NAME);

            final NfvoJob nfvoJob = optional.get();
            nfvoJob.status(JobStatusEnum.ERROR).endTime(LocalDateTime.now());

            if (problemDetails != null) {
                logger.error("Found failed reason: {}", problemDetails);
                nfvoJob.nfvoJobStatus(new NfvoJobStatus().status(JobStatusEnum.ERROR)
                        .description(problemDetails.getDetail()).updatedTime(LocalDateTime.now()));
            }
            nfvoJob.nfvoJobStatus(new NfvoJobStatus().status(JobStatusEnum.ERROR).description(description)
                    .updatedTime(LocalDateTime.now()));

            databaseServiceProvider.addJob(nfvoJob);
        }
        logger.info("Finished setting Job Status to {}", JobStatusEnum.ERROR);

    }

    public void updateNsLcmOpOccStatusToCompleted(final DelegateExecution execution) {
        logger.info("Executing updateNsLcmOpOccStatusToCompleted ...");

        updateNsLcmOpOccOperationState(execution, OperationStateEnum.COMPLETED);

        logger.info("Finished executing updateNsLcmOpOccStatusToCompleted ...");

    }

    public void updateNsLcmOpOccStatusToFailed(final DelegateExecution execution) {
        logger.info("Executing updateNsLcmOpOccStatusToFailed ...");

        updateNsLcmOpOccOperationState(execution, OperationStateEnum.FAILED);

        logger.info("Finished executing updateNsLcmOpOccStatusToFailed ...");

    }

    protected void abortOperation(final DelegateExecution execution, final String message) {
        abortOperation(execution, message, new InlineResponse400().detail(message));
    }

    private void updateNsLcmOpOccOperationState(final DelegateExecution execution,
            final OperationStateEnum operationState) {
        final String occId = (String) execution.getVariable(OCC_ID_PARAM_NAME);

        final boolean isSuccessful = databaseServiceProvider.updateNsLcmOpOccOperationState(occId, operationState);
        if (!isSuccessful) {
            final String message =
                    "Unable to update NsLcmOpOcc " + occId + " operationState to" + operationState + " in database";
            logger.error(message);
            abortOperation(execution, message);
        }
    }

    protected void abortOperation(final DelegateExecution execution, final String message,
            final InlineResponse400 problemDetails) {
        logger.error(message);
        execution.setVariable(NS_WORKFLOW_PROCESSING_EXCEPTION_PARAM_NAME, problemDetails);
        throw new BpmnError("WORKFLOW_FAILED");
    }

    private NfvoJob getNfvoJob(final DelegateExecution execution) {
        final String jobId = (String) execution.getVariable(JOB_ID_PARAM_NAME);
        final Optional<NfvoJob> optional = databaseServiceProvider.getJob(jobId);
        if (optional.isEmpty()) {
            final String message = "Unable to find job using job id: " + jobId;
            logger.error(message);
            execution.setVariable(NS_WORKFLOW_PROCESSING_EXCEPTION_PARAM_NAME, new InlineResponse400().detail(message));
            throw new BpmnError("WORKFLOW_FAILED");

        }
        return optional.get();
    }

    protected void updateNsInstanceStatus(final DelegateExecution execution, final State nsStatus) {
        final String nsInstId = (String) execution.getVariable(NS_INSTANCE_ID_PARAM_NAME);

        logger.info("Updating NfvoNsInst Status to {} and saving to DB", nsStatus);
        databaseServiceProvider.updateNsInstState(nsInstId, nsStatus);
    }

    protected NfvoNsInst getNfvoNsInst(final DelegateExecution execution) {
        final String nsInstId = (String) execution.getVariable(NS_INSTANCE_ID_PARAM_NAME);
        return getNfvoNsInst(execution, nsInstId);
    }

    protected NfvoNsInst getNfvoNsInst(final DelegateExecution execution, final String nsInstId) {
        logger.info("Getting NfvoNsInst to update with nsInstId: {}", nsInstId);
        final Optional<NfvoNsInst> optionalNfvoNsInst = databaseServiceProvider.getNfvoNsInst(nsInstId);

        if (optionalNfvoNsInst.isEmpty()) {
            final String message = "Unable to find NS Instance in database using id: " + nsInstId;
            abortOperation(execution, message);
        }

        return optionalNfvoNsInst.get();
    }

}

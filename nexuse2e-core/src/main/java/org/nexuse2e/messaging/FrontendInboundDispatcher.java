/**
 *  NEXUSe2e Business Messaging Open Source
 *  Copyright 2000-2021, direkt gruppe GmbH
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation version 3 of
 *  the License.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.nexuse2e.messaging;


import org.apache.log4j.Logger;
import org.nexuse2e.BeanStatus;
import org.nexuse2e.Engine;
import org.nexuse2e.Layer;
import org.nexuse2e.MessageStatus;
import org.nexuse2e.NexusException;
import org.nexuse2e.ProtocolSpecificKey;
import org.nexuse2e.configuration.EngineConfiguration;
import org.nexuse2e.configuration.ParameterDescriptor;
import org.nexuse2e.controller.StateTransitionException;
import org.nexuse2e.logging.LogMessage;
import org.nexuse2e.pojo.ActionPojo;
import org.nexuse2e.pojo.ChoreographyPojo;
import org.nexuse2e.pojo.MessagePojo;
import org.nexuse2e.pojo.ParticipantPojo;
import org.nexuse2e.util.NexusThreadStorage;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


/**
 * Component dispatching inbound messages to the correct queue based on their choreography (business process).
 * Additionally the message is checked for consistency with the messaging protocol it belongs to and acknowledgements
 * or error messages are created accordingly if the underlying messaging protocol requires it.
 *
 * @author gesch, sschulze
 */
public class FrontendInboundDispatcher extends ChoreographyValidator implements Dispatcher, Pipelet, InitializingBean {

    private static Logger LOG = Logger.getLogger(FrontendInboundDispatcher.class);

    private ProtocolAdapter[] protocolAdapters;

    private Map<String, StatusUpdateSerializer> statusUpdateSerializers = new HashMap<String, StatusUpdateSerializer>();

    private BeanStatus status = BeanStatus.UNDEFINED;

    private Hashtable<String, MessageContext> synchronousReplies = new Hashtable<String, MessageContext>();

    private Map<String, Object> parameters = new HashMap<String, Object>();

    private boolean frontendPipelet;
    private boolean forwardPipelet;

    private Pipeline pipeline;

    /* (non-Javadoc)
     * @see org.nexuse2e.messaging.FrontendDispatcher#processMessage(org.nexuse2e.messaging.MessageContext)
     */
    public MessageContext processMessage(MessageContext messageContext) throws NexusException {

        boolean failureInHeaderProcessing = false;
        ChoreographyPojo choreography = null;
        ParticipantPojo participant = null;
        MessageContext responseMessageContext = null;
        List<ErrorDescriptor> errorMessages = new ArrayList<ErrorDescriptor>();

        MessagePojo messagePojo = messageContext.getMessagePojo();

        LOG.debug(new LogMessage("Entering FrontendInboundDispatcher.processMessage...", messagePojo));

        // Set some thread-local information so everyone in the pipeline can always access it
        NexusThreadStorage.set("conversationId", messageContext.getConversation().getConversationId());
        NexusThreadStorage.set("messageId", messageContext.getMessagePojo().getMessageId());

        // extract header data
        // Shouldn't only the state machine set the status?
        //            if ( messagePojo.getConversation() != null
        //                    && messagePojo.getConversation().getStatus() == org.nexuse2e.Constants.CONVERSATION_STATUS_CREATED ) {
        //                messagePojo.getConversation().setStatus( org.nexuse2e.Constants.CONVERSATION_STATUS_PROCESSING );
        //                if ( LOG.isDebugEnabled() ) {
        //	                LOG.debug( new LogMessage( "MessageType:" + messagePojo.getType(), messagePojo.getConversation()
        //	                        .getConversationId(), messagePojo.getMessageId() ) );
        //                }
        //            }

        // get protocol adapter
        ProtocolAdapter protocolAdapter = getProtocolAdapterByKey(messageContext.getProtocolSpecificKey());
        if (protocolAdapter == null) {
            String msg = "No protocol implementation found for key: " + messageContext.getProtocolSpecificKey();
            //                LOG.error( new LogMessage( msg, messagePojo ) );
            throw new NexusException(new LogMessage(msg, messageContext));
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace(new LogMessage("ProtocolAdapter found for incoming message", messagePojo));
        }

        // get choreography
        try {
            choreography = validateChoreography(messageContext);
            LOG.debug(new LogMessage("matching choreography found", messagePojo));
        } catch (NexusException e) {
            LOG.error(new LogMessage("Error while validating choreography", messagePojo, e), e);
            responseMessageContext =
                protocolAdapter.createErrorAcknowledgement(Constants.ErrorMessageReasonCode.CHOREOGRAPHY_NOT_FOUND, null, messageContext, null);
            failureInHeaderProcessing = true;
            errorMessages
                .add(new ErrorDescriptor("No matching choreography found in configuration: " + messagePojo.getConversation().getChoreography().getName()));
        }

        // get action
        String actionId = messagePojo.getAction().getName();
        ActionPojo action = Engine.getInstance().getActiveConfigurationAccessService().getActionFromChoreographyByActionId(choreography, actionId);
        if ((action == null) && (messagePojo.getType() == Constants.INT_MESSAGE_TYPE_NORMAL)) {
            failureInHeaderProcessing = true;
            errorMessages.add(new ErrorDescriptor("No matching action found in configuration: " + messagePojo.getAction().getName()));
            responseMessageContext =
                protocolAdapter.createErrorAcknowledgement(Constants.ErrorMessageReasonCode.ACTION_NOT_PERMITTED, null, messageContext, null);
        }

        if (!failureInHeaderProcessing) {
            // get participant
            try {
                participant = validateParticipant(messageContext);
                messageContext.setParticipant(participant);
                LOG.debug(new LogMessage("matching participant found", messagePojo));
            } catch (NexusException e) {
                e.printStackTrace();
                LOG.error(new LogMessage("No matching participant found: " + messagePojo.getConversation().getPartner().getPartnerId(), messagePojo));
                responseMessageContext =
                    protocolAdapter.createErrorAcknowledgement(Constants.ErrorMessageReasonCode.PARTICIPANT_NOT_FOUND, choreography, messageContext, null);
                failureInHeaderProcessing = true;
                errorMessages
                    .add(new ErrorDescriptor("No matching participant found in configuration: " + messagePojo.getConversation().getPartner().getPartnerId()));
            }
        }

        // if there was a failure, write to log an return
        if (failureInHeaderProcessing) {
            LOG.error(new LogMessage("Error processing inbound message.", messagePojo));
            for (ErrorDescriptor errorDescriptor : errorMessages) {
                LOG.error(new LogMessage("Error - " + errorDescriptor.getDescription(), messagePojo));
            }
            return null;
        }

        // header data are accessible, but may not be valid for the current conversation
        if (messageContext.getErrors() != null && messageContext.getErrors().size() > 0) {
            //headerInvalid = true;
            // Shouldn't only the state machine set the status? Why does processing proceed after message/conv was marked as failed/error?
            //                messageContext.getMessagePojo().setStatus( Constants.MessageStatus.FAILED.getOrdinal() );
            //                messageContext.getConversation().setStatus( Constants.CONVERSATION_STATUS_ERROR );
        }

        // determine message type for log messages
        String msgType = messagePojo.getTypeName().toLowerCase();

        if (LOG.isInfoEnabled()) {
            LOG.info(new LogMessage(
                "Received " + msgType + " (" + messagePojo.getMessageId() + ") from " + participant.getPartner().getPartnerId() + " for " + choreography
                    .getName() + (action != null ? "/" + action.getName() : ""), messagePojo));
        }

        // handle different message types
        if (messagePojo.isNormal()) {
            responseMessageContext = handleNormalMessage(messageContext, participant, protocolAdapter, choreography, errorMessages);
        } else if (messagePojo.isAck()) {
            handleAcknowledgment(messageContext);
        } else if (messagePojo.isError()) {
            handleError(messageContext);
        } else {
            LOG.error(new LogMessage("Message of unknown type (" + messagePojo.getType() + ") received", messagePojo));
        }

        // ThreadLocal-objects need to be manually removed
        NexusThreadStorage.remove("conversationId");
        NexusThreadStorage.remove("messageId");

        return responseMessageContext;
    }

    /**
     * Handles dispatching of received message of type NORMAL.
     *
     * @param messageContext
     * @throws NexusException
     */
    private MessageContext handleNormalMessage(MessageContext messageContext, ParticipantPojo participant, ProtocolAdapter protocolAdapter,
                                               ChoreographyPojo choreography, List<ErrorDescriptor> errorMessages) throws NexusException {
        MessageContext responseMessageContext = null;

        boolean headerInvalid = false;

        MessagePojo message = messageContext.getMessagePojo();
        // duplicate
        if (isDuplicate(message)) {
            // duplicate found
            // get according acknowledgment
            if (!messageContext.isRequestMessage() && !participant.getConnection().isSynchronous()) {
                MessageContext ackErr = Engine.getInstance().getTransactionService().getMessageContext(message.getMessageId(), true);
                boolean foundResponse =
                    (ackErr != null && ackErr.getMessagePojo() != null && (ackErr.getMessagePojo().isAck() || ackErr.getMessagePojo().isError()));
                LOG.info(new LogMessage("Received duplicate message: " + message.getMessageId() +
                                        (foundResponse ? ", re-sending ACK/ERROR " + ackErr.getMessagePojo().getMessageId() : ""), message));

                if (foundResponse) {
                    MessageHandlingCenter.getInstance().processMessage(ackErr);
                }
            } else {
                LOG.info(new LogMessage("Ignoring duplicate request or synchronous message", message));
            }
        } else {
            // this message is not a duplicate
            // now we check the different processing models
            if (messageContext.isRequestMessage()) {
                responseMessageContext = handleRequest(messageContext, headerInvalid, protocolAdapter, choreography, errorMessages);
            } else {
                // Forward the message to check the transition, persist it and pass to backend
                try {
                    messageContext.getStateMachine().queueMessage();
                } catch (StateTransitionException e) {
                    LOG.warn(new LogMessage(e.getMessage(), messageContext));
                }
                // Asynchronous processing
                if (!participant.getConnection().isSynchronous()) {
                    // check if we need to respond to a request message
                    if (messageContext.getParticipant().getConnection().isReliable()) {
                        if (!headerInvalid && messageContext.getMessagePojo().getStatus() != MessageStatus.FAILED.getOrdinal()) {
                            LOG.trace(new LogMessage("No error response message found, creating ack", message));
                            responseMessageContext = null;
                            // generate ack for reliable connections
                            try {
                                responseMessageContext = protocolAdapter.createAcknowledgement(choreography, messageContext);
                            } catch (NexusException e) {
                                // Substituted an e.printStackTrace() by LOG.error().
                                // Do we need to do something else here? How about an error ack?
                                // Or let the exception raise up?
                                LOG.error(new LogMessage("Error creating acknowledgement", messageContext, e), e);
                            }
                        } else {
                            LOG.debug(new LogMessage("error response message found", message));
                            responseMessageContext = protocolAdapter
                                .createErrorAcknowledgement(Constants.ErrorMessageReasonCode.UNKNOWN, choreography, messageContext, errorMessages);
                        }
                        try {
                            LOG.debug(new LogMessage("dispatching response message", message));
                            MessageHandlingCenter.getInstance().processMessage(responseMessageContext);
                        } catch (NexusException e) {
                            LOG.error(new LogMessage("Unable to process Acknowledgement", message, e), e);
                        } finally {
                            // in asynchronous mode we have nothing to return in client thread
                            responseMessageContext = null;
                        }
                    } else if (!headerInvalid && messageContext.getMessagePojo().getStatus() != MessageStatus.FAILED.getOrdinal()) {
                        LOG.debug(new LogMessage("Not reliable, not creating ack - message ID: " + message.getMessageId(), message));
                        try {
                            messageContext.getStateMachine().receivedNonReliableMessage();
                        } catch (StateTransitionException e) {
                            LOG.warn(e.getMessage());
                        }
                    }
                }

                try {
                    // Forward message to FrontendActionSerializer for further processing/queueing
                    MessageHandlingCenter.getInstance().processMessage(messageContext);

                    // Block for synchronous processing
                    if (participant.getConnection().isSynchronous()) {
                        responseMessageContext = waitForSynchronousResponse(messageContext);
                        if (responseMessageContext.getConversation().getStatus() != Constants.CONVERSATION_STATUS_ERROR) {
                            try {
                                messageContext.getStateMachine().receivedNonReliableMessage();
                            } catch (StateTransitionException e) {
                                LOG.warn(e.getMessage());
                            }
                        }
                    }
                } catch (NexusException e) {
                    LOG.error(new LogMessage(
                        "Error processing message: " + message.getMessageId() + " (" + message.getConversation().getChoreography().getName() + "/" + message
                            .getConversation().getPartner().getPartnerId() + ")", message, e), e);
                    headerInvalid = true;
                }
            }
        } // no duplicate

        // return response (can be null in asynchronous mode)
        return responseMessageContext;
    }

    /**
     * Handles messages that are marked as requests.
     *
     * @param messageContext
     * @throws NexusException
     */
    private MessageContext handleRequest(MessageContext messageContext, boolean headerInvalid, ProtocolAdapter protocolAdapter, ChoreographyPojo choreography,
                                         List<ErrorDescriptor> errorMessages) throws NexusException {
        // request messages are not passed to backend
        try {
            messageContext.getStateMachine().receivedRequestMessage();
        } catch (StateTransitionException e) {
            throw new NexusException(new LogMessage(e.getMessage(), messageContext), e);
        }

        MessagePojo message = messageContext.getMessagePojo();
        MessageContext responseMessageContext = null;

        if (!headerInvalid && messageContext.getMessagePojo().getStatus() != MessageStatus.FAILED.getOrdinal()) {
            LOG.trace(new LogMessage("No error response message found, creating ack", message));
            // check if we need to respond to a request message
            responseMessageContext = protocolAdapter.createResponse(messageContext);
        } else {
            LOG.debug(new LogMessage("error response message found", message));
            responseMessageContext =
                protocolAdapter.createErrorAcknowledgement(Constants.ErrorMessageReasonCode.UNKNOWN, choreography, messageContext, errorMessages);
        }

        return responseMessageContext;
    }

    private MessageContext waitForSynchronousResponse(MessageContext messageContext) {
        MessageContext responseMessageContext = null;
        MessagePojo message = messageContext.getMessagePojo();

        Engine.getInstance().getTransactionService().addSynchronousRequest(message.getMessageId());
        while (responseMessageContext == null) {
            synchronized (synchronousReplies) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(new LogMessage("Waiting for synchronous reply...", message));
                    }
                    synchronousReplies.wait(); // wait on notification about available response
                } catch (InterruptedException e) {
                    LOG.warn(new LogMessage("Waiting for synchronous reply was interrupted", messageContext), e);
                }
            }
            //TODO we should check the replies first before we wait
            
            // Since the processing process uses notifyAll(),
            // we check whether there is a response for this thread.
            // If response is not present, the loop continues.
            responseMessageContext = synchronousReplies.get(messageContext.getMessagePojo().getMessageId());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(new LogMessage("Found reply for synchronous connection: " + responseMessageContext, message));
        }

        return responseMessageContext;
    }

    /**
     * Handles dispatching of received signal message of type ACKNOWLEDGMENT.
     *
     * @param messageContext
     * @throws NexusException
     */
    private void handleAcknowledgment(MessageContext messageContext) throws NexusException {
        MessagePojo message = messageContext.getMessagePojo();

        // duplicate?
        if (isDuplicate(message)) {
            LOG.info(new LogMessage("Received duplicate " + message.getTypeName() + ": " + message.getMessageId() + " for normal message: " + (
                message.getReferencedMessage() != null ? message.getReferencedMessage() : "n/a"), message));
        } else {
            MessagePojo referencedMessagePojo = message.getReferencedMessage();
            if (referencedMessagePojo != null) {
                try {
                    messageContext.getStateMachine().receivedAckMessage();
                } catch (StateTransitionException stex) {
                    LOG.warn(new LogMessage(stex.getMessage(), message));
                }

                propagateSignalMessageReceived(messageContext);

                // Trigger status update
                StatusUpdateSerializer statusUpdateSerializer = statusUpdateSerializers.get(message.getConversation().getChoreography().getName());
                if (statusUpdateSerializer != null) {
                    // Forward message to StatusUpdateSerializer for further processing/queueing
                    statusUpdateSerializer.processMessage(messageContext);
                }

            } else {
                LOG.error(
                    new LogMessage("Received " + MessagePojo.getTypeName(message.getType()) + " signal (" + message.getMessageId() + ") for unknown message",
                                   message));
            }
        }
    }

    /**
     * Handles dispatching of received signal message of type ERROR.
     *
     * @param messageContext
     * @throws NexusException
     */
    private void handleError(MessageContext messageContext) throws NexusException {
        MessagePojo message = messageContext.getMessagePojo();

        // duplicate?
        if (isDuplicate(message)) {
            LOG.info(new LogMessage("Received duplicate " + message.getTypeName() + ": " + message.getMessageId() + " for normal message: " + (
                message.getReferencedMessage() != null ? message.getReferencedMessage() : "n/a"), message));
        } else {
            MessagePojo referencedMessagePojo = message.getReferencedMessage();
            if (referencedMessagePojo != null) {
                try {
                    messageContext.getStateMachine().receivedErrorMessage();
                } catch (StateTransitionException stex) {
                    LOG.warn(new LogMessage(stex.getMessage(), message));
                }

                propagateSignalMessageReceived(messageContext);
            } else {
                LOG.error(new LogMessage("Received " + message.getTypeName() + " signal (" + message.getMessageId() + ") for unknown message", message));
            }
        }
    }

    /**
     * Check whether message was already received earlier.
     *
     * @param message
     * @return
     * @throws NexusException
     */
    private boolean isDuplicate(MessagePojo message) throws NexusException {
        MessageContext duplicateMessageContext = Engine.getInstance().getTransactionService().getMessageContext(message.getMessageId());
        return duplicateMessageContext != null;
    }

    /**
     * Notifies other nodes in cluster, and re-send schedulers about received signal message.
     *
     * @param messageContext
     */
    private void propagateSignalMessageReceived(MessageContext messageContext) {
        Engine.getInstance().getEngineController().getEngineControllerStub().broadcastAck(messageContext);
        Engine.getInstance().getTransactionService().deregisterProcessingMessage(messageContext.getMessagePojo().getReferencedMessage().getMessageId());
    }

    /**
     * Process a synchronous reply message.
     *
     * @param messageContext The synchronous reply message (not <code>null</code>).
     */
    public void processSynchronousReplyMessage(MessageContext messageContext) {

        synchronousReplies.put(messageContext.getMessagePojo().getMessageId(), messageContext);
        synchronized (synchronousReplies) {
            synchronousReplies.notifyAll();
        }
    } // processSynchronousReplyMessage

    /**
     * @param key
     * @return
     */
    public ProtocolAdapter getProtocolAdapterByKey(ProtocolSpecificKey key) {

        if (getProtocolAdapters() != null) {
            for (int i = 0; i < getProtocolAdapters().length; i++) {
                if (getProtocolAdapters()[i].getKey().equalsIgnoreTransport(key)) {
                    return getProtocolAdapters()[i];
                }
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.messaging.Dispatcher#getProtocolAdapters()
     */
    public ProtocolAdapter[] getProtocolAdapters() {

        return protocolAdapters;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.messaging.Dispatcher#setProtocolAdapters(org.nexuse2e.messaging.ProtocolAdapter[])
     */
    public void setProtocolAdapters(ProtocolAdapter[] protocolAdapter) {

        this.protocolAdapters = protocolAdapter;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {

        // check configuration
        if (protocolAdapters == null || protocolAdapters.length == 0) {
            // no protcoladpter set.
            status = BeanStatus.ERROR;
        }
        status = BeanStatus.INSTANTIATED;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#initialize()
     */
    public void initialize() {

        initialize(Engine.getInstance().getCurrentConfiguration());
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#initialize(org.nexuse2e.configuration.EngineConfiguration)
     */
    public void initialize(EngineConfiguration config) {

        LOG.trace("Initializing...");

        statusUpdateSerializers = config.getStatusUpdateSerializers();

        status = BeanStatus.INITIALIZED;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#teardown()
     */
    public void teardown() {

        LOG.trace("Freeing resources...");

        status = BeanStatus.INSTANTIATED;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#getRunLevel()
     */
    public Layer getActivationLayer() {

        return Layer.INBOUND_PIPELINES;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#start()
     */
    public void activate() {

        LOG.trace("activate");

    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#stop()
     */
    public void deactivate() {

        LOG.trace("deactivate");

    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#validate()
     */
    public boolean validate() {

        return false;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#getStatus()
     */
    public BeanStatus getStatus() {

        return status;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String name) {

        return (T) parameters.get(name);
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Configurable#getParameterMap()
     */
    @SuppressWarnings("unchecked")
    public Map<String, ParameterDescriptor> getParameterMap() {

        return Collections.EMPTY_MAP;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Configurable#getParameters()
     */
    public Map<String, Object> getParameters() {

        return Collections.unmodifiableMap(parameters);
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Configurable#setParameter(java.lang.String, java.lang.Object)
     */
    public void setParameter(String name, Object value) {

        parameters.put(name, value);
    }

    public boolean isForwardPipelet() {

        return forwardPipelet;
    }

    public boolean isFrontendPipelet() {

        return frontendPipelet;
    }

    public void setForwardPipelet(boolean isForwardPipelet) {

        forwardPipelet = isForwardPipelet;

    }

    public void setFrontendPipelet(boolean isFrontendPipelet) {

        frontendPipelet = isFrontendPipelet;

    }

    public Pipeline getPipeline() {

        return pipeline;
    }

    public void setPipeline(Pipeline pipeline) {

        this.pipeline = pipeline;
    }
} // FrontendInboundDispatcher

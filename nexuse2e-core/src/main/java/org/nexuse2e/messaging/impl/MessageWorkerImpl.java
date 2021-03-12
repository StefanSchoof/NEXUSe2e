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
package org.nexuse2e.messaging.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.nexuse2e.Engine;
import org.nexuse2e.MessageStatus;
import org.nexuse2e.NexusException;
import org.nexuse2e.controller.StateTransitionException;
import org.nexuse2e.controller.TransactionService;
import org.nexuse2e.logging.LogMessage;
import org.nexuse2e.messaging.MessageContext;
import org.nexuse2e.messaging.MessageWorker;
import org.nexuse2e.pojo.ActionPojo;
import org.nexuse2e.pojo.ConversationPojo;
import org.nexuse2e.pojo.MessagePojo;
import org.nexuse2e.pojo.ParticipantPojo;

/**
 * This class implements <code>Runnable</code> in order to asynchronously process inbound/outbound
 * messages.
 * <p>
 * For client code, only the method {@link #queue(MessageContext)} is relevant.
 * 
 * @author sschulze, jreese
 */
public class MessageWorkerImpl implements MessageWorker {

    private static final Logger LOG = Logger.getLogger(MessageWorkerImpl.class);

    /**
     * Default thread pool size.
     */
    public static final int DEFAULT_THREAD_POOL_SIZE = 10;
    
    private ScheduledExecutorService threadPool = null;

    /**
     * Constructs a new <code>ConversationWorkerImpl</code> with the given thread pool size.
     * @param threadPoolSize The thread pool size.
     * @throw IllegalArgumentException if the thread pool size is &lt;= 0.
     */
    public MessageWorkerImpl(int threadPoolSize) {
        threadPool = Executors.newScheduledThreadPool(threadPoolSize);
    }
    
    /**
     * Constructs a new <code>ConversationWorkerImpl</code> with the default thread pool size.
     */
    public MessageWorkerImpl() {
        this(DEFAULT_THREAD_POOL_SIZE);
    }
    
    /**
     * Adds a message to the message scheduler.
     * @param messageContext The message context to be queued. Must not be <code>null</code>.
     */
    public void queue(MessageContext messageContext) {
        queue(messageContext, 0, false);
    }
    
    private void queue(MessageContext messageContext, int initialDelay, boolean updateMessageContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(new LogMessage("Created new conversation worker", messageContext));
        }
        // queue msg
        if (LOG.isDebugEnabled()) {
            LOG.debug(new LogMessage("Queuing message", messageContext));
        }

        // init execution of worker
        ConversationWorkerRunnable worker = new ConversationWorkerRunnable(messageContext);
        worker.initialDelay = initialDelay;
        worker.updateMessageContext = updateMessageContext;
        worker.retries = 0;
        worker.interval = org.nexuse2e.messaging.Constants.DEFAULT_MESSAGE_INTERVAL;
        worker.reliable = messageContext.getParticipant().getConnection().isReliable();
        
        // none ack messages must be requeued if properly configured.
        if (!messageContext.getMessagePojo().isAck() ) { // && worker.reliable
            ParticipantPojo participantPojo = messageContext.getMessagePojo().getParticipant();
            if (participantPojo != null) {
                worker.retries = participantPojo.getConnection().getRetries();
                worker.interval = participantPojo.getConnection().getMessageInterval();
            }
        }

        if (messageContext.getMessagePojo().isOutbound() && worker.retries >= 0) {
            worker.handle = threadPool.scheduleWithFixedDelay(worker, initialDelay, worker.interval, TimeUnit.SECONDS);
            Engine.getInstance().getTransactionService().registerProcessingMessage(messageContext.getMessagePojo(), worker.handle);
        } else {
            worker.handle = threadPool.schedule(worker, initialDelay, TimeUnit.SECONDS);
        }
    }

    class ConversationWorkerRunnable implements Runnable {
        private ScheduledFuture<?> handle;
        private int retries;
        private int interval;
        private boolean reliable;
        private boolean updateMessageContext;
        private int initialDelay;
        private MessageContext messageContext;

        ConversationWorkerRunnable(MessageContext messageContext) {
            this.messageContext = messageContext;
        }
        
        public void run() {
            if (updateMessageContext) {
                try {
                    boolean firstTimeInQueue = messageContext.isFirstTimeInQueue();
                    ActionPojo currentAction = messageContext.getConversation().getCurrentAction();
                    messageContext = Engine.getInstance().getTransactionService().getMessageContext(messageContext.getMessagePojo().getMessageId());
                    messageContext.setFirstTimeInQueue(firstTimeInQueue); // restore previous firstTimeInQueue flag, since this is in-memory only
                    messageContext.getConversation().setCurrentAction(currentAction); // restore current action, since this was updated in the old MessageContext
                } catch (NexusException e) {
                    LOG.warn("Error creating new MessageContext on re-processing, recycling old MessageContext", e);
                }
            }
            updateMessageContext = true;
            
            if (LOG.isDebugEnabled()) {
                LOG.debug(new LogMessage("Starting message processing... ", messageContext));
            }
            // process
            if (messageContext.getMessagePojo().isOutbound()) {
                // process outbound
                processOutbound(messageContext);
            } else {
                // process inbound
                processInbound(messageContext);
            }
        }
    
        protected void processInbound(MessageContext messageContext) {
    
            if (LOG.isDebugEnabled()) {
                LOG.debug(new LogMessage("Processing inbound message..." + messageContext.getStateMachine().toString(), messageContext));
            }
    
            // Initiate the backend process
            try {
                Engine.getInstance().getCurrentConfiguration().getStaticBeanContainer().getBackendInboundDispatcher().processMessage(messageContext);
                messageContext.getStateMachine().processedBackend();
    
            } catch (NexusException nex) {
                LOG.error(new LogMessage("Error processing backend", messageContext, nex), nex);
                try {
                    messageContext.getStateMachine().processingFailed();
                } catch (StateTransitionException e) {
                    LOG.warn(new LogMessage(e.getMessage(), messageContext));
                } catch (NexusException e) {
                    LOG.error(new LogMessage("Error while setting conversation status to ERROR", messageContext, e), e);
                }
            } catch (StateTransitionException stex) {
                LOG.warn(new LogMessage(stex.getMessage(), messageContext));
            }

            if (messageContext.getParticipant().getConnection().isSynchronous()) {
                Engine.getInstance().getCurrentConfiguration().getStaticBeanContainer().getFrontendInboundDispatcher()
                        .processSynchronousReplyMessage(messageContext);
                Engine.getInstance().getTransactionService()
                        .removeSynchronousRequest(messageContext.getMessagePojo().getMessageId());
            }
        }
    
        protected void processOutbound(MessageContext messageContext) {
            
            TransactionService transactionService = Engine.getInstance().getTransactionService();
            
            // check if ack message for previous step is pending, so this message needs to be sent later
            if (reliable && !messageContext.getMessagePojo().isAck()) {
                ConversationPojo conv = messageContext.getConversation();
                if (conv != null && conv.getMessages() != null) {
                    for (MessagePojo m : conv.getMessages()) {
                        if (m.isAck() && m.isOutbound() && m.getStatus() == MessageStatus.QUEUED.getOrdinal()) {
                            if (initialDelay < 25) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.info(new LogMessage(
                                            "Not sending " + messageContext.getMessagePojo().getTypeName() +
                                            " message now, outbound ack for previous choreography action still in QUEUED. " +
                                            "Requeueing this message in " + (initialDelay + 1) + " s.", messageContext));
                                }
                                transactionService.deregisterProcessingMessage(messageContext.getMessagePojo().getMessageId());
                                queue(messageContext, initialDelay + 1, true);
                                return;
                            } else {
                                LOG.error(new LogMessage("Message from previous choroegraphy step still in QUEUED, but re-queueing attempts exhausted", messageContext));
                                transactionService.deregisterProcessingMessage(messageContext.getMessagePojo().getMessageId());
                                return;
                            }
                        }
                    }
                }
            }
            
            if (LOG.isDebugEnabled()) {
                LOG.debug(new LogMessage("Processing outbound message...", messageContext));
            }
    
            try {
                if (transactionService.isSynchronousReply(messageContext.getMessagePojo().getMessageId())) {
                    Engine.getInstance().getCurrentConfiguration().getStaticBeanContainer().getFrontendInboundDispatcher().processSynchronousReplyMessage(messageContext);
                    transactionService.removeSynchronousRequest(messageContext.getMessagePojo().getMessageId());
                } else {
                    Engine.getInstance().getCurrentConfiguration().getStaticBeanContainer().getFrontendOutboundDispatcher().processMessage(messageContext);
                }
            } catch (NexusException e) {
                LOG.error(new LogMessage("OutboundQueueListener.run() detected an exception", messageContext, e), e);
            }
        }
    }
}

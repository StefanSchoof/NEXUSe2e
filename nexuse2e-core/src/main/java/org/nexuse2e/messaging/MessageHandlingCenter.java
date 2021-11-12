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
/**
 * 
 */
package org.nexuse2e.messaging;

import org.apache.log4j.Logger;
import org.nexuse2e.Engine;
import org.nexuse2e.MessageStatus;
import org.nexuse2e.NexusException;
import org.nexuse2e.controller.StateTransitionException;
import org.nexuse2e.logging.LogMessage;

/**
 * Central entity that organizes serialized processing of inbound, and outbound
 * messages that belong to the same conversation. All conversations will be
 * processed in parallel.
 * 
 * @author sschulze
 */
public class MessageHandlingCenter implements MessageProcessor {

    private static final Logger LOG = Logger.getLogger( MessageHandlingCenter.class );

    private static MessageHandlingCenter instance;

    private MessageHandlingCenter() {
        super();
    }

    /**
     * Singleton instance.
     * 
     * @return singleton instance.
     */
    public static synchronized MessageHandlingCenter getInstance() {
        if ( instance == null ) {
            instance = new MessageHandlingCenter();
        }
        return instance;
    }

    /**
     * Queues the given message for inbound, or outbound processing. I.e. the
     * state machine transition is performed, the {@link MessageContext} gets
     * persisted, and the message gets appended to the queue. If a message is
     * already in the QUEUED state (most likely if
     * {@link MessageContext#announceQueuing(MessageContext)} was called
     * before), the state machine transition will not be triggered again.
     * 
     * @param messageContext
     * @return The given reference from the input parameter. It's state will
     *         most likely be modified during state machine transition.
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws NexusException
     */
    public MessageContext processMessage( MessageContext messageContext ) throws IllegalStateException, NexusException {

        if (messageContext.getMessagePojo().getStatus() != MessageStatus.QUEUED.getOrdinal() &&
                messageContext.getMessagePojo().getStatus() != MessageStatus.PROCESSED.getOrdinal()) {
            try {
                messageContext.getStateMachine().queueMessage();
            } catch ( StateTransitionException e ) {
                LOG.warn( new LogMessage( e.getMessage(), messageContext ) );
            }
        }

        if (messageContext != null) {
            queue( messageContext );
        }

        return messageContext;
    }

    /**
     * Append the message to the queue, if the participant's connection is not marked as hold.
     * "Hold" connections indicate that the participant polls outbound messages.
     * So we do not queue them for sending. They just remain in the database as QUEUED until the participant issues a request.
     * <p>
     * This method does not perform any state machine transitions.
     * @param messageContext The message context to be added to the queue.
     */
    protected void queue( MessageContext messageContext ) {
        if (!(messageContext.getMessagePojo().isOutbound() && messageContext.getParticipant().getConnection().isHold())) {
            Engine.getInstance().getMessageWorker().queue( messageContext );
        }
    }

    /**
     * Gathers a {@link MessageContext} from the persistent store, and
     * initializes a re-queue.
     *
     * @param messageId
     * @throws NexusException
     */
    public void requeueMessage(
            String messageId) throws NexusException {

        MessageContext messageContext = Engine.getInstance().getTransactionService().getMessageContext(messageId);
        requeueMessage(messageContext);
    }

    /**
     * Re-queues a message.
     * 
     * @param messageContext
     * @throws NexusException
     */
    public void requeueMessage( MessageContext messageContext ) throws NexusException {

        if ( messageContext != null ) {
            // if message is processing, cancel it
            String messageId = messageContext.getMessagePojo().getMessageId();
            if ( Engine.getInstance().getTransactionService().isProcessingMessage( messageId ) ) {
                Engine.getInstance().getTransactionService().deregisterProcessingMessage( messageId );
            }
            // set message end date to null, otherwise it's processing may be
            // cancelled
            if ( messageContext.getMessagePojo() != null ) {
                messageContext.getMessagePojo().setEndDate( null );
            }

            if ( LOG.isDebugEnabled() ) {
                LOG.debug( new LogMessage( "Re-queuing message " + messageContext.toString() ) );
            }

            // queue message
            try {
                messageContext.getStateMachine().queueMessage(true);
            } catch ( StateTransitionException e ) {
                LOG.warn( new LogMessage( e.getMessage(), messageContext ) );
            }
            messageContext.setFirstTimeInQueue(true); // mark re-queued
            queue( messageContext );
        } else {
            LOG.error( "Cannot requeue message with messageContext = null" );
            throw new NexusException("Cannot requeue message with messageContext = null");
        }
    }
}
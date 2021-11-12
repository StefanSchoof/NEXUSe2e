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

import org.nexuse2e.NexusException;
import org.nexuse2e.controller.StateTransitionException;
import org.nexuse2e.pojo.ConversationPojo;
import org.nexuse2e.pojo.MessagePojo;

/**
 * This interface defines all transition methods for a single conversation's state machine.
 * Implementations shall verify the validity and the execution of state transitions in a thread-safe way.
 *
 * @author Jonas Reese
 * @version $LastChangedRevision:  $ - $LastChangedDate:  $ by $LastChangedBy:  $
 */
public interface ConversationStateMachine {

    /**
     * Enumeration of all transitions of the {@link ConversationStateMachine} state machine. 
     * @author Sebastian Schulze
     * @date 16.09.2010
     */
    public enum StateTransition {
        PROCESSED_BACKEND,
        PROCESSING_FAILED,
        QUEUE_MESSAGE,
        RECEIVED_ACK,
        RECEIVED_ERROR,
        RECEIVED_NON_RELIABLE_MESSAGE,
        RECEIVED_REQUEST,
        SENT_MESSAGE
    }

    /**
     * Initilize this <code>ConversationStateMachine</code> with the given parameters.
     * @param conversation The conversation.
     * @param message The message.
     * @param reliable <code>true</code> if reliable messaging is enabled, <code>false</code> otherwise.
     */
    public abstract void initialize(ConversationPojo conversation, MessagePojo message, boolean reliable);
    
    /**
     * Register a job that will be executed only once the specified {@link StateTransition} is performed next.
     * @param transition The transition to be notified of.
     * @param action The action to be executed.
     */
    public abstract void registerStateTransitionJob(StateTransition transition, StateTransitionJob action);

    /**
     * Gets the conversation that is associated with this <code>ConversationStateMachine</code>.
     * @return The conversation.
     */
    public abstract ConversationPojo getConversation();

    /**
     * Puts the conversation to AWAITING_ACK state if necessary. This will be done for oubound normal
     * messages on reliable conversations. This method shall be called for all messages before processing
     * them through the frontend pipeline.
     * @throws StateTransitionException
     * @throws NexusException
     */
    public abstract void sendingMessage() throws StateTransitionException, NexusException;
    
    /**
     * Puts a message to PROCESSED state (except for normal outbound messages on reliable conversations which are put
     * to PROCESSED by the {@link #receivedAckMessage()} transition). Updates the conversation state if necessary.
     * @throws StateTransitionException If the state transition is illegal.
     * @throws NexusException If the transition could not be performed due to a system-specific error.
     */
    public abstract void sentMessage() throws StateTransitionException, NexusException;

    /**
     * Puts an inbound request message to PROCESSED state.
     * @throws StateTransitionException If the state transition is illegal.
     * @throws NexusException If the transition could not be performed due to a system-specific error.
     */
    public abstract void receivedRequestMessage() throws StateTransitionException, NexusException;

    /**
     * Puts a non-reliable inbound message to PROCESSED state
     * @throws StateTransitionException If the state transition is illegal.
     * @throws NexusException If the transition could not be performed due to a system-specific error.
     */
    public abstract void receivedNonReliableMessage() throws StateTransitionException, NexusException;

    /**
     * Puts an ACK message to PROCESSED state and sets the referenced message's end date.
     * Updates the conversation state if necessary.
     * @throws StateTransitionException If the state transition is illegal.
     * @throws NexusException If the transition could not be performed due to a system-specific error.
     */
    public abstract void receivedAckMessage() throws StateTransitionException, NexusException;

    /**
     * Puts an inbound error message to PROCESSED state.
     * @throws StateTransitionException If the state transition is illegal.
     * @throws NexusException If the transition could not be performed due to a system-specific error.
     */
    public abstract void receivedErrorMessage() throws StateTransitionException, NexusException;

    /**
     * Updates the conversation state after backend has been processed.
     * @throws StateTransitionException If the state transition is illegal.
     * @throws NexusException If the transition could not be performed due to a system-specific error.
     */
    public abstract void processedBackend() throws StateTransitionException, NexusException;

    /**
     * Updates the conversation and message state after processing has failed.
     * @throws StateTransitionException If the state transition is illegal.
     * @throws NexusException If the transition could not be performed due to a system-specific error.
     */
    public abstract void processingFailed() throws StateTransitionException, NexusException;

    /**
     * Updates the conversation and message state after processing backend has failed.
     * @throws StateTransitionException If the state transition is illegal.
     * @throws NexusException If the transition could not be performed due to a system-specific error.
     */
    public abstract void processingBackendFailed() throws StateTransitionException, NexusException;

    /**
     * Sets the current conversation message to <code>QUEUED</code> without forcing
     * a state transition.
     * @throws StateTransitionException if the state transition is invalid.
     * @throws NexusException if another error occurred (e.g. on the persistence layer).
     */
    public abstract void queueMessage() throws StateTransitionException, NexusException;

    /**
     * Sets the current conversation message to <code>QUEUED</code>. If the message starts a new
     * choreography action, the conversation is updated accordingly.
     * @param force If <code>true</code>, indicates that the message shall be queued with no
     * respect to state maintenance.
     * @throws StateTransitionException if the state transition is invalid. Not thrown if
     * <code>force</code> is <code>true</code>.
     * @throws NexusException if another error occurred (e.g. on the persistence layer).
     */
    public abstract void queueMessage(boolean force) throws StateTransitionException, NexusException;

}

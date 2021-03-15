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

import java.io.Serializable;
import java.util.ArrayList;

import org.nexuse2e.ActionSpecificKey;
import org.nexuse2e.Engine;
import org.nexuse2e.ProtocolSpecificKey;
import org.nexuse2e.pojo.ChoreographyPojo;
import org.nexuse2e.pojo.ConversationPojo;
import org.nexuse2e.pojo.MessagePojo;
import org.nexuse2e.pojo.ParticipantPojo;
import org.nexuse2e.pojo.PartnerPojo;

/**
 * Wrapper class that combines a message with additional meta data required for processing.
 *
 * @author mbreilmann
 */
public class MessageContext implements Serializable {

    /**
     * 
     */
    private static final long   serialVersionUID    = 5184344055202810866L;
    private MessagePojo         messagePojo         = null;
    private MessagePojo         originalMessagePojo = null;
    private Object              data                = null;
    
    private Object              routingData         = null;
    
    private ProtocolSpecificKey protocolSpecificKey = null;
    private ActionSpecificKey   actionSpecificKey   = null;

    private ChoreographyPojo    choreography        = null;
    private PartnerPojo         partner             = null;
    private ParticipantPojo     participant         = null;
    private ConversationPojo    conversation        = null;

    private ArrayList<ErrorDescriptor> errors       = null;
    private Object synchronusBackendResponse        = null;
    
    private transient ConversationStateMachine conversationStateMachine = null;
    private transient boolean                  processThroughReturnPipeline = true;
    private transient MessageContext           requestMessage = null;
    private transient RequestInfo              requestInfo = null;
    private transient boolean                  firstTimeInQueue = true;
    
    public MessageContext() {
        
    }
    
    /**
     * @return the data
     */
    public Object getData() {

        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData( Object data ) {

        this.data = data;
    }

    /**
     * @return the inboundMessagePojo
     */
    public MessagePojo getMessagePojo() {

        return messagePojo;
    }

    /**
     * @param inboundMessagePojo the inboundMessagePojo to set
     */
    public void setMessagePojo( MessagePojo messagePojo ) {

        this.messagePojo = messagePojo;
    }

    /**
     * @return the actionSpecificKey
     */
    public ActionSpecificKey getActionSpecificKey() {

        return actionSpecificKey;
    }

    /**
     * @param actionSpecificKey the actionSpecificKey to set
     */
    public void setActionSpecificKey( ActionSpecificKey actionSpecificKey ) {

        this.actionSpecificKey = actionSpecificKey;
    }

    /**
     * @return the protocolSpecificKey
     */
    public ProtocolSpecificKey getProtocolSpecificKey() {

        return protocolSpecificKey;
    }

    /**
     * @param protocolSpecificKey the protocolSpecificKey to set
     */
    public void setProtocolSpecificKey( ProtocolSpecificKey protocolSpecificKey ) {

        this.protocolSpecificKey = protocolSpecificKey;
    }

    /**
     * @return the choreography
     */
    public ChoreographyPojo getChoreography() {

        return choreography;
    }

    /**
     * @param choreography the choreography to set
     */
    public void setChoreography( ChoreographyPojo choreography ) {

        this.choreography = choreography;
    }

    /**
     * @return the communicationPartner
     */
    public PartnerPojo getPartner() {

        return partner;
    }

    /**
     * Sets the communication partner.
     * @param partner The partner to set.
     */
    public void setPartner( PartnerPojo partner ) {

        this.partner = partner;
    }

    /**
     * @param communicationPartner the communicationPartner to set
     */
    public void setCommunicationPartner( PartnerPojo partner ) {

        this.partner = partner;
    }

    /**
     * @return the conversation
     */
    public ConversationPojo getConversation() {

        return conversation;
    }

    /**
     * @param conversation the conversation to set
     */
    public void setConversation( ConversationPojo conversation ) {

        this.conversation = conversation;
    }

    /**
     * @return the participant
     */
    public ParticipantPojo getParticipant() {

        return participant;
    }

    /**
     * @param participant the participant to set
     */
    public void setParticipant( ParticipantPojo participant ) {

        this.participant = participant;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {

        MessageContext clone = new MessageContext();
        clone.setActionSpecificKey( this.getActionSpecificKey() );
        clone.setChoreography( this.getChoreography() );
        clone.setCommunicationPartner( this.getPartner() );
        clone.setConversation( this.getConversation() );
        clone.setData( this.getData() );
        clone.setMessagePojo( this.getMessagePojo() );
        clone.setOriginalMessagePojo( this.getOriginalMessagePojo() );
        clone.setParticipant( this.getParticipant() );
        clone.setProtocolSpecificKey( this.getProtocolSpecificKey() );
        clone.setRequestInfo(this.getRequestInfo());
        clone.setRoutingData(this.getRoutingData());
        clone.setErrors(this.getErrors() == null ? null : new ArrayList<ErrorDescriptor>(this.getErrors()));
        clone.setRequestMessage(this.getRequestMessage());
        clone.setFirstTimeInQueue(this.isFirstTimeInQueue());
        return clone;
    }

    public MessagePojo getOriginalMessagePojo() {

        return originalMessagePojo;
    }

    public void setOriginalMessagePojo( MessagePojo originalMessagePojo ) {

        this.originalMessagePojo = originalMessagePojo;
    }

    
    /**
     * @return the routingData
     */
    public Object getRoutingData() {
    
        return routingData;
    }

    
    /**
     * @param routingData the routingData to set
     */
    public void setRoutingData( Object routingData ) {
    
        this.routingData = routingData;
    }

    public void addError(ErrorDescriptor error) {
        if(errors == null) {
            errors = new ArrayList<ErrorDescriptor>();
        }
        errors.add( error );
    }
    
    /**
     * @return the errors
     */
    public ArrayList<ErrorDescriptor> getErrors() {
    
        return errors;
    }

    
    /**
     * @param errors the errors to set
     */
    public void setErrors( ArrayList<ErrorDescriptor> errors ) {
    
        this.errors = errors;
    }
    
    /**
     * Sets the <code>processThroughReturnPipeline</code> flag. Default is <code>true</code>.
     * This flag can be used to mark messages on bidirectional transport mechanisms where the
     * returned message shall be ignored by the return pipeline (e.g., if it is an acknowledgement
     * or a message that does not have any meaning at all).
     * @param processThroughReturnPipeline If <code>true</code>, indicates that this message context
     * shall be processed through a return pipeline if present. Otherwise, it will not be passed
     * to any return pipelets.
     */
    public void setProcessThroughReturnPipeline( boolean processThroughReturnPipeline ) {
        this.processThroughReturnPipeline = processThroughReturnPipeline;
    }
    
    /**
     * Gets the <code>processThroughReturnPipeline</code> flag. Default is <code>true</code>.
     * @return The flag. See {@link #setProcessThroughReturnPipeline(boolean)} for details.
     */
    public boolean isProcessThroughReturnPipeline() {
        return processThroughReturnPipeline;
    }

    /**
     * Gets the <code>ConversationStateMachine</code> that shall be used for
     * state transitions.
     * @return The <code>ConversationStateMachine</code>.
     */
    public ConversationStateMachine getStateMachine() {
        if (conversationStateMachine == null) {
            conversationStateMachine =  new org.nexuse2e.messaging.impl.ConversationStateMachineImpl();
            conversationStateMachine.initialize(conversation, messagePojo, participant.getConnection().isReliable());
        }
        return conversationStateMachine;
    }
    
    /**
     * Determines if this context describes a request message. Please note that a
     * <code>MessageContext</code> is marked as a non-request message until a
     * <code>RequestInfo</code> object is set by calling {@link #setRequestInfo(RequestInfo)}.
     * This will typically be done by a frontend pipelet. 
     * @return <code>true</code> if this is a message that contains a document request instead of
     * a business document itself. If <code>true</code> is returned, {@link #getRequestInfo()} will
     * not return <code>null</code>, otherwise that method returns <code>null</code>.
     */
    public boolean isRequestMessage() {
        return (requestInfo != null);
    }
    
    /**
     * Sets the request information on this <code>MessageContext</code> and marks this
     * as a request message if <code>requestInfo</code> is not <code>null</code>.
     * @param requestInfo The request info, or <code>null</code> if this does not denote a
     * request message.
     */
    public void setRequestInfo( RequestInfo requestInfo ) {
        this.requestInfo = requestInfo;
    }
    
    /**
     * Gets the request information available on this <code>MessageContext</code>.
     * @return The request info, or <code>null</code> if this is not determined to denote a
     * request message.
     */
    public RequestInfo getRequestInfo() {
        return requestInfo;
    }

    /**
     * Determines if this <code>MessageContext</code> contains a response to a (synchronous)
     * request. This method will return <code>true</code> if this message contains a request
     * message, meaning that {@link #getRequestMessage()} does not return <code>null</code>.
     * @return <code>true</code> if and only if this is a response to a request message.
     */
    public boolean isResponseMessage() {
        return requestMessage != null;
    }
    
    /**
     * Gets the request message that is associated with this (response) message.
     * @return The associated request, or <code>null</code> if this message is not a response
     * to a (synchronous) request.
     */
    public MessageContext getRequestMessage() {
        return requestMessage;
    }
    
    /**
     * Sets the <code>responseMessage</code> flag.
     * @param responseMessage <code>true</code> if and only if this is a response to a request message.
     */
    public void setRequestMessage( MessageContext requestMessage ) {
        this.requestMessage = requestMessage;
    }

    /**
     * convenience method for character encoding
     * @return
     */
    public String getEncoding() {
    	if(getParticipant() != null ) {
    		return getParticipant().getDefaultEncoding();
    	} else {
    		return Engine.getInstance().getDefaultCharEncoding();
    	}
    }

    /**
     * Determines if the message associated with this <code>MessageContext</code> has been
     * queued for the first time. This parameter causes outbound messages to be marked with
     * retry count 0 after the first sending attempt. 
     * @return <code>true</code> if the message has been queued for the first time. 
     */
    public boolean isFirstTimeInQueue() {
        return firstTimeInQueue;
    }

    /**
     * Sets the indicator for the first-time message queueing. 
     * @param firstTimeInQueue If <code>true</code>, this message will be marked with retry
     * count 0 after the first sending attempt. Otherwise, the message retry count will be
     * incremented.
     */
    public void setFirstTimeInQueue(boolean firstTimeInQueue) {
        this.firstTimeInQueue = firstTimeInQueue;
    }

    public Object getSynchronusBackendResponse() {
        return synchronusBackendResponse;
    }

    public void setSynchronusBackendResponse(Object synchronusBackendResponse) {
        this.synchronusBackendResponse = synchronusBackendResponse;
    }
} // MessageContext

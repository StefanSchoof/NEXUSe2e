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
package org.nexuse2e.messaging.wsaggateway;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.nexuse2e.ActionSpecificKey;
import org.nexuse2e.Engine;
import org.nexuse2e.MessageStatus;
import org.nexuse2e.NexusException;
import org.nexuse2e.ProtocolSpecificKey;
import org.nexuse2e.configuration.NexusUUIDGenerator;
import org.nexuse2e.controller.StateTransitionException;
import org.nexuse2e.controller.TransactionService;
import org.nexuse2e.messaging.Constants;
import org.nexuse2e.messaging.ErrorDescriptor;
import org.nexuse2e.messaging.MessageContext;
import org.nexuse2e.messaging.RequestInfo;
import org.nexuse2e.messaging.Constants.ErrorMessageReasonCode;
import org.nexuse2e.pojo.ActionPojo;
import org.nexuse2e.pojo.ChoreographyPojo;
import org.nexuse2e.pojo.FollowUpActionPojo;
import org.nexuse2e.pojo.MessagePayloadPojo;
import org.nexuse2e.pojo.MessagePojo;

/**
 * <code>ProtocolAdapter</code> implementation for the AgGateway web service transport.
 *
 * @author Jonas Reese
 * @version $LastChangedRevision:  $ - $LastChangedDate:  $ by $LastChangedBy:  $
 */
public class ProtocolAdapter implements org.nexuse2e.messaging.ProtocolAdapter {
    private static Logger       LOG = Logger.getLogger( ProtocolAdapter.class );

    
    private ProtocolSpecificKey key;
    
    public void addProtcolSpecificParameters( MessageContext messageContext ) {
        // nothing to do
    }

    public MessageContext createAcknowledgement(
            ChoreographyPojo choreography, MessageContext messageContext ) throws NexusException {

        // not supported by AgGateway WS
        return null;
    }

    public MessageContext createErrorAcknowledgement(
            ErrorMessageReasonCode reasonCode, ChoreographyPojo choreography,
            MessageContext messageContext, List<ErrorDescriptor> errorMessages) {

        // not supported by AgGateway WS
        return null;
    }

    public MessageContext createResponse( MessageContext messageContext ) throws NexusException {

        RequestInfo requestInfo = messageContext.getRequestInfo();
        if (requestInfo == null) { // shouldn't happen, just to make sure...
            return null;
        }
        if (requestInfo.getDocumentType() == null) {
            LOG.error( "Invalid request message: Requested document type is empty or could not be resolved" );
            return null;
        }
        
        ActionPojo action = null;
        Set<FollowUpActionPojo> followUpActions = messageContext.getConversation().getCurrentAction().getFollowUpActions();
        if (followUpActions.isEmpty()) {
            // no follow-up action found
            LOG.warn( "No follow-up action found for action " +
                    messageContext.getConversation().getCurrentAction().getName() +
                    ", cannot respond to request for document type " +
                    requestInfo.getDocumentType() );
            return null;
        } else if (followUpActions.size() == 1) {
            // if exactly one follow up action is defined, use it
            action = followUpActions.iterator().next().getFollowUpAction();
        } else {
            // try to find follow up action with same document type
            for (FollowUpActionPojo fua : followUpActions) {
                if (requestInfo.getDocumentType().equals( fua.getFollowUpAction().getDocumentType() )) {
                    action = fua.getFollowUpAction();
                    break;
                }
            }
        }
        if (action == null) {
            LOG.warn( "More than one follow-up action defined for action " +
                    messageContext.getConversation().getCurrentAction().getName() +
                    ", please set document type of " + requestInfo.getDocumentType() +
                    " on the one you want to set as response action for this action. " +
                    "Not responding to request." );
            return null;
        }

        TransactionService ts = Engine.getInstance().getTransactionService();
        List<MessagePojo> messages = ts.getMessagesByActionPartnerDirectionAndStatus(
                action, messageContext.getPartner(), true, MessageStatus.QUEUED.getOrdinal(), 0, false );

        // mark messages as 'sent'
        for (MessagePojo m : messages) {
            // create a MessageContext for message in order to access it's state machine
            MessageContext dummy = new MessageContext();
            dummy.setConversation( m.getConversation() );
            dummy.setMessagePojo( m );
            dummy.setParticipant( m.getParticipant() );
            try {
                dummy.getStateMachine().sentMessage();
            } catch (StateTransitionException e) {
                LOG.error( "Error while trying to mark hold message as 'sent'", e );
                // we don't throw this with a NexusException, because
                // if we already processed messages that were on hold
                // we need to push them down the return pipeline (otherwise
                // they are marked as 'sent' without being sent)
            }
        }
        
        // go through messages that are on 'hold' and collect payloads
        MessagePojo message = new MessagePojo();
        List<MessagePayloadPojo> payloads = new ArrayList<MessagePayloadPojo>();
        for (MessagePojo m : messages) {
            m.setStatus( MessageStatus.PROCESSED.getOrdinal() );
            m.setEndDate( new Date());
            for (MessagePayloadPojo payload : m.getMessagePayloads()) {
                try {
                    MessagePayloadPojo copy = (MessagePayloadPojo) payload.clone();
                    copy.setMessage( message );
                    payloads.add( copy );
                } catch (CloneNotSupportedException cnsex) {
                    LOG.error( "Unexpected error:", cnsex );
                }
            }
        }
        messageContext.getConversation().setCurrentAction( action );
        message.setMessagePayloads( payloads );
        message.setAction( action );
        message.setConversation( messageContext.getMessagePojo().getConversation() );
        message.getConversation().getMessages().add( message );
        message.setMessageId( new NexusUUIDGenerator().getId() );
        message.setModifiedDate( new Date() );
        message.setOutbound( true );
        message.setTRP( messageContext.getMessagePojo().getTRP() );
        message.setStatus( MessageStatus.QUEUED.getOrdinal() );
        message.setType( Constants.INT_MESSAGE_TYPE_NORMAL );
        message.setReferencedMessage( messageContext.getMessagePojo() );

        ActionSpecificKey actionSpecificKey = new ActionSpecificKey();
        actionSpecificKey.setActionId( action.getName() );
        actionSpecificKey.setChoreographyId( action.getChoreography().getName() );
        
        MessageContext response = new MessageContext();
        response.setRequestMessage( messageContext );
        response.setActionSpecificKey( actionSpecificKey );
        response.setChoreography( action.getChoreography() );
        response.setPartner( messageContext.getPartner() );
        response.setConversation( messageContext.getConversation() );
        response.setMessagePojo( message );
        response.setOriginalMessagePojo( message );
        response.setParticipant( messageContext.getParticipant() );
        response.setProtocolSpecificKey( messageContext.getProtocolSpecificKey() );
        response.setRequestInfo( messageContext.getRequestInfo() );
        
        return response;
    }

    public ProtocolSpecificKey getKey() {
        return key;
    }

    public void setKey( ProtocolSpecificKey key ) {
        this.key = key;
    }

}

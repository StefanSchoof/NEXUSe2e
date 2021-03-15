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
import org.nexuse2e.ActionSpecificKey;
import org.nexuse2e.BeanStatus;
import org.nexuse2e.Engine;
import org.nexuse2e.Layer;
import org.nexuse2e.Manageable;
import org.nexuse2e.NexusException;
import org.nexuse2e.configuration.EngineConfiguration;
import org.nexuse2e.logging.LogMessage;
import org.nexuse2e.pojo.ActionPojo;
import org.nexuse2e.pojo.ChoreographyPojo;
import org.nexuse2e.pojo.ConversationPojo;
import org.nexuse2e.util.NexusThreadStorage;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;

/**
 * Component dispatching inbound messages to the correct pipeline based on their choreography.
 *
 * @author gesch
 */
public class BackendInboundDispatcher implements InitializingBean, Manageable {

    private static Logger                               LOG                     = Logger
                                                                                        .getLogger( BackendInboundDispatcher.class );

    private BeanStatus                                  status                  = BeanStatus.UNDEFINED;

    private Map<ActionSpecificKey, BackendPipeline> backendInboundPipelines = new HashMap<ActionSpecificKey, BackendPipeline>();

    /**
     * Dispatch a message to the correct backend inbound pipeline.
     * @param messageContext The message to process wrapped in a <code>MessageContext</code>.
     * @return 
     * @throws NexusException
     */
    public MessageContext processMessage( MessageContext messageContext ) throws NexusException {

    	if ( LOG.isDebugEnabled() ) {
    		LOG.debug( "BackendInboundDispatcher.processMessage..." );
    	}

        if ( backendInboundPipelines != null ) {

            // Set some thread-local information so everyone in the pipeline can always access it
            NexusThreadStorage.set("conversationId", messageContext.getConversation().getConversationId());
            NexusThreadStorage.set("messageId", messageContext.getMessagePojo().getMessageId());

            ActionPojo action = messageContext.getMessagePojo().getAction();
            ConversationPojo conversation = messageContext.getMessagePojo().getConversation();
            ChoreographyPojo choreography = (conversation == null ? null : conversation.getChoreography());
            ActionSpecificKey actionSpecificKey = new ActionSpecificKey(
                    (action == null ? null : action.getName()), (choreography == null ? null : choreography.getName()) );
            BackendPipeline backendInboundPipeline = backendInboundPipelines.get( actionSpecificKey );
            if ( backendInboundPipeline != null ) {
                if ( LOG.isDebugEnabled() ) {
	            	LOG.debug( new LogMessage( "Found pipeline: " + backendInboundPipeline + " - " + actionSpecificKey,
	                        messageContext.getMessagePojo() ) );
                }

                backendInboundPipeline.processMessage(messageContext);
            } else {
                throw new NexusException( "No backend inbound pipeline found for message: "
                        + messageContext.getMessagePojo().getMessageId() + " ("
                        + messageContext.getMessagePojo().getConversation().getChoreography().getName() + " - "
                        + messageContext.getMessagePojo().getAction() + ")" );
            }

            // ThreadLocal-objects need to be manually removed
            NexusThreadStorage.remove("conversationId");
            NexusThreadStorage.remove("messageId");
        } else {
            throw new NexusException( "No backend inbound pipelines configured!" );
        }

        return messageContext;
    } // processMessage

    /**
     * 
     */
    public void initialize() {

        initialize( Engine.getInstance().getCurrentConfiguration() );

    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#initialize(org.nexuse2e.configuration.EngineConfiguration)
     */
    public void initialize( EngineConfiguration config ) {

        LOG.trace( "Initializing..." );

        backendInboundPipelines = config.getBackendInboundPipelines();

        status = BeanStatus.INITIALIZED;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#teardown()
     */
    public void teardown() {

        LOG.trace( "Freeing resources..." );

        status = BeanStatus.INSTANTIATED;
    } // teardown

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#getRunLevel()
     */
    public Layer getActivationLayer() {

        return Layer.INBOUND_PIPELINES;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#activate()
     */
    public void activate() {

        LOG.trace( "activate" );
        status = BeanStatus.ACTIVATED;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#deactivate()
     */
    public void deactivate() {

        LOG.trace( "deactivate" );
        status = BeanStatus.INITIALIZED;
    }

    /**
     * @return
     */
    public boolean validate() {

        return false;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#getStatus()
     */
    public BeanStatus getStatus() {

        return status;
    }

} // BackendInboundDispatcher

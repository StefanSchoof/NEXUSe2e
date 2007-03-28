/**
 * NEXUSe2e Business Messaging Open Source  
 * Copyright 2007, Tamgroup and X-ioma GmbH   
 *  
 * This is free software; you can redistribute it and/or modify it  
 * under the terms of the GNU Lesser General Public License as  
 * published by the Free Software Foundation version 2.1 of  
 * the License.  
 *  
 * This software is distributed in the hope that it will be useful,  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU  
 * Lesser General Public License for more details.  
 *  
 * You should have received a copy of the GNU Lesser General Public  
 * License along with this software; if not, write to the Free  
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.nexuse2e.messaging;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.nexuse2e.ActionSpecificKey;
import org.nexuse2e.Engine;
import org.nexuse2e.Manageable;
import org.nexuse2e.NexusException;
import org.nexuse2e.Constants.BeanStatus;
import org.nexuse2e.Constants.Runlevel;
import org.nexuse2e.configuration.EngineConfiguration;
import org.springframework.beans.factory.InitializingBean;

/**
 * Component dispatching inbound messages to the correct pipeline based on their choreography.
 *
 * @author gesch
 */
public class BackendInboundDispatcher implements InitializingBean, Manageable {

    private static Logger                               LOG                     = Logger
                                                                                        .getLogger( BackendInboundDispatcher.class );

    private BeanStatus                                  status                  = BeanStatus.UNDEFINED;

    private HashMap<ActionSpecificKey, BackendPipeline> backendInboundPipelines = new HashMap<ActionSpecificKey, BackendPipeline>();

    /**
     * Dispatch a message to the correct backend inbound pipeline.
     * @param messagePipeletParameter The message to process wrapped in a <code>MessagePipeletParameter</code>.
     * @return 
     * @throws NexusException
     */
    public MessageContext processMessage( MessageContext messagePipeletParameter )
            throws NexusException {

        LOG.debug( "BackendInboundDispatcher.processMessage..." );

        if ( backendInboundPipelines != null ) {
            ActionSpecificKey actionSpecificKey = new ActionSpecificKey( messagePipeletParameter.getMessagePojo()
                    .getAction().getName(), messagePipeletParameter.getMessagePojo().getConversation()
                    .getChoreography().getName() );
            BackendPipeline backendInboundPipeline = backendInboundPipelines.get( actionSpecificKey );
            if ( backendInboundPipeline != null ) {
                LOG.debug( "Found pipeline: " + backendInboundPipeline + " - " + actionSpecificKey );

                backendInboundPipeline.processMessage( messagePipeletParameter );
            } else {
                throw new NexusException( "No backend inbound pipeline found for message: "
                        + messagePipeletParameter.getMessagePojo().getMessageId() + " ("
                        + messagePipeletParameter.getMessagePojo().getConversation().getChoreography().getName()
                        + " - " + messagePipeletParameter.getMessagePojo().getAction() + ")" );
            }
        } else {
            throw new NexusException( "No backend inbound pipelines configured!" );
        }

        return messagePipeletParameter;
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
        
        LOG.debug( "Initializing..." );

        backendInboundPipelines = config.getBackendInboundPipelines();

        status = BeanStatus.INITIALIZED;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#teardown()
     */
    public void teardown() {

        LOG.debug( "Freeing resources..." );

        status = BeanStatus.INSTANTIATED;
    } // teardown

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#getRunLevel()
     */
    public Runlevel getActivationRunlevel() {

        return Runlevel.INBOUND_PIPELINES;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#activate()
     */
    public void activate() {

        // TODO Auto-generated method stub
        LOG.trace( "activate" );
        status = BeanStatus.ACTIVATED;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#deactivate()
     */
    public void deactivate() {

        // TODO Auto-generated method stub
        LOG.trace( "deactivate" );
        status = BeanStatus.INITIALIZED;
    }

    /**
     * @return
     */
    public boolean validate() {

        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {

        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.nexuse2e.Manageable#getStatus()
     */
    public BeanStatus getStatus() {

        return status;
    }

} // BackendInboundDispatcher

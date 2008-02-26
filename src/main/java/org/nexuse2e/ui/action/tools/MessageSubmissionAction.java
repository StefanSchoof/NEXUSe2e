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
package org.nexuse2e.ui.action.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.upload.FormFile;
import org.nexuse2e.Engine;
import org.nexuse2e.configuration.Constants;
import org.nexuse2e.configuration.EngineConfiguration;
import org.nexuse2e.pojo.ActionPojo;
import org.nexuse2e.pojo.ChoreographyPojo;
import org.nexuse2e.pojo.ParticipantPojo;
import org.nexuse2e.pojo.PartnerPojo;
import org.nexuse2e.ui.action.NexusE2EAction;
import org.nexuse2e.ui.form.MessageSubmissionForm;

/**
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 *
 * @author markus.breilmann
 */
public class MessageSubmissionAction extends NexusE2EAction {

    private static final String MSG_KEY_SUBMITTED          = "messagesubmission.submitted";
    private static final String MSG_KEY_ERROR              = "messagesubmission.error";
    private static final String MSG_KEY_ERROR_NOPRIMARYKEY = "messagesubmission.error.noprimarykey";
    private static final String MSG_KEY_ERROR_NOREPEAT     = "messagesubmission.error.norepeat";

    /* (non-Javadoc)
     * @see com.tamgroup.nexus.e2e.ui.action.NexusE2EAction#executeNexusE2EAction(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.apache.struts.action.ActionMessages)
     */
    @Override
    public ActionForward executeNexusE2EAction( ActionMapping actionMapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response, EngineConfiguration engineConfiguration, ActionMessages errors, ActionMessages messages )
            throws Exception {

        SortedSet<String> choreographyIds = new TreeSet<String>();
        ActionForward success = actionMapping.findForward( ACTION_FORWARD_SUCCESS );
        MessageSubmissionForm form = (MessageSubmissionForm) actionForm;

        // Set current list of choroegraphies
        List<ChoreographyPojo> choreographyList = engineConfiguration.getChoreographies();
        Iterator<ChoreographyPojo> choreographyIterator = choreographyList.iterator();
        while ( choreographyIterator.hasNext() ) {
            choreographyIds.add( choreographyIterator.next().getName() );
        }
        form.setChoreographies( choreographyIds );

        // Set current list of actions
        // Set current list of receivers
        String choreographyId = form.getChoreographyId();
        SortedSet<String> actions = new TreeSet<String>();
        SortedSet<PartnerPojo> receiverList = new TreeSet<PartnerPojo>( Constants.PARTNERCOMPARATOR );
        if ( ( ( choreographyId == null ) || ( choreographyId.length() == 0 ) ) && !choreographyIds.isEmpty() ) {
            choreographyId = choreographyIds.iterator().next();
        }
        if ( ( choreographyId != null ) && ( choreographyId.length() != 0 ) ) {
            ChoreographyPojo choreography = engineConfiguration
                    .getChoreographyByChoreographyId( choreographyId );
            if ( choreography != null ) {
                // Actions
                Set<ActionPojo> actionSet = choreography.getActions();
                Iterator<ActionPojo> actionIterator = actionSet.iterator();
                while ( actionIterator.hasNext() ) {
                    actions.add( actionIterator.next().getName() );
                }
                // Receivers
                List<ParticipantPojo> participantSet = choreography.getParticipants();
                Iterator<ParticipantPojo> participantIterator = participantSet.iterator();
                while ( participantIterator.hasNext() ) {
                    receiverList.add( participantIterator.next().getPartner() );
                    // LOG.trace( "Receiver: " + receiverList.get( receiverList.size() - 1 ) );
                }
            }
        }
        form.setActions( actions );
        form.setReceivers( receiverList );
        ArrayList<String> encodings = new ArrayList<String>();
        encodings.add( "UTF-8" );
        encodings.add( "ISO-8859-1" );
        encodings.add( "UTF-16" );
        encodings.add( "UTF-16LE" );
        encodings.add( "UTF-16BE" );
        encodings.add( "US-ASCII" );
        form.setEncodings( encodings );

        // Check whether we need to submit the message(s)
        if ( SUBMIT_BUTTON.equals( request.getParameter( SUBMIT_BUTTON ) ) ) {
            // LOG.trace( "Submitting message!" );
            String conversationId = form.getConversationId();
            int receiverNxPartnerId = form.getReceiver();

            PartnerPojo partner = engineConfiguration.getPartnerByNxPartnerId(
                    receiverNxPartnerId );
            if ( partner == null ) {
                errors.add( ActionMessages.GLOBAL_MESSAGE, new ActionMessage( MSG_KEY_ERROR,
                        "no partner found for receiverNxPartnerId: " + receiverNxPartnerId ) );
            }

            String action = form.getActionId();
            String primaryKey = form.getPrimaryKey();
            FormFile payload1 = form.getPayloadFile1();

            if ( ( payload1 == null ) && ( ( primaryKey == null ) || ( primaryKey.length() == 0 ) ) ) {
                errors.add( ActionMessages.GLOBAL_MESSAGE, new ActionMessage( MSG_KEY_ERROR_NOPRIMARYKEY ) );
                return success;
            }
            if ( form.getRepeat() < 1 ) {
                errors.add( ActionMessages.GLOBAL_MESSAGE, new ActionMessage( MSG_KEY_ERROR_NOREPEAT ) );
                return success;
            }
            try {
                for ( int i = 0; i < form.getRepeat(); i++ ) {
                    if ( ( payload1 != null ) && ( payload1.getFileSize() != 0 ) ) {
                        String encoding = form.getEncoding();
                        if ( encoding == null ) {
                            encoding = "UTF-8";
                        }
                        
                        String payloadString = new String( payload1.getFileData(), encoding );
                        Engine.getInstance().getCurrentConfiguration().getBackendPipelineDispatcher().processMessage(
                                partner.getPartnerId(), choreographyId, action, conversationId, null, null,
                                payloadString.getBytes() );
                        // Set primaryKey for UI confirmation message
                        primaryKey = payload1.getFileName();
                    } else {
                        Engine.getInstance().getCurrentConfiguration().getBackendPipelineDispatcher().processMessage(
                                partner.getPartnerId(), choreographyId, action, conversationId, null, primaryKey, null );
                    }
                    if ( form.getRepeat() > 1 ) {
                        conversationId = null;
                        form.setConversationId( null );
                    } else {
                        form.setConversationId( conversationId );
                    }
                } // for
                messages.add( ActionMessages.GLOBAL_MESSAGE, new ActionMessage( MSG_KEY_SUBMITTED, primaryKey ) );
            } catch ( Exception ex ) {
                errors.add( ActionMessages.GLOBAL_MESSAGE, new ActionMessage( MSG_KEY_ERROR, ex.toString() ) );
            }

        }

        return success;
    } // executeNexusE2EAction
} // MessageSubmissionViewAction

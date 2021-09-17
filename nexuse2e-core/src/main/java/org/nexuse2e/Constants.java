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
package org.nexuse2e;

/**
 * Constants used throughout the entire NEXUSe2e system
 * 
 * @author mbreilmann
 */
public class Constants {

    public static enum Severity {
        INFO, WARN, ERROR;
    }

    public static String       HIBERNATESESSIONFACTORYBEANID                 = "HybernateSessionFactory";

    // DAO bean IDs
    public static final String ACTION_DAO                                    = "actionDao";
    public static final String CHOREOGRAPHY_DAO                              = "choreographyDao";
    public static final String PARTICIPANT_DAO                               = "participantDao";
    public static final String CERTIFICATE_DAO                               = "certificateDao";
    public static final String COMMUNICATIONPARTNERDAO                       = "communicationPartnerDao";
    public static final String CONVERSATION_DAO                              = "conversationDao";
    public static final String MESSAGE_DAO                                   = "messageDao";

    // skeleton bean IDs
    public static final String FRONTEND_INBOUND_DISPATCHER                   = "frontendInboundDispatcher";
    public static final String FRONTEND_OUTBOUND_DISPATCHER                  = "frontendOutboundDispatcher";
    public static final String BACKEND_INBOUND_DISPATCHER                    = "backendInboundDispatcher";
    public static final String BACKEND_OUTBOUND_DISPATCHER                   = "backendOutboundDispatcher";
    public static final String BACKEND_PIPELINE_DISPATCHER                   = "backendPipelineDispatcher";
    public static final String TRANSACTION_SERVICE                           = "transactionService";
    public static final String FRONTEND_INBOUND_RESPONSE_ENDPOINT            = "frontendInboundResponseEndpoint";
    public static final String FRONTEND_OUTBOUND_RESPONSE_ENDPOINT           = "frontendOutboundResponseEndpoint";

    public static final String TRANSPORT_DISPATCHER_MAPPING                  = "nexusTransportDispatcherMapping";

    // naming postfixes
    public static final String POSTFIX_INBOUND_QUEUE                         = "-Inbound-Queue";
    public static final String POSTFIX_OUTBOUND_QUEUE                        = "-Outbound-Queue";
    public static final String POSTFIX_STATUS_UPDATE_SERIALIZER              = "-Status-Update-Serializer";

    public static final String POSTFIX_BACKEND_PIPELINE                      = "-Backend-Pipeline";
    public static final String POSTFIX_FRONTEND_PIPELINE                     = "-Frontend-Pipeline";
    public static final String POSTFIX_STATUS_UPDATE_PIPELINE                = "-Status-Update-Pipeline";

    public static final String ID_GENERATOR_MESSAGE                          = "messageId";
    public static final String ID_GENERATOR_CONVERSATION                     = "conversationId";
    public static final String ID_GENERATOR_MESSAGE_PAYLOAD                  = "messagePayloadId";

    // conversation status
    public static final int    CONVERSATION_STATUS_ERROR                     = -1;
    public static final int    CONVERSATION_STATUS_UNKNOWN                   = 0;
    public static final int    CONVERSATION_STATUS_CREATED                   = 1;
    public static final int    CONVERSATION_STATUS_PROCESSING                = 2;
    public static final int    CONVERSATION_STATUS_AWAITING_ACK              = 3;
    public static final int    CONVERSATION_STATUS_IDLE                      = 4;
    public static final int    CONVERSATION_STATUS_SENDING_ACK               = 5;
    public static final int    CONVERSATION_STATUS_ACK_SENT_AWAITING_BACKEND = 6;
    public static final int    CONVERSATION_STATUS_AWAITING_BACKEND          = 7;
    public static final int    CONVERSATION_STATUS_BACKEND_SENT_SENDING_ACK  = 8;
    public static final int    CONVERSATION_STATUS_COMPLETED                 = 9;

    // Database ID/primary key of system user
    public static final int    SYSTEM_USER_ID                                = 1;

    /**
     * The configuration root directory relative to Nexus home.
     */
    public static final String CONFIGROOT                                    = "WEB-INF/config/";

    /**
     * The default mime config file.
     */
    public static final String DEFAULT_MIME_CONFIG                           = "MimeConfig.xml";

    /**
     * The location of the TransportProtocol.xml root.
     */
    public static final String DERBYROOT                                     = CONFIGROOT + "database/derby/";

    public static final String PROTOCOL_ID_EBXML                             = "ebxml";
    public static final String PROTOCOL_ID_HTTP_PLAIN                        = "httpplain";

    // File name constant (for message labels, message payload content IDs)
    public static final String NX_LABEL_FILE_NAME                            = "_nxFileName";

    public static String getMessageStatusString(int messageStatus) {

        switch (org.nexuse2e.MessageStatus.getByOrdinal(messageStatus)) {
            case FAILED:
                // setStatus( "Failed (" + messagePojo.getStatus() + ")" );
                return "Failed";
            case QUEUED:
                // setStatus( "Queued (" + messagePojo.getStatus() + ")" );
                return "Queued";
            case RETRYING:
                // setStatus( "Retrying (" + messagePojo.getStatus() + ")" );
                return "Retrying";
            case COMPLETED:
                // setStatus( "Completed (" + messagePojo.getStatus() + ")" );
                return "Completed";
            case STOPPED:
                // setStatus( "Stopped (" + messagePojo.getStatus() + ")" );
                return "Stopped";
            default:
                // setStatus( "unknown (" + messagePojo.getStatus() + ")" );
                return "Unknown (" + messageStatus + ")";
        }
    }

}

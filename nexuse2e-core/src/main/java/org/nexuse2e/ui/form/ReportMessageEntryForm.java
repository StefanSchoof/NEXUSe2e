/*
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
package org.nexuse2e.ui.form;

import org.apache.struts.action.ActionForm;
import org.nexuse2e.MessageBackendStatus;
import org.nexuse2e.MessageStatus;
import org.nexuse2e.pojo.MessagePojo;
import org.nexuse2e.util.DateUtil;

import java.util.Date;

import static org.nexuse2e.util.DateUtil.getTimezone;

/**
 * @author gesch
 */
public class ReportMessageEntryForm extends ActionForm {

    /**
     *
     */
    private static final long serialVersionUID = 3348518676137301240L;
    private String            choreographyId   = null;
    private String            participantId    = null;
    private String            messageId        = null;
    private String            conversationId   = null;
    private int               nxConversationId = 0;
    private String            type             = null;
    private String            referencedId     = null;
    private String            status           = null;
    private String            backendStatus    = null;
    private String            action           = null;
    private Date              modifiedDate     = null;
    private Date              createdDate      = null;
    private Date              endDate          = null;
    private String            turnaroundTime   = null;
    private Date              expireDate       = null;
    private String            retries          = null;
    private String            protocol         = null;
    private String            direction        = null;
    private boolean           outbound         = false;

    public void setMessageProperties(MessagePojo messagePojo) {

        setChoreographyId(messagePojo.getConversation().getChoreography().getName());
        setParticipantId(messagePojo.getConversation().getPartner().getPartnerId());
        setMessageId(messagePojo.getMessageId());

        setConversationId(messagePojo.getConversation().getConversationId());
        setNxConversationId(messagePojo.getConversation().getNxConversationId());
        if (messagePojo.getReferencedMessage() == null) {
            setReferencedId("n/a");
        } else {
            setReferencedId(messagePojo.getReferencedMessage().getMessageId());
        }
        setModifiedDate(messagePojo.getModifiedDate());
        // LOG.trace("modified: "+pojo.getModifiedDate());
        setCreatedDate(messagePojo.getCreatedDate());
        // LOG.trace("created: "+pojo.getCreatedDate());
        setExpireDate(messagePojo.getExpirationDate());
        setRetries("" + messagePojo.getRetries());

        setOutbound(messagePojo.isOutbound());

        setProtocol(messagePojo.getTRP().getProtocol() + " / " + messagePojo.getTRP().getVersion());
        if (messagePojo.getType() == org.nexuse2e.messaging.Constants.INT_MESSAGE_TYPE_ACK) {
            setType("Acknowledgement");
        } else if (messagePojo.getType() == org.nexuse2e.messaging.Constants.INT_MESSAGE_TYPE_NORMAL) {
            setType("Normal");
        } else if (messagePojo.getType() == org.nexuse2e.messaging.Constants.INT_MESSAGE_TYPE_ERROR) {
            setType("Error");
        } else {
            setType("Unknown Messagetype (" + messagePojo.getType() + ")");
        }

        switch (MessageStatus.getByOrdinal(messagePojo.getStatus())) {
            case FAILED:
                // setStatus( "Failed (" + messagePojo.getStatus() + ")" );
                setStatus("Failed");
                break;
            case QUEUED:
                // setStatus( "Queued (" + messagePojo.getStatus() + ")" );
                setStatus("Queued");
                break;
            case RETRYING:
                // setStatus( "Retrying (" + messagePojo.getStatus() + ")" );
                setStatus("Retrying");
                break;
            case COMPLETED:
                // setStatus( "Sent (" + messagePojo.getStatus() + ")" );
                setStatus("Completed");
                break;
            case STOPPED:
                // setStatus( "Stopped (" + messagePojo.getStatus() + ")" );
                setStatus("Stopped");
                break;
            default:
                // setStatus( "unknown (" + messagePojo.getStatus() + ")" );
                setStatus("Unknown");
        }

        switch (MessageBackendStatus.getByOrdinal(messagePojo.getBackendStatus())) {
            case FAILED:
                setBackendStatus("Failed");
                break;
            case SENT:
                setBackendStatus("Sent");
                break;
            case OUTBOUND:
                setBackendStatus("Outbound");
                break;
            default:
                setBackendStatus("Unknown");
        }

        setAction(messagePojo.getAction().getName());
        if (messagePojo.isOutbound()) {
            setDirection("Outbound");
        } else {
            setDirection("Inbound");
        }

        setEndDate(messagePojo.getEndDate());

        if (messagePojo.getEndDate() == null) {
            setTurnaroundTime("Not terminated");
        } else {
            setTurnaroundTime(DateUtil.getDiffTimeRounded(messagePojo.getCreatedDate(), messagePojo.getEndDate()));
        }
    }

    public String getCreatedDate() {

        if (createdDate == null) {
            return "";
        }
        return DateUtil.localTimeToTimezone(createdDate, getTimezone(), null);
    }

    public String getMessageId() {

        return messageId;
    }

    public void setMessageId(String messageId) {

        this.messageId = messageId;
    }

    public String getAction() {

        return action;
    }

    public void setAction(String action) {

        this.action = action;
    }

    public String getStatus() {

        return status;
    }

    public void setStatus(String status) {

        this.status = status;
    }

    public String getBackendStatus() {

        return backendStatus;
    }

    public void setBackendStatus(String backendStatus) {

        this.backendStatus = backendStatus;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String getTurnaroundTime() {

        return turnaroundTime;
    }

    public void setTurnaroundTime(String turnaroundTime) {

        this.turnaroundTime = turnaroundTime;
    }

    public String getChoreographyId() {

        return choreographyId;
    }

    public void setChoreographyId(String choreographyId) {

        this.choreographyId = choreographyId;
    }

    public String getConversationId() {

        return conversationId;
    }

    public void setConversationId(String conversationId) {

        this.conversationId = conversationId;
    }

    public int getNxConversationId() {
        return nxConversationId;
    }

    public void setNxConversationId(int nxConversationId) {
        this.nxConversationId = nxConversationId;
    }

    public String getParticipantId() {

        return participantId;
    }

    public void setParticipantId(String participantId) {

        this.participantId = participantId;
    }

    public String getProtocol() {

        return protocol;
    }

    public void setProtocol(String protocol) {

        this.protocol = protocol;
    }

    public String getReferencedId() {

        return referencedId;
    }

    public void setReferencedId(String referencedId) {

        this.referencedId = referencedId;
    }

    public String getRetries() {

        return retries;
    }

    public void setRetries(String retries) {

        this.retries = retries;
    }

    public String getDirection() {

        return direction;
    }

    public void setDirection(String direction) {

        this.direction = direction;
    }

    /**
     * @return the endDate
     */
    public String getEndDate() {
        if (endDate == null) {
            return "";
        }
        return DateUtil.localTimeToTimezone(endDate, getTimezone(), null);
    }

    /**
     * @param endDate
     *            the endDate to set
     */
    public void setEndDate(Date endDate) {

        this.endDate = endDate;
    }

    /**
     * @return the expireDate
     */
    public Date getExpireDate() {

        return expireDate;
    }

    /**
     * @param expireDate
     *            the expireDate to set
     */
    public void setExpireDate(Date expireDate) {

        this.expireDate = expireDate;
    }

    /**
     * @return the modifiedDate
     */
    public Date getModifiedDate() {

        return modifiedDate;
    }

    /**
     * @param modifiedDate
     *            the modifiedDate to set
     */
    public void setModifiedDate(Date modifiedDate) {

        this.modifiedDate = modifiedDate;
    }

    /**
     * @param createdDate
     *            the createdDate to set
     */
    public void setCreatedDate(Date createdDate) {

        this.createdDate = createdDate;
    }

    public boolean isOutbound() {

        return outbound;
    }

    public void setOutbound(boolean outbound) {

        this.outbound = outbound;
    }
}

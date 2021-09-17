/*
   NEXUSe2e Business Messaging Open Source
   Copyright 2000-2021, direkt gruppe GmbH

   This is free software; you can redistribute it and/or modify it
   under the terms of the GNU Lesser General Public License as
   published by the Free Software Foundation version 3 of
   the License.

   This software is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with this software; if not, write to the Free
   Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
   02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.nexuse2e.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.type.IntegerType;
import org.hibernate.type.Type;
import org.nexuse2e.Constants;
import org.nexuse2e.Engine;
import org.nexuse2e.MessageStatus;
import org.nexuse2e.NexusException;
import org.nexuse2e.controller.StateTransitionException;
import org.nexuse2e.dao.UpdateTransactionOperation.UpdateScope;
import org.nexuse2e.logging.LogMessage;
import org.nexuse2e.pojo.*;
import org.nexuse2e.reporting.Statistics;
import org.nexuse2e.reporting.StatisticsCertificate;
import org.nexuse2e.reporting.StatisticsConversation;
import org.nexuse2e.reporting.StatisticsMessage;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

import static org.nexuse2e.messaging.Constants.*;

/**
 * Data access object (DAO) to provide persistence services for transaction related entities.
 *
 * @author gesch
 */
@Repository
public class TransactionDAOImpl extends BasicDAOImpl implements TransactionDAO {

    protected static Logger LOG = Logger.getLogger(TransactionDAOImpl.class);

    private static final Map<Integer, int[]> followUpConversationStates;
    private static final Map<Integer, int[]> followUpMessageStates;

    static {
        followUpConversationStates = new HashMap<Integer, int[]>();
        followUpConversationStates.put(Constants.CONVERSATION_STATUS_ERROR, new int[]{
                Constants.CONVERSATION_STATUS_IDLE, Constants.CONVERSATION_STATUS_COMPLETED,
                Constants.CONVERSATION_STATUS_PROCESSING});
        followUpConversationStates.put(Constants.CONVERSATION_STATUS_CREATED,
                new int[]{Constants.CONVERSATION_STATUS_PROCESSING});
        followUpConversationStates.put(Constants.CONVERSATION_STATUS_PROCESSING, new int[]{
                Constants.CONVERSATION_STATUS_AWAITING_ACK, Constants.CONVERSATION_STATUS_AWAITING_BACKEND,
                Constants.CONVERSATION_STATUS_SENDING_ACK, Constants.CONVERSATION_STATUS_ACK_SENT_AWAITING_BACKEND,
                Constants.CONVERSATION_STATUS_BACKEND_SENT_SENDING_ACK, Constants.CONVERSATION_STATUS_IDLE,
                Constants.CONVERSATION_STATUS_ERROR, Constants.CONVERSATION_STATUS_COMPLETED});
        followUpConversationStates.put(Constants.CONVERSATION_STATUS_AWAITING_ACK, new int[]{
                Constants.CONVERSATION_STATUS_COMPLETED, Constants.CONVERSATION_STATUS_ERROR,
                Constants.CONVERSATION_STATUS_IDLE});
        followUpConversationStates.put(Constants.CONVERSATION_STATUS_IDLE,
                new int[]{Constants.CONVERSATION_STATUS_PROCESSING, Constants.CONVERSATION_STATUS_AWAITING_ACK, Constants.CONVERSATION_STATUS_COMPLETED});
        followUpConversationStates.put(Constants.CONVERSATION_STATUS_SENDING_ACK,
                new int[]{Constants.CONVERSATION_STATUS_ACK_SENT_AWAITING_BACKEND});
        followUpConversationStates.put(Constants.CONVERSATION_STATUS_ACK_SENT_AWAITING_BACKEND, new int[]{
                Constants.CONVERSATION_STATUS_COMPLETED, Constants.CONVERSATION_STATUS_ERROR, Constants.CONVERSATION_STATUS_PROCESSING,
                Constants.CONVERSATION_STATUS_IDLE});
        followUpConversationStates.put(Constants.CONVERSATION_STATUS_AWAITING_BACKEND,
                new int[]{Constants.CONVERSATION_STATUS_BACKEND_SENT_SENDING_ACK});
        followUpConversationStates.put(Constants.CONVERSATION_STATUS_BACKEND_SENT_SENDING_ACK, new int[]{
                Constants.CONVERSATION_STATUS_COMPLETED, Constants.CONVERSATION_STATUS_ERROR,
                Constants.CONVERSATION_STATUS_IDLE});
        followUpConversationStates.put(Constants.CONVERSATION_STATUS_COMPLETED,
                new int[]{Constants.CONVERSATION_STATUS_ERROR, Constants.CONVERSATION_STATUS_PROCESSING});

        followUpMessageStates = new HashMap<Integer, int[]>();
        followUpMessageStates.put(MessageStatus.FAILED.getOrdinal(), new int[]{MessageStatus.QUEUED.getOrdinal(),
                MessageStatus.COMPLETED.getOrdinal()});
        followUpMessageStates.put(MessageStatus.RETRYING.getOrdinal(), new int[]{MessageStatus.FAILED.getOrdinal(),
                MessageStatus.COMPLETED.getOrdinal()});
        followUpMessageStates.put(MessageStatus.QUEUED.getOrdinal(), new int[]{MessageStatus.RETRYING.getOrdinal(),
                MessageStatus.FAILED.getOrdinal(), MessageStatus.COMPLETED.getOrdinal()});
        followUpMessageStates.put(MessageStatus.COMPLETED.getOrdinal(), new int[]{MessageStatus.QUEUED.getOrdinal()});
    }

    private String getType(int messageType) {
        switch (messageType) {
            case INT_MESSAGE_TYPE_NORMAL:
                return "normal";
            case INT_MESSAGE_TYPE_ACK:
                return "acknowledgement";
            case INT_MESSAGE_TYPE_ERROR:
                return "error";
        }
        return "unknown";
    }

    /**
     * Helper method for trace log level output
     */
    private void printConversationInfo(String prefix, ConversationPojo pojo, MessagePojo lastMessage) {
        if (pojo != null) {

            // DEBUG
            int id = pojo.getNxConversationId();
            pojo.setNxConversationId(0);
            String s = pojo.toString();
            pojo.setNxConversationId(id);
            // END OF DEBUG


            List<MessagePojo> messages = pojo.getMessages();
            if (lastMessage == null) {
                lastMessage = (messages == null || messages.isEmpty() ? null : messages.get(messages.size() - 1));
            }
            org.nexuse2e.configuration.EngineConfiguration cfg = org.nexuse2e.Engine.getInstance().getCurrentConfiguration();
            ActionPojo action = null;
            if (lastMessage != null) {
                try {
                    action = cfg.getActionFromChoreographyByNxActionId(
                            cfg.getChoreographyByNxChoreographyId(lastMessage.getConversation().getChoreography().getNxChoreographyId()),
                            lastMessage.getAction().getNxActionId());
                } catch (NexusException ex) {
                    ex.printStackTrace();
                }
            }
            LOG.trace(new LogMessage(prefix + s + " conversationId: " + pojo.getConversationId() + " " +
                    (messages == null ? 0 : messages.size()) + " messages" +
                    (lastMessage == null ? "" : ", last message type is " + getType(lastMessage.getType()) +
                            ", messageId is " + lastMessage.getMessageId()) + (action == null ? "" : " (" + action.getName() + ")") +
                    ", thread " + Thread.currentThread().getName(), lastMessage));
        } else if (lastMessage != null) {
            LOG.trace(new LogMessage(prefix + " message type is " + getType(lastMessage.getType()) + ", messageId is " + lastMessage.getMessageId(), lastMessage));
        }
    }
    //    public static final String TYPE_ACK      = "Acknowledgment";
    //    public static final String TYPE_DEFAULT  = "Normal";

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getConversationByConversationId(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public ConversationPojo getConversationByConversationId(String conversationId) {

        DetachedCriteria dc = DetachedCriteria.forClass(ConversationPojo.class);
        dc.add(Restrictions.eq("conversationId", conversationId));
        List<ConversationPojo> result = (List<ConversationPojo>) getListThroughSessionFind(dc, 0, 0);

        if (result != null && result.size() > 0) {
            ConversationPojo pojo = result.get(0);
            if (LOG.isTraceEnabled()) {
                printConversationInfo("loaded conversation:", pojo, null);
            }
            return pojo;
        }

        return null;
    } // getConversationByConversationId

    public ConversationPojo getConversationByConversationId(int nxConversationId) {

        return (ConversationPojo) sessionFactory.getCurrentSession().get(ConversationPojo.class, nxConversationId);
    } // getConversationByConversationId

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getMessageByMessageId(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public MessagePojo getMessageByMessageId(String messageId) throws NexusException {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
        dc.add(Restrictions.eq("messageId", messageId));
        List<MessagePojo> result = (List<MessagePojo>) getListThroughSessionFind(dc, 0, 0);

        if (result != null && result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    public MessagePojo getMessageByNxMessageId(Integer nxMessageId) throws NexusException {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
        dc.add(Restrictions.eq("nxMessageId", nxMessageId));
        List<MessagePojo> result = (List<MessagePojo>) getListThroughSessionFind(dc, 0, 0);

        if (result != null && result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getMessageByReferencedMessageId(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public MessagePojo getMessageByReferencedMessageId(String messageId) throws NexusException {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
        dc.createCriteria("referencedMessage").add(Restrictions.eq("messageId", messageId));
        //StringBuffer query = new StringBuffer( "from MessagePojo where referencedMessage.messageId='" + messageId + "'" );

        List<MessagePojo> result = (List<MessagePojo>) getListThroughSessionFind(dc, 0, 0);

        if (result != null && result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getMessagesCount(java.lang.String, int, int, java.lang.String, java.lang.String, java.util.Date, java.util.Date)
     */
    public int getMessagesCount(String status, Integer type, int nxChoreographyId, int nxPartnerId, String conversationId,
                                String messageId, Date startDate, Date endDate) throws NexusException {

        return getCountThroughSessionFind(getMessagesForReportCriteria(status, nxChoreographyId, nxPartnerId,
                conversationId, messageId, type, startDate, endDate, 0, false));
    }

    public Statistics getStatistics(Date startDate, Date endDate) throws NexusException {

        Statistics result = new Statistics();
        result.setStartDate(startDate);
        result.setEndDate(endDate);

        String errorMessageQueryString = buildErrorMessageQuery(startDate, endDate);

        SQLQuery messageQuery = sessionFactory.getCurrentSession().createSQLQuery(errorMessageQueryString);
        setTimestampFields(startDate, endDate, messageQuery);

        messageQuery.setMaxResults(11);
        List<Object[]> messageResultSet = messageQuery.list();

        for (Object[] record : messageResultSet) {
            StatisticsMessage line = new StatisticsMessage(record);
            result.getFailedMessages().add(line);
        }

        result.setIdleConversations(getIdleConversations(startDate, endDate));

        List<ConversationPojo> conversations = getConversationsForStatisticsPlain(startDate, endDate);
        result.setConversations(conversations);

        return result;
    }

    public List<StatisticsConversation> getIdleConversations(Date startDate, Date endDate) {
        int idleGracePeriodInMinutes = Engine.getInstance().getIdleGracePeriodInMinutes();

        Date modifiedEndDate = endDate;
        if (modifiedEndDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, idleGracePeriodInMinutes * -1);
            modifiedEndDate = new Timestamp(cal.getTimeInMillis());
        }
        String idleConversationQueryString = buildIdleConversationQuery(startDate, modifiedEndDate);
        SQLQuery idleConversationQuery = sessionFactory.getCurrentSession().createSQLQuery(idleConversationQueryString);
        setTimestampFields(startDate, modifiedEndDate, idleConversationQuery);

        idleConversationQuery.setMaxResults(11);
        List<Object[]> idleConversationResultSet = idleConversationQuery.list();

        List<StatisticsConversation> idleConversations = new LinkedList<>();
        for (Object[] record : idleConversationResultSet) {
            StatisticsConversation conversation = new StatisticsConversation(record);
            idleConversations.add(conversation);
        }
        return idleConversations;
    }


    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getConversationsForReport(java.lang.String, int, int, java.lang.String, java.util.Date, java.util.Date, int, int, int, boolean)
     */
    @SuppressWarnings("unchecked")
    public List<ConversationPojo> getConversationsForStatisticsPlain(Date start, Date end)
            throws NexusException {

        StringBuilder sqlQuery = new StringBuilder("SELECT " +
                "nx_conversation.nx_conversation_id, nx_conversation.status, nx_conversation.conversation_id " +
                "FROM nx_conversation " +
                "INNER JOIN nx_choreography " +
                "ON nx_conversation.nx_choreography_id = nx_choreography.nx_choreography_id " +
                "INNER JOIN nx_partner " +
                "ON  nx_conversation.nx_partner_id = nx_partner.nx_partner_id " +
                "INNER JOIN nx_action " +
                "ON nx_conversation.current_nx_action_id = nx_action.nx_action_id");


        if (start != null || end != null) {
            sqlQuery.append(" WHERE ");
            String prefix = "";
            if (start != null) {
                sqlQuery.append("nx_conversation.created_date >= :start");
                prefix = " AND ";
            }
            if (end != null) {
                sqlQuery.append(prefix + " nx_conversation.created_date <= :end");
                prefix = " AND ";
            }

        }
        sqlQuery.append(" ORDER by nx_conversation.created_date DESC");
        SQLQuery query = sessionFactory.getCurrentSession().createSQLQuery(sqlQuery.toString());
        setTimestampFields(start, end, query);

        List<Object[]> rowList = query.list();
        List<ConversationPojo> results = new ArrayList<>();
        for(Object[] row : rowList) {
            ConversationPojo entry = new ConversationPojo();
            entry.setNxConversationId(((Number) row[0]).intValue());
            entry.setStatus(((Number) row[1]).intValue());
            entry.setConversationId((String) row[2]);
            results.add(entry);
        }

        return results;

    }


    private String buildIdleConversationQuery(Date startDate, Date endDate) {
        StringBuilder sqlQuery = new StringBuilder("SELECT nx_conversation.conversation_id, nx_conversation.created_date, nx_conversation.modified_date, nx_conversation.status, nx_conversation.nx_conversation_id, nx_choreography.name as choreography, nx_partner.partner_id, nx_action.name as currentAction\n" +
                " FROM nx_conversation " +
                " INNER JOIN nx_choreography " +
                " ON nx_conversation.nx_choreography_id = nx_choreography.nx_choreography_id " +
                " INNER JOIN nx_partner " +
                " ON nx_conversation.nx_partner_id = nx_partner.nx_partner_id " +
                " INNER JOIN nx_action " +
                " ON nx_conversation.current_nx_action_id = nx_action.nx_action_id " +
                " WHERE nx_conversation.status = 4 ");

        if (startDate != null || endDate != null) {
            sqlQuery.append(" AND ");
            String prefix = "";
            if (startDate != null) {
                sqlQuery.append(" nx_conversation.modified_date >= :start ");
                prefix = " AND ";
            }
            if (endDate != null) {
                sqlQuery.append(prefix).append(" nx_conversation.modified_date <= :end ");
            }
        }

        sqlQuery.append(" ORDER by nx_conversation.modified_date DESC");
        return sqlQuery.toString();
    }

    private String buildErrorMessageQuery(Date startDate, Date endDate) {
        StringBuilder sqlQuery = new StringBuilder("SELECT message_id, nx_action.name as action, nx_choreography.name as choreography, nx_message.created_date, nx_message.type, nx_message.status, nx_conversation.conversation_id, nx_message.nx_message_id, nx_conversation.nx_conversation_id, nx_partner.partner_id\n" +
                " FROM nx_message " +
                " INNER JOIN nx_conversation " +
                " ON nx_conversation.nx_conversation_id = nx_message.nx_conversation_id " +
                " INNER JOIN nx_action " +
                " ON nx_action.nx_action_id = nx_message.nx_action_id " +
                " INNER JOIN nx_choreography " +
                " ON nx_conversation.nx_choreography_id = nx_choreography.nx_choreography_id " +
                " INNER JOIN nx_partner " +
                " ON nx_conversation.nx_partner_id = nx_partner.nx_partner_id " +
                " WHERE nx_message.status = -1 ");

        if (startDate != null || endDate != null) {
            sqlQuery.append(" AND ");
            String prefix = "";
            if (startDate != null) {
                sqlQuery.append(" nx_message.created_date >= :start ");
                prefix = " AND ";
            }
            if (endDate != null) {
                sqlQuery.append(prefix).append(" nx_message.created_date <= :end ");
            }
        }

        sqlQuery.append(" ORDER by nx_message.created_date DESC");
        return sqlQuery.toString();
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getActiveMessages()
     */
    @SuppressWarnings("unchecked")
    public List<MessagePojo> getActiveMessages() throws NexusException {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
        dc.add(Restrictions.in("status", new Object[]{MessageStatus.RETRYING.getOrdinal(),
                MessageStatus.QUEUED.getOrdinal()}));
        dc.add(Restrictions.eq("outbound", true));

        return (List<MessagePojo>) getListThroughSessionFind(dc, 0, 0);
    } // getActiveMessages

    /* (non-Javadoc)
     * Type is currently not supported via the UI and the value mapping for nx_message.type(int) to String is not defined.
     *
     * @see org.nexuse2e.dao.TransactionDAO#getMessagesForReport(java.lang.String, int, int, java.lang.String, java.lang.String, java.lang.String, java.util.Date, java.util.Date, int, int, int, boolean)
     */
    @SuppressWarnings("unchecked")
    public List<MessagePojo> getMessagesForReport(String status, int nxChoreographyId, int nxPartnerId,
                                                  String conversationId, String messageId, Integer type, Date start, Date end, int itemsPerPage, int page,
                                                  int field, boolean ascending) throws NexusException {


        StringBuilder sqlQuery = new StringBuilder("SELECT nx_message_id " +
                "FROM nx_message " +
                "INNER JOIN nx_conversation " +
                "ON nx_conversation.nx_conversation_id = nx_message.nx_conversation_id " +
                "INNER JOIN nx_choreography " +
                "ON nx_conversation.nx_choreography_id = nx_choreography.nx_choreography_id " +
                "INNER JOIN nx_partner " +
                "ON nx_conversation.nx_partner_id = nx_partner.nx_partner_id ");

        if (StringUtils.isNotEmpty(status) || nxChoreographyId > 0 || nxPartnerId > 0 || StringUtils.isNotEmpty(conversationId)
                || StringUtils.isNotEmpty(messageId) || start != null || end != null || type != null) {

            sqlQuery.append(" WHERE ");
            String prefix = "";
            if (start != null) {
                sqlQuery.append("nx_message.created_date >= :start");
                prefix = " AND ";
            }
            if (end != null) {
                sqlQuery.append(prefix + " nx_message.created_date <= :end");
                prefix = " AND ";
            }
            if (type != null) {
                sqlQuery.append(prefix + " nx_message.type = " + type);
                prefix = " AND ";
            }
            if (StringUtils.isNotBlank(status)) {
                if (status.contains(",")) {
                    sqlQuery.append(prefix + " nx_message.status in (" + status + ")");

                } else {
                    sqlQuery.append(prefix + " nx_message.status = " + status);
                }
                prefix = " AND ";
            }
            if (nxPartnerId > 0) {
                sqlQuery.append(prefix + " nx_partner.nx_partner_id = " + nxPartnerId);
                prefix = " AND ";
            }
            if (nxChoreographyId > 0) {
                sqlQuery.append(prefix + " nx_choreography.nx_choreography_id = " + nxChoreographyId);
                prefix = " AND ";
            }
            if (StringUtils.isNotBlank(conversationId)) {
                sqlQuery.append(prefix + " nx_conversation.conversation_id like '" + conversationId + "'");
            }

            if (StringUtils.isNotBlank(messageId)) {
                sqlQuery.append(prefix + " nx_message.message_id like '" + messageId + "'");
            }


        }

        sqlQuery.append(" ORDER by nx_message.created_date DESC");
        SQLQuery query = sessionFactory.getCurrentSession().createSQLQuery(sqlQuery.toString());
        setTimestampFields(start, end, query);

        List idList = query.list();


        int fromIndex = itemsPerPage * page;
        int toIndex = itemsPerPage * (page + 1);
        if (idList.size() > fromIndex) {
            idList = idList.subList(fromIndex, toIndex < idList.size() ? toIndex : idList.size());
        }

        Integer[] ids = getIntegers(idList);

        if (idList.size() > 0) {

            Criteria crit = sessionFactory.getCurrentSession().createCriteria(MessagePojo.class);
            crit.add(Restrictions.in("nxMessageId", ids));
            crit.addOrder(Order.desc("createdDate"));
            List result = crit.list();
            return result;
        }
        return new ArrayList<MessagePojo>();

    }

    /**
     * internal mapping method for converting database specific values into integers for further consistent processing.
     *
     * @param idList
     * @return
     */
    private Integer[] getIntegers(List idList) {
        Integer[] ids = new Integer[idList.size()];
        for (int i = 0; i < idList.size(); i++) {
            Object value = idList.get(i);
            // value is returned as BigDecimal from OracleDB and as Integer from other databases
            ids[i] = ((Number) value).intValue();
        }
        return ids;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getConversationsForReport(java.lang.String, int, int, java.lang.String, java.util.Date, java.util.Date, int, int, int, boolean)
     */
    @SuppressWarnings("unchecked")
    public List<ConversationPojo> getConversationsForReport(String status, int nxChoreographyId, int nxPartnerId,
                                                            String conversationId, Date start, Date end, int itemsPerPage, int page, int field, boolean ascending)
            throws NexusException {

        List<BigDecimal> idList = getConversationsForReportPlain(status, nxChoreographyId, nxPartnerId, conversationId, start, end);


        int fromIndex = itemsPerPage * page;
        int toIndex = itemsPerPage * (page + 1);
        if (idList.size() > fromIndex) {
            idList = idList.subList(fromIndex, toIndex < idList.size() ? toIndex : idList.size());
        }

        Integer[] ids = getIntegers(idList);

        if (idList.size() > 0) {
            Criteria crit = sessionFactory.getCurrentSession().createCriteria(ConversationPojo.class);
            crit.add(Restrictions.in("nxConversationId", ids));
            crit.addOrder(Order.desc("createdDate"));
            List result = crit.list();
            return result;
        }

        return new ArrayList<ConversationPojo>();
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getConversationsForReport(java.lang.String, int, int, java.lang.String, java.util.Date, java.util.Date, int, int, int, boolean)
     */
    @SuppressWarnings("unchecked")
    public List<BigDecimal> getConversationsForReportPlain(String status, int nxChoreographyId, int nxPartnerId, String conversationId, Date start, Date end)
            throws NexusException {

        StringBuilder sqlQuery = new StringBuilder("SELECT " +
                "nx_conversation.nx_conversation_id " +
                "FROM nx_conversation " +
                "INNER JOIN nx_choreography " +
                "ON nx_conversation.nx_choreography_id = nx_choreography.nx_choreography_id " +
                "INNER JOIN nx_partner " +
                "ON  nx_conversation.nx_partner_id = nx_partner.nx_partner_id " +
                "INNER JOIN nx_action " +
                "ON nx_conversation.current_nx_action_id = nx_action.nx_action_id");


        if (StringUtils.isNotEmpty(status) || nxChoreographyId > 0 || nxPartnerId > 0 || StringUtils.isNotEmpty(conversationId) || start != null || end != null) {
            sqlQuery.append(" WHERE ");
            String prefix = "";
            if (start != null) {
                sqlQuery.append("nx_conversation.created_date >= :start");
                prefix = " AND ";
            }
            if (end != null) {
                sqlQuery.append(prefix + " nx_conversation.created_date <= :end");
                prefix = " AND ";
            }
            if (StringUtils.isNotBlank(status)) {
                if (status.contains(",")) {
                    sqlQuery.append(prefix + " nx_conversation.status in (" + status + ")");

                } else {
                    sqlQuery.append(prefix + " nx_conversation.status = " + status);
                }
                prefix = " AND ";
            }
            if (nxPartnerId > 0) {
                sqlQuery.append(prefix + " nx_partner.nx_partner_id = " + nxPartnerId);
                prefix = " AND ";
            }
            if (nxChoreographyId > 0) {
                sqlQuery.append(prefix + " nx_choreography.nx_choreography_id = " + nxChoreographyId);
                prefix = " AND ";
            }
            if (StringUtils.isNotBlank(conversationId)) {
                sqlQuery.append(prefix + " nx_conversation.conversation_id like '" + conversationId + "'");
            }
        }
        sqlQuery.append(" ORDER by nx_conversation.created_date DESC");
        SQLQuery query = sessionFactory.getCurrentSession().createSQLQuery(sqlQuery.toString());
        setTimestampFields(start, end, query);

        List<BigDecimal> idList = query.list();


        return idList;

    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getConversationsCount(java.util.Date, java.util.Date)
     */
    public long getConversationsCount(Date start, Date end) throws NexusException {

        Session session = sessionFactory.getCurrentSession();
        StringBuilder query = new StringBuilder("select count(*) from nx_conversation conv ");
        Map<String, Timestamp> map = appendQueryDate(query, "conv", start, end);
        Query sqlquery = session.createSQLQuery(query.toString());
        for (String name : map.keySet()) {
            sqlquery.setTimestamp(name, map.get(name));
        }
        return ((Number) sqlquery.uniqueResult()).longValue();
    }

    public long getMessagesCount(Date start, Date end) throws NexusException {

        Session session = sessionFactory.getCurrentSession();
        StringBuilder query = new StringBuilder("select count(*) from nx_message msg inner join nx_conversation conv on (msg.nx_conversation_id = conv.nx_conversation_id) ");
        Map<String, Timestamp> map = appendQueryDate(query, "conv", start, end);
        Query sqlquery = session.createSQLQuery(query.toString());
        for (String name : map.keySet()) {
            sqlquery.setTimestamp(name, map.get(name));
        }
        return ((Number) sqlquery.uniqueResult()).longValue();
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getLogCount(java.util.Date, java.util.Date)
     */
    public long getLogCount(Date start, Date end) throws NexusException {

        Session session = sessionFactory.getCurrentSession();
        StringBuilder query = new StringBuilder("select count(nx_log_id) from nx_log log ");
        Map<String, Timestamp> map = appendQueryDate(query, "log", start, end);
        Query sqlquery = session.createSQLQuery(query.toString());
        for (String name : map.keySet()) {
            sqlquery.setTimestamp(name, map.get(name));
        }
        return ((Number) sqlquery.uniqueResult()).longValue();
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getLogCount(java.util.Date, java.util.Date)
     */
    public Map<Level, Long> getLogCount(Date start, Date end, Level minLevel, Level maxLevel) throws NexusException {

        int min = (minLevel == null ? 0 : minLevel.toInt());
        int max = (maxLevel == null ? 1000000 : maxLevel.toInt());

        Session session = sessionFactory.getCurrentSession();
        StringBuilder query = new StringBuilder("select count(nx_log_id) as msg_count, severity from nx_log log ");
        if (minLevel != null || maxLevel != null) {
            query.append("where severity >= ");
            query.append(min);
            query.append(" and severity <= ");
            query.append(max);
            query.append(" ");
        }
        Map<String, Timestamp> map = appendQueryDate(query, "log", start, end);
        query.append(" group by severity");
        Query sqlquery = session.createSQLQuery(query.toString());
        for (String name : map.keySet()) {
            sqlquery.setTimestamp(name, map.get(name));
        }
        List<?> l = sqlquery.list();
        Map<Level, Long> result = new LinkedHashMap<Level, Long>(l.size());
        for (Object o : l) {
            Object[] kv = (Object[]) o;
            Long count = ((Number) kv[0]).longValue();
            Level level = Level.toLevel(((Number) kv[1]).intValue());
            result.put(level, count);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#removeLogEntries(java.util.Date, java.util.Date)
     */
    public long removeLogEntries(Date start, Date end) throws NexusException {

        Session session = sessionFactory.getCurrentSession();

        StringBuilder query = new StringBuilder("delete from nx_log ");
        Map<String, Timestamp> map = appendQueryDate(query, "", start, end);
        Query sqlquery = session.createSQLQuery(query.toString());
        for (String name : map.keySet()) {
            sqlquery.setTimestamp(name, map.get(name));
        }
        long l = sqlquery.executeUpdate();
        return l;
    }

    /**
     * @param start
     * @param end
     * @return
     */
    private Map<String, Timestamp> appendQueryDate(StringBuilder queryString, String prefix, Date start, Date end) {

        Timestamp startTs = (start == null ? null : (start instanceof Timestamp ? (Timestamp) start : new Timestamp(start.getTime())));
        Timestamp endTs = (end == null ? null : (end instanceof Timestamp ? (Timestamp) end : new Timestamp(end.getTime())));

        if (StringUtils.isEmpty(prefix)) {
            prefix = "";
        } else {
            prefix += ".";
        }
        boolean first = queryString.indexOf("where") < 0;
        Map<String, Timestamp> map = new HashMap<String, Timestamp>(2);
        if (start != null) {
            if (!first) {
                queryString.append(" and ");
            } else {
                queryString.append(" where ");
            }
            queryString.append(prefix + "created_date >= :startDate");
            map.put("startDate", startTs);
            first = false;
        }
        if (end != null) {
            if (!first) {
                queryString.append(" and ");
            } else {
                queryString.append(" where ");
            }
            queryString.append(prefix + "created_date <= :endDate");
            map.put("endDate", endTs);
            first = false;
        }
        return map;
    }

    @Transactional
    public void removeMessages(String status, int nxChoreographyId, int nxPartnerId, String conversationId, String messageId, Date start, Date end) throws HibernateException {
        StringBuilder getMessages = new StringBuilder(
                "SELECT nx_message_id\n" + 
                "FROM nx_message msg\n" + 
                "INNER JOIN nx_conversation conv ON conv.nx_conversation_id = msg.nx_conversation_id\n" + 
                "INNER JOIN nx_choreography choreo ON conv.nx_choreography_id = choreo.nx_choreography_id\n" + 
                "INNER JOIN nx_partner partner ON conv.nx_partner_id = partner.nx_partner_id\n" + 
                "INNER JOIN nx_action action ON conv.current_nx_action_id = action.nx_action_id");
        appendMsgWhereCause(status, nxChoreographyId, nxPartnerId, conversationId, messageId, start, end, getMessages);

        String deleteLabels = "DELETE FROM nx_message_label WHERE nx_message_id IN (" + getMessages + ')';
        String deletePayloads = "DELETE FROM nx_message_payload WHERE nx_message_id IN (" + getMessages + ')';

        SQLQuery deleteLabelsQuery = sessionFactory.getCurrentSession().createSQLQuery(deleteLabels);
        setTimestampFields(start, end, deleteLabelsQuery);
        deleteLabelsQuery.executeUpdate();

        SQLQuery deletePayloadsQuery = sessionFactory.getCurrentSession().createSQLQuery(deletePayloads);
        setTimestampFields(start, end, deletePayloadsQuery);
        deletePayloadsQuery.executeUpdate();

        String deleteMessages;
        if (getDatabaseDialect().toString().toLowerCase().contains("mysql")) {
            StringBuilder delQuery = new StringBuilder(
                    "DELETE msg FROM nx_message msg\n" + 
                    "INNER JOIN nx_conversation conv ON conv.nx_conversation_id = msg.nx_conversation_id\n" + 
                    "INNER JOIN nx_choreography choreo ON conv.nx_choreography_id = choreo.nx_choreography_id\n" + 
                    "INNER JOIN nx_partner partner ON conv.nx_partner_id = partner.nx_partner_id\n" + 
                    "INNER JOIN nx_action action ON conv.current_nx_action_id = action.nx_action_id");
            appendMsgWhereCause(status, nxChoreographyId, nxPartnerId, conversationId, messageId, start, end, delQuery);
            deleteMessages = delQuery.toString();
        } else {
            deleteMessages = "DELETE FROM nx_message WHERE nx_message_id IN (" + getMessages + ')';
        }

        SQLQuery deleteMessagesQuery = sessionFactory.getCurrentSession().createSQLQuery(deleteMessages);
        setTimestampFields(start, end, deleteMessagesQuery);
        deleteMessagesQuery.executeUpdate();
    }

    @Transactional
    public void removeConversations(String status, int nxChoreographyId, int nxPartnerId, String conversationId, Date start, Date end) {
        StringBuilder getConversations = new StringBuilder(
                "SELECT conv.nx_conversation_id conv\n" +
                "FROM nx_conversation conv\n" +
                "INNER JOIN nx_choreography choreo ON conv.nx_choreography_id = choreo.nx_choreography_id\n" +
                "INNER JOIN nx_partner partner ON conv.nx_partner_id = partner.nx_partner_id");
        appendConvWhereCause(status, nxChoreographyId, nxPartnerId, conversationId, start, end, getConversations, false);

        String getMessages = "SELECT nx_message_id FROM nx_message WHERE nx_conversation_id IN (" + getConversations + ')';
        String deleteLabels = "DELETE FROM nx_message_label WHERE nx_message_id IN (" + getMessages + ')';
        String deletePayloads = "DELETE FROM nx_message_payload WHERE nx_message_id IN (" + getMessages + ')';

        SQLQuery deletePayloadsQuery = sessionFactory.getCurrentSession().createSQLQuery(deletePayloads);
        setTimestampFields(start, end, deletePayloadsQuery);
        deletePayloadsQuery.executeUpdate();

        SQLQuery deleteLabelsQuery = sessionFactory.getCurrentSession().createSQLQuery(deleteLabels);
        setTimestampFields(start, end, deleteLabelsQuery);
        deleteLabelsQuery.executeUpdate();

        String deleteMessages = "DELETE FROM nx_message WHERE nx_conversation_id IN (" + getConversations + ')';
        SQLQuery deleteMessagesQuery = sessionFactory.getCurrentSession().createSQLQuery(deleteMessages);
        setTimestampFields(start, end, deleteMessagesQuery);
        deleteMessagesQuery.executeUpdate();

        String deleteConversations;
        if (getDatabaseDialect().toString().toLowerCase().contains("mysql")) {
            StringBuilder sqlQueryConv = new StringBuilder("DELETE conv " +
                    "FROM nx_conversation conv " +
                    "INNER JOIN nx_choreography choreo " +
                    "ON conv.nx_choreography_id = choreo.nx_choreography_id " +
                    "INNER JOIN nx_partner partner " +
                    "ON  conv.nx_partner_id = partner.nx_partner_id " +
                    "INNER JOIN nx_action action " +
                    "ON conv.current_nx_action_id = action.nx_action_id");
            appendConvWhereCause(status, nxChoreographyId, nxPartnerId, conversationId, start, end, sqlQueryConv, false);
            deleteConversations = sqlQueryConv.toString();
        } else {
            deleteConversations = "DELETE FROM nx_conversation WHERE nx_conversation_id IN (" + getConversations + ')';
        }

        SQLQuery deleteConversationsQuery = sessionFactory.getCurrentSession().createSQLQuery(deleteConversations);
        setTimestampFields(start, end, deleteConversationsQuery);
        deleteConversationsQuery.executeUpdate();
    }

    protected void setTimestampFields(Date start, Date end, SQLQuery queryConv) {
        if (start != null) {
            queryConv.setTimestamp("start", start);
        }
        if (end != null) {
            queryConv.setTimestamp("end", end);
        }
    }

    private void appendMsgWhereCause(String status, int nxChoreographyId, int nxPartnerId, String conversationId, String messageId, Date start, Date end, StringBuilder sqlQuery) {
        if (StringUtils.isNotEmpty(status) || nxChoreographyId > 0 || nxPartnerId > 0 || StringUtils.isNotEmpty(conversationId) || start != null || end != null) {
            sqlQuery.append(" WHERE ");
            String prefix = "";
            if (start != null) {
                sqlQuery.append("msg.created_date >= :start");
                prefix = " AND ";
            }
            if (end != null) {
                sqlQuery.append(prefix + " msg.created_date <= :end");
                prefix = " AND ";
            }
            if (StringUtils.isNotBlank(status)) {
                if (status.contains(",")) {
                    sqlQuery.append(prefix + " msg.status in (" + status + ")");

                } else {
                    sqlQuery.append(prefix + " msg.status = " + status);
                }
                prefix = " AND ";
            }
            if (nxPartnerId > 0) {
                sqlQuery.append(prefix + " partner.nx_partner_id = " + nxPartnerId);
                prefix = " AND ";
            }
            if (nxChoreographyId > 0) {
                sqlQuery.append(prefix + " choreo.nx_choreography_id = " + nxChoreographyId);
                prefix = " AND ";
            }
            if (StringUtils.isNotBlank(messageId)) {
                sqlQuery.append(prefix + " msg.message_id like '" + messageId + "'");
            }
            if (StringUtils.isNotBlank(conversationId)) {
                sqlQuery.append(prefix + " conv.conversation_id like '" + conversationId + "'");
            }
        }
    }

    protected void appendConvWhereCause(String status, int nxChoreographyId, int nxPartnerId, String conversationId, Date start, Date end,
                                        StringBuilder sqlQuery, boolean isPostgreSql) {
        if (StringUtils.isNotEmpty(status) || nxChoreographyId > 0 || nxPartnerId > 0 || StringUtils.isNotEmpty(conversationId) || start != null || end != null) {
            if (isPostgreSql) {
                sqlQuery.append(" AND ");
            } else {
                sqlQuery.append(" WHERE ");
            }
            String prefix = "";
            if (start != null) {
                sqlQuery.append("conv.created_date >= :start");
                prefix = " AND ";
            }
            if (end != null) {
                sqlQuery.append(prefix + " conv.created_date <= :end");
                prefix = " AND ";
            }
            if (StringUtils.isNotBlank(status)) {
                if (status.contains(",")) {
                    sqlQuery.append(prefix + " conv.status in (" + status + ")");

                } else {
                    sqlQuery.append(prefix + " conv.status = " + status);
                }
                prefix = " AND ";
            }
            if (nxPartnerId > 0) {
                sqlQuery.append(prefix + " partner.nx_partner_id = " + nxPartnerId);
                prefix = " AND ";
            }
            if (nxChoreographyId > 0) {
                sqlQuery.append(prefix + " choreo.nx_choreography_id = " + nxChoreographyId);
                prefix = " AND ";
            }
            if (StringUtils.isNotBlank(conversationId)) {
                sqlQuery.append(prefix + " conv.conversation_id like '" + conversationId + "'");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#removeConversations(java.util.Date, java.util.Date)
     */
    public long removeConversations(Date start, Date end) throws NexusException {

        Session session = sessionFactory.getCurrentSession();
        StringBuilder query = new StringBuilder("delete from nx_message_label");
        Map<String, Timestamp> map = null;
        if (start != null || end != null) {
            query.append(" where (select message.nx_message_id from nx_message message, nx_conversation conv where " +
                    "nx_message_label.nx_message_id = message.nx_message_id and message.nx_conversation_id = conv.nx_conversation_id");
            map = appendQueryDate(query, "conv", start, end);
            query.append(") is not null");
        }
        LOG.debug("sql1: " + query);
        Query sqlquery1 = session.createSQLQuery(query.toString());
        if (map != null) {
            for (String name : map.keySet()) {
                sqlquery1.setTimestamp(name, map.get(name));
            }
        }

        map = null;
        query = new StringBuilder("delete from nx_message_payload");
        if (start != null || end != null) {
            query.append(" where (select message.nx_message_id from nx_message message, nx_conversation conv where " +
                    "nx_message_payload.nx_message_id = message.nx_message_id and message.nx_conversation_id = conv.nx_conversation_id");
            map = appendQueryDate(query, "conv", start, end);
            query.append(") is not null");
        }

        LOG.debug("sql2: " + query);
        Query sqlquery2 = session.createSQLQuery(query.toString());
        if (map != null) {
            for (String name : map.keySet()) {
                sqlquery2.setTimestamp(name, map.get(name));
            }
        }

        map = null;
        query = new StringBuilder("delete from nx_message where referenced_nx_message_id is not null");
        if (start != null || end != null) {
            query.append(" and (select conv.nx_conversation_id from nx_conversation conv where nx_message.nx_conversation_id = conv.nx_conversation_id");
            map = appendQueryDate(query, "conv", start, end);
            query.append(") is not null");
        }

        LOG.debug("sql3: " + query);
        Query sqlquery3 = session.createSQLQuery(query.toString());
        if (map != null) {
            for (String name : map.keySet()) {
                sqlquery3.setTimestamp(name, map.get(name));
            }
        }

        query = new StringBuilder("delete from nx_message");
        if (start != null || end != null) {
            query.append(" where (select conv.nx_conversation_id from nx_conversation conv where nx_message.nx_conversation_id = conv.nx_conversation_id");
            map = appendQueryDate(query, "conv", start, end);
            query.append(") is not null");
        }

        LOG.debug("sql4: " + query);
        Query sqlquery4 = session.createSQLQuery(query.toString());
        if (map != null) {
            for (String name : map.keySet()) {
                sqlquery4.setTimestamp(name, map.get(name));
            }
        }

        query = new StringBuilder("delete from nx_conversation");

        map = appendQueryDate(query, "nx_conversation", start, end);
        LOG.debug("sql5: " + query);
        Query sqlquery5 = session.createSQLQuery(query.toString());
        for (String name : map.keySet()) {
            sqlquery5.setTimestamp(name, map.get(name));
        }

        sqlquery1.executeUpdate();
        int result = sqlquery2.executeUpdate();
        sqlquery3.executeUpdate();
        sqlquery4.executeUpdate();
        sqlquery5.executeUpdate();

        return result;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getConversationsCount(java.lang.String, int, int, java.lang.String, java.util.Date, java.util.Date, int, boolean)
     */
    public int getConversationsCount(String status, int nxChoreographyId, int nxPartnerId, String conversationId,
                                     Date start, Date end, int field, boolean ascending) throws NexusException {

        return getCountThroughSessionFind(getConversationsForReportCriteria(status, nxChoreographyId, nxPartnerId,
                conversationId, start, end, SORT_NONE, ascending));
    }

    /**
     * @param status
     * @param nxChoreographyId
     * @param nxPartnerId
     * @param conversationId
     * @param start
     * @param end
     * @param field
     * @param ascending
     * @return
     */
    private DetachedCriteria getConversationsForReportCriteria(String status, int nxChoreographyId, int nxPartnerId,
                                                               String conversationId, Date start, Date end, int field, boolean ascending) {

        DetachedCriteria dc = DetachedCriteria.forClass(ConversationPojo.class);

        if (status != null) {

            if (status.indexOf(',') == -1) {
                dc.add(Restrictions.eq("status", Integer.parseInt(status)));

            } else {
                String[] statusValues = status.split(",");
                Integer[] intValues = new Integer[statusValues.length];
                for (int i = 0; i < statusValues.length; i++) {
                    intValues[i] = Integer.parseInt(statusValues[i]);
                }
                dc.add(Restrictions.in("status", intValues));
            }
        }
        if (nxChoreographyId != 0) {
            dc.createCriteria("choreography").add(Restrictions.eq("nxChoreographyId", nxChoreographyId));
        }
        if (nxPartnerId != 0) {
            dc.createCriteria("partner").add(Restrictions.eq("nxPartnerId", nxPartnerId));
        }
        if (conversationId != null) {
            dc.add(Restrictions.like("conversationId", conversationId.trim()));
        }
        if (start != null) {
            dc.add(Restrictions.ge("createdDate", start));
        }
        if (end != null) {

            dc.add(Restrictions.le("createdDate", end));
        }

        Order order = getSortOrder(field, ascending);
        if (order != null) {
            dc.addOrder(order);
        }

        return dc;
    }

    /**
     * @param status
     * @param nxChoreographyId
     * @param nxPartnerId
     * @param conversationId
     * @param messageId
     * @param type
     * @param start
     * @param end
     * @param field
     * @param ascending
     * @return
     */
    private DetachedCriteria getMessagesForReportCriteria(String status, int nxChoreographyId, int nxPartnerId,
                                                          String conversationId, String messageId, Integer type, Date start, Date end, int field, boolean ascending) {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);

        if (status != null) {
            if (status.indexOf(',') == -1) {
                dc.add(Restrictions.eq("status", Integer.parseInt(status)));

            } else {
                String[] statusValues = status.split(",");
                Integer[] intValues = new Integer[statusValues.length];
                for (int i = 0; i < statusValues.length; i++) {
                    intValues[i] = Integer.parseInt(statusValues[i]);
                }
                dc.add(Restrictions.in("status", intValues));
            }
        }

        DetachedCriteria conv = null;
        if (conversationId != null) {
            conv = dc.createCriteria("conversation");
            conv.add(Restrictions.like("conversationId", conversationId.trim()));
        }

        if (nxChoreographyId != 0) {
            if (conv == null) {
                conv = dc.createCriteria("conversation");
            }
            conv.createCriteria("choreography").add(
                    Restrictions.eq("nxChoreographyId", nxChoreographyId));
        }
        if (nxPartnerId != 0) {
            if (conv == null) {
                conv = dc.createCriteria("conversation");
            }
            conv.createCriteria("partner").add(
                    Restrictions.eq("nxPartnerId", nxPartnerId));
        }

        if (messageId != null) {
            dc.add(Restrictions.like("messageId", messageId.trim()));
        }
        if (type != null) {
            dc.add(Restrictions.eq("type", type));
        }
        if (start != null) {
            dc.add(Restrictions.ge("createdDate", start));
        }
        if (end != null) {
            dc.add(Restrictions.le("createdDate", end));
        }

        Order order = getSortOrder(field, ascending);
        if (order != null) {
            dc.addOrder(order);
        }
        return dc;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#storeTransaction(org.nexuse2e.pojo.ConversationPojo, org.nexuse2e.pojo.MessagePojo)
     */
    public void storeTransaction(ConversationPojo conversationPojo, MessagePojo messagePojo) throws NexusException {

        if (LOG.isTraceEnabled()) {
            LOG.trace(new LogMessage("(s)persisting state for message: "
                    + MessagePojo.getStatusName(messagePojo.getStatus())
                    + "/" + ConversationPojo.getStatusName(messagePojo.getConversation().getStatus()), messagePojo));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(new LogMessage("storeTransaction: " + conversationPojo + " - " + messagePojo, messagePojo));
        }

        saveOrUpdateRecord(conversationPojo);
        if (LOG.isTraceEnabled()) {
            printConversationInfo("stored conversation:", conversationPojo, null);
        }

    } // storeTransaction

    /**
     * @param field
     * @param ascending
     * @return
     */
    private Order getSortOrder(int field, boolean ascending) {

        Order order = null;

        switch (field) {
            case SORT_NONE:
                break;
            case SORT_CREATED:
                if (ascending) {
                    order = Order.asc("createdDate");
                } else {
                    order = Order.desc("createdDate");
                }
                break;
            case SORT_MODIFIED:
                if (ascending) {
                    order = Order.asc("lastModifiedDate");
                } else {
                    order = Order.desc("lastModifiedDate");
                }
                break;
            case SORT_STATUS:
                if (ascending) {
                    order = Order.asc("status");
                } else {
                    order = Order.desc("status");
                }
                break;
            case SORT_CPAID:
                if (ascending) {
                    order = Order.asc("choreographyId");
                } else {
                    order = Order.desc("choreographyId");
                }
                break;
            case SORT_ACTION:
                if (ascending) {
                    order = Order.asc("action");
                } else {
                    order = Order.desc("action");
                }
                break;
        }

        return order;
    } // getSortString

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getConversationsByPartner(org.nexuse2e.pojo.PartnerPojo)
     */
    @SuppressWarnings("unchecked")
    public List<ConversationPojo> getConversationsByPartner(PartnerPojo partner) {

        //String query = "from ConversationPojo conv where conv.partner.nxPartnerId=" + partner.getNxPartnerId();

        DetachedCriteria dc = DetachedCriteria.forClass(ConversationPojo.class);
        dc.createCriteria("partner").add(Restrictions.eq("nxPartnerId", partner.getNxPartnerId()));

        return (List<ConversationPojo>) getListThroughSessionFind(dc, 0, 0);

    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getConversationsByPartnerAndChoreography(org.nexuse2e.pojo.PartnerPojo, org.nexuse2e.pojo.ChoreographyPojo)
     */
    @SuppressWarnings("unchecked")
    public List<ConversationPojo> getConversationsByPartnerAndChoreography(PartnerPojo partner,
                                                                           ChoreographyPojo choreography) throws NexusException {

        DetachedCriteria dc = DetachedCriteria.forClass(ConversationPojo.class);
        dc.createCriteria("partner").add(Restrictions.eq("nxPartnerId", partner.getNxPartnerId()))
                .createCriteria("choreography").add(
                Restrictions.eq("nxChoreographyId", choreography.getNxChoreographyId()));

        return (List<ConversationPojo>) getListThroughSessionFind(dc, 0, 0);
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getConversationsByChoreography(org.nexuse2e.pojo.ChoreographyPojo)
     */
    @SuppressWarnings("unchecked")
    public List<ConversationPojo> getConversationsByChoreography(ChoreographyPojo choreography) {

        DetachedCriteria dc = DetachedCriteria.forClass(ConversationPojo.class);
        dc.createCriteria("choreography").add(
                Restrictions.eq("nxChoreographyId", choreography.getNxChoreographyId()));

        return (List<ConversationPojo>) getListThroughSessionFind(dc, 0, 0);
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getMessagesByPartner(org.nexuse2e.pojo.PartnerPojo, int, boolean)
     */
    @SuppressWarnings("unchecked")
    public List<MessagePojo> getMessagesByPartner(PartnerPojo partner, int field, boolean ascending)
            throws NexusException {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
        dc.createCriteria("covnersation").createCriteria("partner").add(
                Restrictions.eq("nxPartnerId", partner.getNxPartnerId()));
        Order order = getSortOrder(field, ascending);
        if (order != null) {
            dc.addOrder(order);
        }

        return (List<MessagePojo>) getListThroughSessionFind(dc, 0, 0);
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#deleteMessage(org.nexuse2e.pojo.MessagePojo)
     */
    public void deleteMessage(MessagePojo messagePojo) throws NexusException {

        LOG.debug("deleteMessage: " + messagePojo);
        deleteRecord(messagePojo);
    } // updateMessage

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#deleteConversation(org.nexuse2e.pojo.ConversationPojo)
     */
    public void deleteConversation(ConversationPojo conversationPojo) throws NexusException {

        LOG.debug("deleteMessage: " + conversationPojo);
        deleteRecords(conversationPojo.getMessages());
        deleteRecord(conversationPojo);
    } // updateMessage

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getMessagesByPartnerAndDirection(org.nexuse2e.pojo.PartnerPojo, boolean, int, boolean)
     */
    @SuppressWarnings("unchecked")
    public List<MessagePojo> getMessagesByPartnerAndDirection(PartnerPojo partner, boolean outbound, int field,
                                                              boolean ascending) throws NexusException {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
        dc.createCriteria("conversation").createCriteria("partner").add(
                Restrictions.eq("nxPartnerId", partner.getNxPartnerId()));
        dc.add(Restrictions.eq("outbound", (outbound ? 1 : 0)));

        Order order = getSortOrder(field, ascending);
        if (order != null) {
            dc.addOrder(order);
        }

        return (List<MessagePojo>) getListThroughSessionFind(dc, 0, 0);
    }

    public MessagePojo getLastSuccessfulMessageByChoreographyAndDirection(ChoreographyPojo choreography, boolean outbound) {
        DetachedCriteria criteria = DetachedCriteria.forClass(MessagePojo.class);
        criteria.createCriteria("conversation").createCriteria("choreography").add(Restrictions.eq("nxChoreographyId", choreography.getNxChoreographyId()));
        return getLastSuccessfulMessageByCriteriaAndDirection(criteria, outbound);
    }

    public MessagePojo getLastSuccessfulMessageByPartnerAndDirection(PartnerPojo partner, boolean outbound) {
        DetachedCriteria criteria = DetachedCriteria.forClass(MessagePojo.class);
        criteria.createCriteria("conversation").createCriteria("partner").add(Restrictions.eq("nxPartnerId", partner.getNxPartnerId()));
        return getLastSuccessfulMessageByCriteriaAndDirection(criteria, outbound);
    }

    private MessagePojo getLastSuccessfulMessageByCriteriaAndDirection(DetachedCriteria criteria, boolean outbound) {
        criteria.add(Restrictions.eq("outbound", outbound));
        criteria.add(Restrictions.eq("status", MessageStatus.COMPLETED.getOrdinal()));
        criteria.add(Restrictions.eq("type", INT_MESSAGE_TYPE_NORMAL));
        criteria.addOrder(Order.desc("createdDate"));

        List<?> results = getListThroughSessionFind(criteria, 0, 1);
        return results.isEmpty() ? null : (MessagePojo) results.get(0);
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getMessagesByActionPartnerDirectionAndStatus(org.nexuse2e.pojo.ActionPojo, org.nexuse2e.pojo.PartnerPojo, boolean, int, int, boolean)
     */
    @SuppressWarnings("unchecked")
    public List<MessagePojo> getMessagesByActionPartnerDirectionAndStatus(
            ActionPojo action, PartnerPojo partner, boolean outbound, int status, int field, boolean ascending) {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);

        dc.createCriteria("conversation").createCriteria("partner").add(
                Restrictions.eq("nxPartnerId", partner.getNxPartnerId()));
        DetachedCriteria actionCriteria = dc.createCriteria("action");
        actionCriteria.add(Restrictions.eq("name", action.getName()));
        actionCriteria.createCriteria("choreography").add(
                Restrictions.eq("name", action.getChoreography().getName()));
        dc.add(Restrictions.eq("outbound", outbound));
        dc.add(Restrictions.eq("status", status));

        Order order = getSortOrder(field, ascending);
        if (order != null) {
            dc.addOrder(order);
        }

        return (List<MessagePojo>) getListThroughSessionFind(dc, 0, 0);
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getMessagesByChoreographyAndPartner(org.nexuse2e.pojo.ChoreographyPojo, org.nexuse2e.pojo.PartnerPojo, int, boolean)
     */
    @SuppressWarnings("unchecked")
    public List<MessagePojo> getMessagesByChoreographyAndPartner(ChoreographyPojo choreography, PartnerPojo partner,
                                                                 int field, boolean ascending) {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
        dc.createCriteria("conversation").createCriteria("choreography").add(
                Restrictions.eq("nxChoreographyId", choreography.getNxChoreographyId()));
        dc.createCriteria("conversation").createCriteria("partner").add(
                Restrictions.eq("nxPartnerId", partner.getNxPartnerId()));
        Order order = getSortOrder(field, ascending);
        if (order != null) {
            dc.addOrder(order);
        }

        return (List<MessagePojo>) getListThroughSessionFind(dc, 0, 0);
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getMessagesByChoreographyPartnerAndConversation(org.nexuse2e.pojo.ChoreographyPojo, org.nexuse2e.pojo.PartnerPojo, org.nexuse2e.pojo.ConversationPojo, int, boolean)
     */
    @SuppressWarnings("unchecked")
    public List<MessagePojo> getMessagesByChoreographyPartnerAndConversation(ChoreographyPojo choreography,
                                                                             PartnerPojo partner, ConversationPojo conversation, int field, boolean ascending) {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);

        dc.createCriteria("conversation").add(
                Restrictions.eq("nxConversationId", conversation.getNxConversationId()));
        dc.createCriteria("conversation").createCriteria("choreography").add(
                Restrictions.eq("nxChoreographyId", choreography.getNxChoreographyId()));
        dc.createCriteria("conversation").createCriteria("partner").add(
                Restrictions.eq("nxPartnerId", partner.getNxPartnerId()));

        Order order = getSortOrder(field, ascending);
        if (order != null) {
            dc.addOrder(order);
        }

        return (List<MessagePojo>) getListThroughSessionFind(dc, 0, 0);
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#deleteLogEntry(org.nexuse2e.pojo.LogPojo)
     */
    public void deleteLogEntry(LogPojo logEntry) throws NexusException {

        deleteRecord(logEntry);
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#fetchLazyPayloads(org.nexuse2e.pojo.MessagePojo)
     */
    public List<MessagePayloadPojo> fetchLazyPayloads(MessagePojo message) {

        lockRecord(message);
        List<MessagePayloadPojo> payloads = message.getMessagePayloads();
        // Force db access 
        payloads.size();
        return payloads;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#fetchLazyMessages(org.nexuse2e.pojo.ConversationPojo)
     */
    public List<MessagePojo> fetchLazyMessages(ConversationPojo conversation) {

        lockRecord(conversation);
        List<MessagePojo> messages = conversation.getMessages();
        // Force db access 
        messages.size();
        return messages;
    }

    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#updateTransaction(org.nexuse2e.pojo.MessagePojo, boolean)
     */
    public void updateTransaction(MessagePojo message, boolean force) throws NexusException, StateTransitionException {

        LOG.trace(new LogMessage(
                "persisting state for message: " + message.getConversation().getStatusName(), message));

        int messageStatus = message.getStatus();
        int conversationStatus = message.getConversation().getStatus();

        if (messageStatus < MessageStatus.FAILED.getOrdinal()
                || messageStatus > MessageStatus.STOPPED.getOrdinal()) {
            throw new IllegalArgumentException("Illegal message status: " + messageStatus
                    + ", only values >= " + MessageStatus.FAILED.getOrdinal()
                    + " and <= " + MessageStatus.STOPPED.getOrdinal() + " allowed");
        }

        if (conversationStatus < Constants.CONVERSATION_STATUS_ERROR
                || conversationStatus > Constants.CONVERSATION_STATUS_COMPLETED) {
            throw new IllegalArgumentException("Illegal conversation status: " + conversationStatus
                    + ", only values >= " + Constants.CONVERSATION_STATUS_ERROR
                    + " and <= " + Constants.CONVERSATION_STATUS_COMPLETED + " allowed");
        }

        int allowedMessageStatus = messageStatus;
        int allowedConversationStatus = conversationStatus;

        MessagePojo persistentMessage;
        ConversationPojo persistentConversation;
        if (message.getNxMessageId() > 0) {
            persistentMessage = (MessagePojo) sessionFactory.getCurrentSession().get(MessagePojo.class, message.getNxMessageId());
            persistentConversation = (persistentMessage != null ? persistentMessage.getConversation() : null);
        } else {
            persistentMessage = message;
            if (message.getConversation() != null && message.getConversation().getNxConversationId() > 0) {
                persistentConversation = (ConversationPojo) sessionFactory.getCurrentSession().get(
                        ConversationPojo.class, message.getConversation().getNxConversationId());
            } else {
                persistentConversation = message.getConversation();
            }
        }
        if (persistentMessage != null) {
            if (!force) {
                allowedMessageStatus = getAllowedMessageTransitionStatus(persistentMessage.getStatus(), messageStatus);
                allowedConversationStatus = getAllowedConversationTransitionStatus(persistentMessage.getConversation().getStatus(), conversationStatus);
            }
            if (message.getStatus() != allowedMessageStatus) {
                message.setStatus(allowedMessageStatus);
            }
            message.getConversation().setStatus(allowedConversationStatus);

            if (messageStatus == allowedMessageStatus && conversationStatus == allowedConversationStatus) {
                if (persistentMessage != null) {
                    persistentMessage.setProperties(message);
                }
                if (persistentConversation != null) {
                    persistentConversation.setProperties(message.getConversation());
                    persistentConversation.addMessage(message);
                    for (MessagePojo m : message.getConversation().getMessages()) {
                        if (m != null && m != message && m.getNxMessageId() == 0) {
                            persistentConversation.addMessage(m);
                        }
                    }
                    persistentConversation.setModifiedDate(new Date());
                    sessionFactory.getCurrentSession().saveOrUpdate(persistentConversation);
                    for (MessagePojo mesg : persistentConversation.getMessages()) {
                        sessionFactory.getCurrentSession().saveOrUpdate(mesg);
                    }
                }
            }
        }


        String errMsg = null;

        if (allowedMessageStatus != messageStatus) {
            errMsg = "Illegal transition: Cannot set message status for " + message.getMessageId() + " from " +
                    MessagePojo.getStatusName(allowedMessageStatus) + " to " + MessagePojo.getStatusName(messageStatus);
        }
        if (allowedConversationStatus != conversationStatus) {
            if (errMsg != null) {
                errMsg += ", cannot set conversation status for " + message.getConversation().getConversationId() +
                        " from " + ConversationPojo.getStatusName(allowedConversationStatus) + " to " + ConversationPojo.getStatusName(conversationStatus);
            } else {
                errMsg = "Illegal transition: Cannot set conversation status for " + message.getConversation().getConversationId() + " from "
                        + ConversationPojo.getStatusName(allowedConversationStatus) + " to " + ConversationPojo.getStatusName(conversationStatus);
            }
        }
        if (errMsg != null) {
            throw new StateTransitionException(errMsg);
        }


    } // updateTransaction

    public void updateTransaction(MessagePojo message, UpdateTransactionOperation operation, boolean force) throws NexusException, StateTransitionException {

        LOG.trace(new LogMessage("persisting state for message: " + message.getConversation().getStatusName(), message));

        // get persistent message and conversation
        MessagePojo persistentMessage;
        ConversationPojo persistentConversation;
        Session session = sessionFactory.getCurrentSession();

        if (message.getConversation() != null && message.getConversation().getNxConversationId() > 0) {
            persistentConversation = (ConversationPojo) session.get(ConversationPojo.class, message.getConversation().getNxConversationId());
        } else {
            persistentConversation = message.getConversation();
        }
        if (message.getNxMessageId() > 0) {
            persistentMessage = (MessagePojo) session.get(MessagePojo.class, message.getNxMessageId());
        } else {
            persistentMessage = message;
        }

        if (persistentConversation != null && persistentMessage != null) {
            String oldActionName = (persistentConversation.getCurrentAction() == null ? null : persistentConversation.getCurrentAction().getName());

            // remember persistent status for state transition check
            int persistentMessageStatus = persistentMessage.getStatus();
            int persistentConversationStatus = persistentConversation.getStatus();

            // perform update operation
            UpdateScope updateScope = UpdateScope.NOTHING;
            MessagePojo persistentReferencedMessage = null;
            if (persistentMessage.getReferencedMessage() != null) {
                persistentReferencedMessage = (MessagePojo) session.get(MessagePojo.class, persistentMessage.getReferencedMessage().getNxId());
            }
            if (operation != null) {
                updateScope = operation.update(persistentConversation, persistentMessage, persistentReferencedMessage);
                if (updateScope == null) {
                    updateScope = UpdateScope.NOTHING;
                }
            }

            int allowedMessageStatus = persistentMessage.getStatus();
            int allowedConversationStatus = persistentConversation.getStatus();
            if (!force) {
                if (persistentMessage.getNxId() > 0) {
                    allowedMessageStatus = getAllowedMessageTransitionStatus(persistentMessageStatus, persistentMessage.getStatus());
                }
                if (persistentConversation.getNxId() > 0) {
                    allowedConversationStatus = getAllowedConversationTransitionStatus(persistentConversationStatus, persistentConversation.getStatus());
                }
            }
            int messageStatus = persistentMessage.getStatus();
            int conversationStatus = persistentConversation.getStatus();
            if (persistentMessage.getStatus() != allowedMessageStatus) {
                persistentMessage.setStatus(allowedMessageStatus);
            }
            persistentMessage.setHeaderData(message.getHeaderData());
            if (persistentConversation.getStatus() != allowedConversationStatus) {
                persistentConversation.setStatus(allowedConversationStatus);
            }

            // persist result
            List<NEXUSe2ePojo> entities = new ArrayList<NEXUSe2ePojo>();
            if (updateScope.updateConversation()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("persisting conv " + persistentConversation.getConversationId() + " with action " +
                            persistentConversation.getCurrentAction().getName() + " (was " + oldActionName + ")");
                }
                entities.add(persistentConversation);
            }
            if (updateScope.updateMessage()) {
                entities.add(persistentMessage);
            }
            if (updateScope.updateReferencedMessage() && persistentReferencedMessage != null) {
                entities.add(persistentReferencedMessage);
            }
            for (Object entity : entities) {
                session.saveOrUpdate(entity);
            }

            // write possible changes back so that we can go on working with original message
            if (updateScope.updateConversation()) {
                message.getConversation().setStatus(allowedConversationStatus);
                message.getConversation().setNxId(persistentConversation.getNxId());
                message.getConversation().setCurrentAction(persistentConversation.getCurrentAction());
                if (persistentConversation.getCurrentAction() != null) { // force current action to be loaded
                    persistentConversation.getCurrentAction().getChoreography();
                    persistentConversation.getCurrentAction().getName();
                }
                message.getConversation().setEndDate(persistentConversation.getEndDate());
                message.getConversation().setModifiedDate(persistentConversation.getModifiedDate());
                message.getConversation().setMessages(persistentConversation.getMessages());
            }
            if (updateScope.updateMessage() || updateScope.updateReferencedMessage()) {
                message.setReferencedMessage(persistentMessage.getReferencedMessage());
                if (message.getStatus() != allowedMessageStatus) {
                    message.setStatus(allowedMessageStatus);
                }
                message.setEndDate(persistentMessage.getEndDate());
                message.setNxId(message.getNxId());
                message.setRetries(message.getRetries());
                message.setModifiedDate(persistentMessage.getModifiedDate());
            }


            String errMsg = null;

            if (allowedMessageStatus != messageStatus && updateScope != UpdateScope.NOTHING) {
                errMsg = "Illegal transition: Cannot set message status for " + message.getMessageId() + " from " +
                        MessagePojo.getStatusName(allowedMessageStatus) + " to " + MessagePojo.getStatusName(messageStatus);
            }
            if (allowedConversationStatus != conversationStatus && updateScope.updateConversation()) {
                if (errMsg != null) {
                    errMsg += ", cannot set conversation status for " + message.getConversation().getConversationId() + " from " + ConversationPojo.getStatusName(allowedConversationStatus) +
                            " to " + ConversationPojo.getStatusName(conversationStatus);
                } else {
                    errMsg = "Illegal transition: Cannot set conversation status for " + message.getConversation().getConversationId() + " from "
                            + ConversationPojo.getStatusName(allowedConversationStatus) + " to " + ConversationPojo.getStatusName(conversationStatus);
                }
            }
            if (errMsg != null) {
                throw new StateTransitionException(errMsg);
            }
        }

    } // updateTransaction


    public void updateRetryCount(MessagePojo message) throws NexusException {
        MessagePojo persistentMessage = (MessagePojo) sessionFactory.getCurrentSession().get(MessagePojo.class, message.getNxMessageId());
        if (persistentMessage != null) {
            persistentMessage.setRetries(message.getRetries());
            sessionFactory.getCurrentSession().saveOrUpdate(persistentMessage);
        }
    }

    protected int getAllowedConversationTransitionStatus(int persistentConversationStatus, int conversationStatus) {

        if (persistentConversationStatus == conversationStatus) {
            return conversationStatus;
        }
        int[] validStates = followUpConversationStates.get(persistentConversationStatus);
        if (validStates != null) {
            for (int status : validStates) {
                if (status == conversationStatus) {
                    return conversationStatus;
                }
            }
        }

        return persistentConversationStatus;
    }

    protected int getAllowedMessageTransitionStatus(int persistentMessageStatus, int messageStatus) {

        if (persistentMessageStatus == messageStatus) {
            return messageStatus;
        }
        int[] validStates = followUpMessageStates.get(persistentMessageStatus);
        if (validStates != null) {
            for (int status : validStates) {
                if (status == messageStatus) {
                    return messageStatus;
                }
            }
        }
        return persistentMessageStatus;
    }


    /* (non-Javadoc)
     * @see org.nexuse2e.dao.TransactionDAO#getCreatedMessagesSinceCount(java.util.Date)
     */
    public int getCreatedMessagesSinceCount(Date since) throws NexusException {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
        dc.add(Restrictions.ge("createdDate", since));
        return getCountThroughSessionFind(dc);
    }

    public List<int[]> getConversationStatesSince(Date since) {

        DetachedCriteria dc = DetachedCriteria.forClass(ConversationPojo.class);
        dc.add(Restrictions.ge("createdDate", since));
        ProjectionList pros = Projections.projectionList();
        pros.add(Projections.groupProperty("status"));
        pros.add(Projections.count("status"));
        dc.setProjection(pros);

        List<?> l = getListThroughSessionFind(dc, 0, Integer.MAX_VALUE);
        List<int[]> list = new ArrayList<int[]>(l.size());
        for (Object o : l) {
            int[] kv = new int[]{
                    ((Number) ((Object[]) o)[0]).intValue(),
                    ((Number) ((Object[]) o)[1]).intValue()
            };
            list.add(kv);
        }

        return list;
    }

    public List<int[]> getMessageStatesSince(Date since) {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
        dc.add(Restrictions.ge("createdDate", since));
        dc.add(Restrictions.eq("type", 1));
        ProjectionList pros = Projections.projectionList();
        pros.add(Projections.groupProperty("status"));
        pros.add(Projections.count("status"));
        dc.setProjection(pros);

        List<?> l = getListThroughSessionFind(dc, 0, Integer.MAX_VALUE);
        List<int[]> list = new ArrayList<int[]>(l.size());
        for (Object o : l) {
            int[] kv = new int[]{
                    ((Number) ((Object[]) o)[0]).intValue(),
                    ((Number) ((Object[]) o)[1]).intValue()
            };
            list.add(kv);
        }

        return list;
    }

    public List<String[]> getMessagesPerConversationSince(Date since) {

        DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
        dc.createAlias("conversation", "theConversation", Criteria.INNER_JOIN);
        dc.createAlias("theConversation.choreography", "theChoreography", Criteria.INNER_JOIN);
        ProjectionList pros = Projections.projectionList();
        pros.add(Projections.groupProperty("theChoreography.name"));
        pros.add(Projections.count("messageId"));
        dc.setProjection(pros);
        dc.add(Restrictions.ge("createdDate", since));
        dc.add(Restrictions.eq("type", 1));


        List<?> l = getListThroughSessionFind(dc, 0, Integer.MAX_VALUE);
        List<String[]> list = new ArrayList<String[]>(l.size());
        for (Object o : l) {
            String[] kv = new String[]{
                    (((Object[]) o)[0]).toString(),
                    ((Object[]) o)[1].toString()
            };
            list.add(kv);
        }

        return list;
    }

    public List<int[]> getMessagesPerHourLast24Hours() {

        String hourFunction = "HOUR(created_date)";
        DatabaseType t = getDatabaseType();
        if (t == DatabaseType.MSSQL) {
            hourFunction = "DATEPART(hour, created_date)";
        } else if (t == DatabaseType.ORACLE) {
            hourFunction = "EXTRACT(HOUR FROM created_date)";
        }

        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        int currentHourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        List<int[]> list = new ArrayList<int[]>(24);

        if (t == DatabaseType.DERBY) {
            // derby doesn't like functional expressions in GRUP BY, so we create one query per hour
            for (int i = 0; i < 24; i++) {
                DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
                dc.setProjection(Projections.count("nxMessageId"));
                Date from = cal.getTime();
                int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
                cal.add(Calendar.HOUR_OF_DAY, 1);
                Date to = cal.getTime();
                dc.add(Restrictions.eq("type", 1));
                dc.add(Restrictions.ge("createdDate", from));
                dc.add(Restrictions.lt("createdDate", to));

                List<?> l = getListThroughSessionFind(dc, 0, Integer.MAX_VALUE);
                Object o = l.get(0);
                int[] kv = new int[]{
                        hourOfDay,
                        ((Number) o).intValue()
                };
                list.add(kv);
            }
        } else {
            // default implementation for "normal" DBMS
            DetachedCriteria dc = DetachedCriteria.forClass(MessagePojo.class);
            ProjectionList pros = Projections.projectionList();
            pros.add(Projections.sqlGroupProjection(
                    hourFunction + " AS messageHour, COUNT(nx_message_id) AS messageCount", "messageHour",
                    new String[]{"messageHour", "messageCount"},
                    new Type[]{new IntegerType(), new IntegerType()}));
            dc.setProjection(pros);
            dc.add(Restrictions.ge("createdDate", cal.getTime()));
            dc.add(Restrictions.eq("type", 1));
            dc.addOrder(Order.asc("createdDate"));

            List<?> l = getListThroughSessionFind(dc, 0, Integer.MAX_VALUE);


            // create default 0-message entries
            for (int i = currentHourOfDay; i < currentHourOfDay + 24; i++) {
                list.add(new int[]{i % 24, 0});
            }

            // put list entries to appropriate positions
            for (Object o : l) {
                int hour = ((Number) ((Object[]) o)[0]).intValue();
                int value = ((Number) ((Object[]) o)[1]).intValue();

                int index = hour - currentHourOfDay;
                if (index < 0) {
                    index += 24;
                }
                list.set(index, new int[]{hour, value});
            }
        }

        return list;
    }

    public Session getDBSession() {
        return sessionFactory.getCurrentSession().getSessionFactory().openSession();
    }

    public void releaseDBSession(Session session) {
        session.close();
    }

    private Dialect getDatabaseDialect() {
        return ((SessionFactoryImpl) Engine.getInstance()
                .getBeanFactory().getBean("hibernateSessionFactory")).getDialect();
    }

    public Set<StatisticsCertificate> getStatisticsCertificates(List<ChoreographyPojo> choreographyPojos) {
        Set<StatisticsCertificate> certificates = new TreeSet<>();
        for (ChoreographyPojo choreographyPojo : choreographyPojos) {
            List<ParticipantPojo> participants = choreographyPojo.getParticipants();
            for (ParticipantPojo participantPojo : participants) {
                ConnectionPojo connection = participantPojo.getConnection();
                CertificatePojo partnerCertificate = connection.getCertificate();
                CertificatePojo localCertificate = participantPojo.getLocalCertificate();
                if (localCertificate != null) {
                    certificates.add(new StatisticsCertificate(localCertificate, true));
                }
                if (partnerCertificate != null) {
                    certificates.add(new StatisticsCertificate(partnerCertificate, false));
                }
            }
        }
        return certificates;
    }

}

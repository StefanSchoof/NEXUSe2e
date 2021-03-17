package org.nexuse2e.ui2.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.nexuse2e.Engine;
import org.nexuse2e.MessageStatus;
import org.nexuse2e.dao.TransactionDAO;
import org.nexuse2e.pojo.ConversationPojo;
import org.nexuse2e.pojo.MessagePojo;
import org.nexuse2e.reporting.StatisticsConversation;
import org.nexuse2e.reporting.StatisticsMessage;
import org.nexuse2e.util.DateWithTimezoneSerializer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.nexuse2e.util.DateUtil.getCurrentDateMinusWeeks;

public class TransactionReportingHandler implements Handler {
    // TODO get date range from filter
    public static final Date TWO_WEEKS_AGO = getCurrentDateMinusWeeks(2);

    @Override
    public boolean canHandle(String path, String method) {
        return ("GET".equalsIgnoreCase(method) && "/messages".equalsIgnoreCase(path)) ||
                ("GET".equalsIgnoreCase(method) && "/messages-count".equalsIgnoreCase(path)) ||
                ("GET".equalsIgnoreCase(method) && "/conversations".equalsIgnoreCase(path)) ||
                ("GET".equalsIgnoreCase(method) && "/conversations-count".equalsIgnoreCase(path));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = request.getPathInfo();
        if (path != null) {
            switch (StringUtils.lowerCase(path)) {
                case "/messages":
                    this.getMessages(request, response);
                    break;
                case "/messages-count":
                    this.getMessagesCount(request, response);
                    break;
                case "/conversations":
                    this.getConversations(request, response);
                    break;
                case "/conversations-count":
                    this.getConversationsCount(response);
                    break;
            }
        }
    }

    private void getMessagesCount(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String status = request.getParameter("status");
        long messagesCount = Engine.getInstance().getTransactionService().getMessagesCount(
                getStatusNumberFromName(status),
                0, 0, null, null,
                TWO_WEEKS_AGO,
                new Date());
        String message = new Gson().toJson(messagesCount);
        response.getOutputStream().print(message);
    }

    private String getStatusNumberFromName(String statusName) {
        try {
            return String.valueOf(MessageStatus.valueOf(statusName).getOrdinal());
        } catch (Exception e) {
            return null;
        }
    }

    private void getMessages(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String pageIndex = request.getParameter("pageIndex");
        String itemsPerPage = request.getParameter("itemsPerPage");
        String status = request.getParameter("status");
        if (NumberUtils.isNumber(pageIndex) && NumberUtils.isNumber(itemsPerPage)) {
            List<MessagePojo> reportMessages = Engine.getInstance().getTransactionService().getMessagesForReport(
                    getStatusNumberFromName(status),
                    0, 0, null, null, null,
                    TWO_WEEKS_AGO,
                    new Date(),
                    Integer.parseInt(itemsPerPage),
                    Integer.parseInt(pageIndex),
                    TransactionDAO.SORT_CREATED,
                    false);

            List<StatisticsMessage> messages = new LinkedList<>();
            for (MessagePojo messagePojo : reportMessages) {
                messages.add(new StatisticsMessage(messagePojo));
            }

            Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new DateWithTimezoneSerializer()).create();
            String messageJson = gson.toJson(messages);
            response.getOutputStream().print(messageJson);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Required parameters: pageIndex and itemsPerPage");
        }
    }

    private void getConversationsCount(HttpServletResponse response) throws Exception {
        long conversationsCount = Engine.getInstance().getTransactionService().getConversationsCount(TWO_WEEKS_AGO, new Date());
        String message = new Gson().toJson(conversationsCount);
        response.getOutputStream().print(message);
    }

    private void getConversations(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String pageIndex = request.getParameter("pageIndex");
        String itemsPerPage = request.getParameter("itemsPerPage");
        if (NumberUtils.isNumber(pageIndex) && NumberUtils.isNumber(itemsPerPage)) {
            List<ConversationPojo> reportConversations = Engine.getInstance().getTransactionService().getConversationsForReport(
                    null, 0, 0, null,
                    TWO_WEEKS_AGO,
                    new Date(),
                    Integer.parseInt(itemsPerPage),
                    Integer.parseInt(pageIndex),
                    TransactionDAO.SORT_CREATED,
                    false);
            List<StatisticsConversation> conversations = new LinkedList<>();
            for (ConversationPojo conversationPojo : reportConversations) {
                conversations.add(new StatisticsConversation(conversationPojo));
            }

            Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new DateWithTimezoneSerializer()).create();
            String conversationJson = gson.toJson(conversations);
            response.getOutputStream().print(conversationJson);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Required parameters: pageIndex and itemsPerPage");
        }
    }
}

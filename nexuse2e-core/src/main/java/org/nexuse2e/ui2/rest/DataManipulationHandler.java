package org.nexuse2e.ui2.rest;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nexuse2e.Engine;
import org.nexuse2e.NexusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import static org.nexuse2e.util.FileUtil.readAll;

public class DataManipulationHandler implements Handler {
    private static final Logger LOG = Logger.getLogger(DataManipulationHandler.class);

    @Override
    public boolean canHandle(String path, String method) {
        return ("POST".equalsIgnoreCase(method) && "/messages/requeue".equalsIgnoreCase(path)) ||
                ("POST".equalsIgnoreCase(method) && "/messages/stop".equalsIgnoreCase(path)) ||
                ("POST".equalsIgnoreCase(method) && "/conversations/delete".equalsIgnoreCase(path));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = request.getPathInfo();
        if (path != null) {
            switch (StringUtils.lowerCase(path)) {
                case "/messages/requeue":
                    this.requeueMessages(request, response);
                    break;
                case "/messages/stop":
                    this.stopMessages(request, response);
                    break;
                case "/conversations/delete":
                    this.deleteConversations(request, response);
                    break;
            }
        }
    }

    private void requeueMessages(HttpServletRequest request, HttpServletResponse response) {
    }

    private void stopMessages(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestBody = readAll(request.getInputStream());
        String[] messageIds = new Gson().fromJson(requestBody, String[].class);
        ArrayList<String> failedMessageIds = new ArrayList<>();

        for (String messageId : messageIds) {
            try {
                Engine.getInstance().getTransactionService().stopProcessingMessage(messageId);
            } catch (NexusException e) {
                failedMessageIds.add(messageId);
                LOG.error("An error occurred while trying to stop message" + messageId, e);
            }
        }

        if (failedMessageIds.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An internal server error occurred while trying to stop messages" + failedMessageIds);
        }
    }

    private void deleteConversations(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestBody = readAll(request.getInputStream());
        //TODO
        //ArrayList<String> failedConversations = new ArrayList<>();

        //for (//TODO) {
            /*try {
                Engine.getInstance().getTransactionService().deleteConversation(conversation); //vom Typ ConversationPojo
            } catch (NexusException e) {
                failedConversations.add(//TODO);
                LOG.error("An error occurred while trying to delete conversation" + // TODO , e);
            }*/
        //}

        /*if (//TODO.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An internal server error occurred while trying to delete conversations" + //TODO);
        }*/
    }

}

package com.thin.cqrsesorder.infrastructure;

import javax.servlet.http.HttpSession;

public class Session {
    HttpSession session;
    private static final int MAX_SAVED_RESPONSES = 5;

    HashMapQueue<String, Object> clientResponses;

    public Session(HttpSession session) {
        this.session = session;
        clientResponses = (HashMapQueue)session.getAttribute("clientResponses");
        if (null == clientResponses) {
            clientResponses = new HashMapQueue<>(MAX_SAVED_RESPONSES);
            session.setAttribute("clientResponses", clientResponses);
        }
    }

    public Object getResponse(String requestId) {
        return clientResponses.get(requestId);

    }

    public void addResponse(String requestId, Object response) {
        clientResponses.put(requestId, response);
    }

}
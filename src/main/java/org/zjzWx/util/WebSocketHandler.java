package org.zjzWx.util;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String,WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String code = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst("code");
        if(code==null || code.trim().isEmpty()){
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        WebSocketSession oldSession = sessions.put(code,session);
        if(oldSession!=null && oldSession.isOpen()){
            oldSession.close();
        }
        session.getAttributes().put("code",code);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object code = session.getAttributes().get("code");
        if(code!=null){
            sessions.remove(code.toString(),session);
        }
    }

    public void authorized(String code) {
        WebSocketSession session = sessions.remove(code);
        if(session!=null && session.isOpen()){
            try {
                session.sendMessage(new TextMessage("{\"type\":\"authorized\"}"));
                session.close(CloseStatus.NORMAL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

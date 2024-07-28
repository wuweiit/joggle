package com.wuweibi.bullet.websocket;

import org.springframework.http.HttpHeaders;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

    public static final String IP_ADDR = "IP.ADDR";

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        Map<String, Object> attributes = sec.getUserProperties();
        List<String> values = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        List<String> versions = request.getHeaders().get("version");

        attributes.put(HttpHeaders.AUTHORIZATION, null != values?values.get(0): "");
        attributes.put("version", null != versions?versions.get(0): "");
//        if (session != null) {
//            attributes.put(IP_ADDR, session.getAttribute("ip"));
//            Enumeration<String> names = session.getAttributeNames();
//            while (names.hasMoreElements()) {
//                String name = names.nextElement();
//                attributes.put(name, session.getAttribute(name));
//            }
//        }
    }
}

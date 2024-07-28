package com.wuweibi.bullet.utils;

import javax.websocket.server.HandshakeRequest;
import java.util.List;


/**
 * websocket 工具类
 * @author marker
 */
public class WebSocketUtil {


    /**
     * 获取websocket 请求头
     * @param request websocket请求
     * @param header 头名称
     * @return
     */
    public static String getHeader(HandshakeRequest request, String header) {
        List<String> values = request.getHeaders().get(header);
        return null != values?values.get(0): "";
    }
}

package com.guyi.kindredspirits.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置类
 *
 * @author 孤诣
 */
@Configuration
public class WebSocketConfig {

    /**
     * 注入 ServerEndpointExporter
     * 这个 bean 会自动注册使用了 @ServerEndpoint 注解声明的 Websocket endpoint
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}

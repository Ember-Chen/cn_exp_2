package com.websocketServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;


@EnableWebSocket
@SpringBootApplication
public class WsServerApp {
    public static void main(String[] args) {
        SpringApplication.run(WsServerApp.class, args);
    }
}


package com.websocketServer.controller;

import com.websocketServer.server.WsServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WsServerController {
    @Autowired
    WsServer wsServer;

    /**
     * 服务端发消息给客户端
     * @param message 消息
     */
    @PostMapping("/send2client")
    public void send2Client(@RequestBody String message){
        WsServer.broadcast(message,"txt");
    }
}

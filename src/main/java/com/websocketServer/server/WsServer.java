package com.websocketServer.server;

import com.alibaba.fastjson2.*;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
//@RestController
@ServerEndpoint("/websocket-server/{username}")
//@ServerEndpoint("/")
public class WsServer {
    private static final Logger log = LoggerFactory.getLogger(WsServer.class);
    /**
     *  记录username-session的对应
     */
    public static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    /**
     *  用于记录整体群组情况 群组号-组员list 的MAP
     */
    public static final Map<String, Set<String>> groupMap = new ConcurrentHashMap<>();
    /**
     * 服务端与客户端连接成功时执行
     * @param session 会话
     */

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username){
//        if(username.equals("ADMIN")){
//            log.info("有用户尝试假扮ADMIN");
//            try{
//                session.close();
//            } catch (IOException e){
//                e.printStackTrace();
//            }
//            return;
//        }
        //集合中存入客户端对象+1
        sessionMap.put(username,session);
        int count = sessionMap.size();
        log.info("与客户端连接成功，当前连接的客户端数量为：{}", count);
        String welcome = "欢迎您，" +username;
        sendOneMsg("SERVER",username,welcome,"txt");
        // 广播部分内容
        broadcast("用户"+username+"已上线  "+"当前服务器人数: "+ count,"console");
        broadcast(userList(username),"update_user");
    }

    /**
     * 收到客户端的消息时执行
     * @param message 消息
     * @param session 会话
     */
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("username") String username){
        JSONObject msgObj = new JSONObject();
        try{
            msgObj = JSON.parseObject(message);
        } catch(JSONException e){
            sendOneMsg("SERVER",username,"非json格式的错误消息","error");
            log.info("*收到来自客户端的消息(非json格式)，用户名：{}，消息内容：{}", username, message);
            return;
        }
        String msgType = msgObj.getString("type");
        String content = msgObj.getString("content");
        String target = msgObj.getString("target");
        String groupId = msgObj.getString("group_id");
        if(msgType==null){
            sendOneMsg("SERVER",username,"无类型的错误消息","error");
            log.info("无类型的错误消息");
        } else if(msgType.equals("add_group")){ // 处理进群信息
            addGroup(groupId,username);
        } else if(msgType.equals("side_txt")){
            sendOneMsg(username,target,content,msgType);
        } else if(msgType.equals("group_txt")){
            sendGroupMsg(username,groupId,content,msgType);
        } else if(msgType.equals("exile")&&username.equals("ADMIN")){
            exile(target);
        } else if(msgType.equals("get_group_info")){
            sendGroupInfo(groupId,username);
        } else {
            sendOneMsg("SERVER",username,"未知类型的错误消息","error");
            log.info("未知类型的错误消息");
        }
        log.info("收到来自客户端的消息，用户名：{}，消息内容：{}", username, message);
    }

    /**
     * 连接发生报错时执行
     * @param session 会话
     * @param throwable 报错
     */
    @OnError
    public void onError(Session session, @NonNull Throwable throwable){
        log.error("连接发生报错");
        throwable.printStackTrace();
    }
    /**
     * 连接断开时执行
     */
    @OnClose
    public void onClose(@PathParam("username") String username){
//        if(username.equals("ADMIN")) return;
        //集合中的客户端对象-1
        sessionMap.remove(username);
        broadcast(userList(username),"update_user");
        int count = sessionMap.size();
        log.info("服务端断开连接，当前连接的客户端数量为：{}", count);
    }

    public static void sendOneMsg(String source, String target, String content, String type){
        Session targetSession = sessionMap.get(target);
        if(targetSession==null){
            log.info("用户不存在");
            return;
        }
        JSONObject rtnMsg = new JSONObject();
        rtnMsg.put("source",source);
        rtnMsg.put("content", content);
        rtnMsg.put("type",type);
        try{
            targetSession.getBasicRemote().sendText(rtnMsg.toString());
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void sendGroupMsg(String source, String groupId, String content, String type){
        log.info(groupId);
        Set<String> members = groupMap.getOrDefault(groupId,null);
        if(members==null) return;
        for(String member : members){
            if(member.equals(source)) continue;
            sendOneMsg(source,member,content,type);
        }
    }
    public static void broadcast(String content, String type){
        for(String username : sessionMap.keySet()){
            sendOneMsg("SERVER",username,content,type);
        }
    }
    public static void sendGroupInfo(String groupId, String username) {
        JSONObject rtnMsg = new JSONObject();
        JSONArray members = getGroupMembers(groupId);
        rtnMsg.putArray("members");
        rtnMsg.put("members",members);
        sendOneMsg("SERVER",username,rtnMsg.toString(),"group_info");
    }
    public static void exile(String username){
        Session s = sessionMap.get(username);
        sendOneMsg("SERVER",username,"您已被请出聊天室","exit");
        try{
            s.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void addGroup(String groupId, String username){
        Set<String> members = groupMap.get(groupId);
        if(members==null){
            members = new HashSet<>();
            groupMap.put(groupId,members);
        }
        members.add(username);
        int size = members.size();
        String content = "您已加入群组"+groupId +" 当前群组人数为:" + size;
        sendOneMsg("SERVER",username, content,"txt");
        log.info("用户{}加入群组{}, 该群组人数为{}",username, groupId, size);
    }
    private static JSONArray getGroupMembers(String groupId){
        Set<String> members = groupMap.get(groupId);
        JSONArray ary = new JSONArray();
        if(members==null) return ary;
        ary.addAll(members);
        return ary;
    }
    public static String userList(String cur){
        StringBuilder sb = new StringBuilder();
        for(String user : sessionMap.keySet()){
            if(user.equals(cur)) continue;
            sb.append("^").append(user);
        }
        if(sb.length()==0) return "^";
        sb.deleteCharAt(0);
        return sb.toString();
    }
    public static String groupList(String username){
        StringBuilder sb = new StringBuilder();
        for(String group : groupMap.keySet()){
            sb.append("^").append(group);
        }
        if(sb.length()==0) return "^";
        sb.deleteCharAt(0);
        return sb.toString();
    }
}


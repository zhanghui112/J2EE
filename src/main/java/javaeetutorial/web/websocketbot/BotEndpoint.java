/**
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * You may not modify, use, reproduce, or distribute this software except in
 * compliance with the terms of the License at:
 * http://java.net/projects/javaeetutorial/pages/BerkeleyLicense
 */
package javaeetutorial.web.websocketbot;

import javaeetutorial.web.websocketbot.encoders.UsersMessageEncoder;
import javaeetutorial.web.websocketbot.encoders.ChatMessageEncoder;
import javaeetutorial.web.websocketbot.encoders.InfoMessageEncoder;
import javaeetutorial.web.websocketbot.encoders.JoinMessageEncoder;
import javaeetutorial.web.websocketbot.messages.ChatMessage;
import javaeetutorial.web.websocketbot.messages.UsersMessage;
import javaeetutorial.web.websocketbot.messages.JoinMessage;
import javaeetutorial.web.websocketbot.messages.InfoMessage;
import javaeetutorial.web.websocketbot.decoders.MessageDecoder;
import javaeetutorial.web.websocketbot.messages.Message;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/* Websocket endpoint */
@ServerEndpoint(
        value = "/websocketbot",
        decoders = {MessageDecoder.class},
        encoders = {JoinMessageEncoder.class, ChatMessageEncoder.class,
            InfoMessageEncoder.class, UsersMessageEncoder.class}
)
/* There is a BotEndpoint instance per connetion */
public class BotEndpoint {

    private static final Logger logger = Logger.getLogger("BotEndpoint");
    /* Bot functionality bean */
    @Inject
    private BotBean botbean;
    /* Executor service for asynchronous processing */
    @Resource(name = "comp/DefaultManagedExecutorService")
    private ManagedExecutorService mes;

    @OnOpen
    public void openConnection(Session session) {
        logger.log(Level.INFO, "Connection opened.");
    }

    @OnMessage
    public void message(final Session session, Message msg) {
        logger.log(Level.INFO, "Received: {0}", msg.toString());

        if (msg instanceof JoinMessage) {
            /* Add the new user and notify everybody */
            JoinMessage jmsg = (JoinMessage) msg;
            session.getUserProperties().put("name", jmsg.getName());
            session.getUserProperties().put("active", true);
            logger.log(Level.INFO, "Received: {0}", jmsg.toString());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String hi = sdf.format(Calendar.getInstance().getTime());
            String ip = "";
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                e.printStackTrace();
            }
//            sendAll(session, new InfoMessage(jmsg.getName() + " 登录系统！"));
            
            sendAll(session, new ChatMessage("System", jmsg.getName(),
                    "欢迎您," + jmsg.getName() + "! 登录时间：" + hi + ", IP："+ip+"\n您可以直接输入景点名称进行查询或者输入\"所有景点\"查看全部景点信息，或输入\"帮助\"查看系统帮助信息."));
            sendAll(session, new UsersMessage(this.getUserList(session)));

        } else if (msg instanceof ChatMessage) {
            /* Forward the message to everybody */
            final ChatMessage cmsg = (ChatMessage) msg;
            logger.log(Level.INFO, "Received: {0}", cmsg.toString());
            sendAll(session, cmsg);
            if (cmsg.getTarget().compareTo("System") == 0) {
                /* The bot replies to the message */
                mes.submit(new Runnable() {
                    @Override
                    public void run() {
                        String resp = botbean.respond(cmsg.getMessage());
                        sendAll(session, new ChatMessage("System",
                                cmsg.getName(), resp));
                    }
                });
            }
        }
    }

    @OnClose
    public void closedConnection(Session session) {
        /* Notify everybody */
        session.getUserProperties().put("active", false);
        if (session.getUserProperties().containsKey("name")) {
            String name = session.getUserProperties().get("name").toString();
            sendAll(session, new InfoMessage(name + " has left the chat"));
            sendAll(session, new UsersMessage(this.getUserList(session)));
        }
        logger.log(Level.INFO, "Connection closed.");
    }

    @OnError
    public void error(Session session, Throwable t) {
        logger.log(Level.INFO, "Connection error ({0})", t.toString());
    }

    /* Forward a message to all connected clients
     * The endpoint figures what encoder to use based on the message type */
    public synchronized void sendAll(Session session, Object msg) {
        try {
            for (Session s : session.getOpenSessions()) {
                if (s.isOpen()) {
                    s.getBasicRemote().sendObject(msg);
                    logger.log(Level.INFO, "Sent: {0}", msg.toString());
                }
            }
        } catch (IOException | EncodeException e) {
            logger.log(Level.INFO, e.toString());
        }
    }

    /* Returns the list of users from the properties of all open sessions */
    public List<String> getUserList(Session session) {
        List<String> users = new ArrayList<>();
        users.add("System");
        for (Session s : session.getOpenSessions()) {
            if (s.isOpen() && (boolean) s.getUserProperties().get("active")) {
                users.add(s.getUserProperties().get("name").toString());
            }
        }
        return users;
    }
}

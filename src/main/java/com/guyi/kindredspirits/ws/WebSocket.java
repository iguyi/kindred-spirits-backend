package com.guyi.kindredspirits.ws;

import cn.hutool.json.JSONObject;
import com.guyi.kindredspirits.common.contant.BaseConstant;
import com.guyi.kindredspirits.common.contant.UserConstant;
import com.guyi.kindredspirits.common.enums.ChatTypeEnum;
import com.guyi.kindredspirits.config.HttpSessionConfig;
import com.guyi.kindredspirits.model.domain.Chat;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.domain.UserTeam;
import com.guyi.kindredspirits.model.request.ChatRequest;
import com.guyi.kindredspirits.model.vo.ChatVo;
import com.guyi.kindredspirits.service.ChatService;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.service.UserTeamService;
import com.guyi.kindredspirits.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * WebSocket 服务
 *
 * @author 孤诣
 */
@Component
@Slf4j
@ServerEndpoint(value = "/websocket/{userId}/{teamId}", configurator = HttpSessionConfig.class)
public class WebSocket {

    /**
     * 用来存放每个客户端对应的 WebSocket 对象。
     */
    private static final CopyOnWriteArraySet<WebSocket> WEB_SOCKETS = new CopyOnWriteArraySet<>();

    /**
     * 保存在线用户的会话:
     * - 以用户 id 为 key
     */
    private static final ConcurrentHashMap<String, Session> SESSION_POOL = new ConcurrentHashMap<>();

    /**
     * 保存队伍的连接信息:
     * - 外层 map 以 队伍 id 为 key;
     * - 内层 map 以 用户(队员) id 为 key
     * - 内存 map 的 value 是在线成员对应的 WebSocket 会话
     */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, WebSocket>> TEAM_SESSIONS
            = new ConcurrentHashMap<>();

    /**
     * 但前与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 协议升级为 WebSocket 后, 之前的 Http 状态不会被保存;
     * 因为 WebSocket 是无状态的, 因此需要将升级前的 Http 会话保存
     */
    private HttpSession httpSession;

    /**
     * 创建会话的用户
     */
    private User user;

    /**
     * 队伍会话对应的队伍的 id
     */
    private Long teamId;

    private static final Object SESSION_LOCK = new Object();

    /**
     * 消息发送失败的提示消息
     */
    private static final String SEND_FAIL = "发送失败";

    /**
     * 当 teamId=0 时, 表示发送的是私聊请求
     */
    private static final String ZERO_ID = "0";

    private static UserService userService;

    private static TeamService teamService;

    private static ChatService chatService;

    private static UserTeamService userTeamService;

    /**
     * 连接成功调用的方法
     *
     * @param userId  - 用户 id
     * @param teamId  - 队伍id
     * @param session - 会话
     * @param config  - 配置
     */
    @OnOpen
    public void onOpen(@PathParam(value = "userId") String userId, @PathParam(value = "teamId") String teamId,
                       Session session, EndpointConfig config) {
        if (!validId(userId)) {
            sendError(userId, "user id error");
            return;
        }
        if (!validId(teamId) && !ZERO_ID.equals(teamId)) {
            sendError(userId, "team id error");
            return;
        }

        try {
            HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
            User user = (User) httpSession.getAttribute(UserConstant.USER_LOGIN_STATE);

            if (user != null) {
                // 用户登录消息存在, 设置 Session 和 HttpSession
                this.session = session;
                this.httpSession = httpSession;
            }

            if (!ZERO_ID.equals(teamId)) {
                // 队伍聊天室
                if (!TEAM_SESSIONS.containsKey(teamId)) {
                    // 对应队伍聊天室不存在, 创建并将当前用户加入进去
                    ConcurrentHashMap<String, WebSocket> room = new ConcurrentHashMap<>(0);
                    room.put(userId, this);
                    TEAM_SESSIONS.put(String.valueOf(teamId), room);
                } else {
                    // 对应队伍聊天室存在
                    if (!TEAM_SESSIONS.get(teamId).containsKey(userId)) {
                        // 当前用户不在队伍聊天室中, 需要将其加入
                        TEAM_SESSIONS.get(teamId).put(userId, this);
                    }
                }
                return;
            }

            // 私聊室
            WEB_SOCKETS.add(this);
            SESSION_POOL.put(userId, session);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 连接关闭
     *
     * @param userId  - 用户id
     * @param teamId  - 团队id
     * @param session - 会话
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId, @PathParam("teamId") String teamId, Session session) {
        try {
            if (!ZERO_ID.equals(teamId) && !validId(teamId)) {
                // 关闭队伍聊天室
                TEAM_SESSIONS.get(teamId).remove(userId);
                return;
            }

            if (!SESSION_POOL.isEmpty() && !validId(userId)) {
                // 关闭私聊室
                SESSION_POOL.remove(userId);
                WEB_SOCKETS.remove(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param userId  用户id
     * @param message 消息
     */
    @OnMessage
    public void onMessage(@PathParam("userId") String userId, String message) {
        if (BaseConstant.HEARTBEAT_PING.equals(message)) {
            // 心跳检测
            sendOneMessage(userId, BaseConstant.HEARTBEAT_PONG);
            return;
        }

        // 消息数据反序列化
        ChatRequest chatRequest = JsonUtil.fromJson(message, ChatRequest.class);

        // 获取消息发送者信息
        Long senderId = chatRequest.getSenderId();
        if (!validId(String.valueOf(senderId))) {
            sendError(userId, "user id error");
            return;
        }
        User senderUser = userService.getById(senderId);

        // 获取队伍信息
        Long teamId = chatRequest.getTeamId();
        if (!validId(String.valueOf(teamId)) && !ZERO_ID.equals(teamId.toString())) {
            sendError(userId, "team id error");
            return;
        }
        Team team = teamService.getById(teamId);

        String chatContent = chatRequest.getChatContent();
        Integer chatType = chatRequest.getChatType();
        if (ChatTypeEnum.PRIVATE_CHAT.getType().equals(chatType)) {
            // 私聊
            Long receiverId = chatRequest.getReceiverId();
            if (!validId(String.valueOf(receiverId))) {
                sendError(userId, "user id error");
                return;
            }
            User receiverUser = userService.getById(receiverId);
            privateChat(senderUser, receiverUser, chatContent);
        }
    }

    /**
     * 发送一个消息
     *
     * @param receiverId  消息接收者 id
     * @param chatContent 消息
     */
    public void sendOneMessage(String receiverId, String chatContent) {
        // 获取消息接收者对应的 Session
        Session session = SESSION_POOL.get(receiverId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (SESSION_LOCK) {
                    session.getAsyncRemote().sendText(chatContent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 私聊
     *
     * @param senderUser   - 消息发送者
     * @param receiverUser - 消息接收者
     * @param chatContent  - 消息内容
     */
    private void privateChat(User senderUser, User receiverUser, String chatContent) {
        // 获取 "聊天请求响" 对象
        ChatVo chatVo = chatService.getChatVo(senderUser, receiverUser, chatContent, ChatTypeEnum.PRIVATE_CHAT);

        boolean saveResult = saveChat(chatVo);
        if (!saveResult) {
            // todo 发生消息告知用户
            log.error("聊天结果保存失败");
            return;
        }

        // 发送消息
        String sendResult = JsonUtil.G.toJson(chatVo);
        sendOneMessage(receiverUser.getId().toString(), sendResult);
    }

    /**
     * 将聊天结果保存到数据库中
     *
     * @param chatVo - 聊天请求响应对象
     * @return 保存结果
     */
    private boolean saveChat(ChatVo chatVo) {
        Chat chat = new Chat();
        chat.setSenderId(chatVo.getSenderUser().getId());
        chat.setReceiverId(chatVo.getReceiverUser().getId());
        Long teamId = chatVo.getTeamId();
        chat.setTeamId(teamId);
        chat.setChatContent(chatVo.getChatContent());
        chat.setChatType(chatVo.getChatType());
        List<UserTeam> receiverDataList = userTeamService.getMessageByTeamId(teamId);
        List<Long> receiverIdList = receiverDataList.stream().map(UserTeam::getUserId).collect(Collectors.toList());
        String receiverIds = JsonUtil.G.toJson(receiverIdList);
        chat.setReceiverIds(receiverIds);
        return chatService.save(chat);
    }


    /**
     * 消息发生错误时调用, 起到告知作用
     *
     * @param receiverId - 接收者 id, 即当前用户
     * @param errorTip   - 错误提示
     */
    private void sendError(String receiverId, String errorTip) {
        log.error(errorTip);

        JSONObject obj = new JSONObject();
        obj.set("error", errorTip);
        sendOneMessage(receiverId, obj.toString());
    }

    /**
     * 判断 userId 或 teamId 是否合法
     *
     * @param idStr - userId or teamId
     * @return 合法 - true; 不合法 - false
     */
    private boolean validId(String idStr) {
        // 匹配正整数
        String pattern = "^[1-9][0-9]*$";
        Pattern compiledPattern = Pattern.compile(pattern);
        return compiledPattern.matcher(idStr).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WebSocket webSocket = (WebSocket) o;

        return new EqualsBuilder()
                .append(session, webSocket.session)
                .append(httpSession, webSocket.httpSession)
                .append(user, webSocket.user)
                .append(teamId, webSocket.teamId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(session)
                .append(httpSession)
                .append(user)
                .append(teamId)
                .toHashCode();
    }

    @Resource
    public void setUserService(UserService userService) {
        WebSocket.userService = userService;
    }

    @Resource
    public void setTeamService(TeamService teamService) {
        WebSocket.teamService = teamService;
    }

    @Resource
    public void setChatService(ChatService chatService) {
        WebSocket.chatService = chatService;
    }

    @Resource
    public void setUserTeamService(UserTeamService userTeamService) {
        WebSocket.userTeamService = userTeamService;
    }

}

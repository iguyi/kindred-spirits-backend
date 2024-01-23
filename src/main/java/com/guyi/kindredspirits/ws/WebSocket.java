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
import com.guyi.kindredspirits.model.vo.WebSocketVo;
import com.guyi.kindredspirits.service.ChatService;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.service.UserTeamService;
import com.guyi.kindredspirits.util.JsonUtil;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
@EqualsAndHashCode
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
     * 队伍会话对应的队伍的 id
     */
    private Long teamId;

    private static final Object SESSION_LOCK = new Object();

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
        if (invalidId(userId)) {
            sendError(userId, "user id error");
            return;
        }
        if (invalidId(teamId) && !ZERO_ID.equals(teamId)) {
            sendError(userId, "team id error");
            return;
        }

        this.teamId = Long.valueOf(teamId);

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
                    TEAM_SESSIONS.put(teamId, room);
                } else {
                    // 对应队伍聊天室存在
                    if (!TEAM_SESSIONS.get(teamId).containsKey(userId)) {
                        // 当前用户不在队伍聊天室中, 需要将其加入
                        TEAM_SESSIONS.get(teamId).put(userId, this);
                    }
                }
            } else {
                // 私聊室
                WEB_SOCKETS.add(this);
                SESSION_POOL.put(userId, session);
            }
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
            if (!ZERO_ID.equals(teamId) && !invalidId(teamId)) {
                // 关闭队伍聊天室
                TEAM_SESSIONS.get(teamId).remove(userId);
                return;
            }

            if (!SESSION_POOL.isEmpty() && !invalidId(userId)) {
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
     * @param userId  消息发送者 id
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
        if (invalidId(String.valueOf(senderId))) {
            sendError(userId, "User id error");
            return;
        }
        User senderUser = userService.getById(senderId);
        if (senderUser == null) {
            sendError(userId, "The user does not exist. ");
            return;
        }

        // 获取消息内容、消息类型
        String chatContent = chatRequest.getChatContent();
        Integer chatType = chatRequest.getChatType();

        if (ChatTypeEnum.PRIVATE_CHAT.getType().equals(chatType) && ZERO_ID.equals(teamId.toString())) {
            // 私聊
            Long receiverId = chatRequest.getReceiverId();
            if (invalidId(String.valueOf(receiverId))) {
                sendError(userId, "User id error");
                return;
            }

            // 获取消息接收者消息
            User receiverUser = userService.getById(receiverId);
            if (receiverUser == null) {
                sendError(userId, "The target does not exist. ");
            }

            // 发送私聊消息
            privateChat(senderUser, receiverUser, chatContent);
            return;
        }

        if (ChatTypeEnum.GROUP_CHAT.getType().equals(chatType) && !ZERO_ID.equals(teamId.toString())) {
            // 队伍聊天
            Long teamId = chatRequest.getTeamId();
            if (invalidId(String.valueOf(teamId))) {
                sendError(userId, "Team id error");
                return;
            }

            // 获取队伍信息
            Team team = teamService.getById(teamId);
            if (team == null) {
                sendError(userId, "The team does not exist.");
                return;
            }

            // 发送队伍聊天消息
            teamChat(senderUser, team, chatContent);
            return;
        }

        sendError(userId, "参数错误");
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
        // 获取 "聊天请求响应" 对象
        ChatVo chatVo = chatService.getChatVo(senderUser, receiverUser, chatContent, ChatTypeEnum.PRIVATE_CHAT);

        // 保存聊天记录
        boolean saveResult = saveChat(chatVo);
        if (!saveResult) {
            sendError(senderUser.getId().toString(), "发送失败");
            log.error("聊天结果保存失败");
            return;
        }

        // 发送消息
        String finalSend = JsonUtil.G.toJson(chatVo);
        sendOneMessage(receiverUser.getId().toString(), finalSend);
    }

    /**
     * 队伍聊天
     *
     * @param senderUser  - 发送者 id
     * @param team        - 接收消息的队伍
     * @param chatContent - 消息内容
     */
    private void teamChat(User senderUser, Team team, String chatContent) {
        // 获取消息发送者签名
        WebSocketVo senderUserLogo = new WebSocketVo();
        BeanUtils.copyProperties(senderUser, senderUserLogo);

        // 获取消息发送者 id
        Long senderUserId = senderUser.getId();

        // 保存聊天记录
        boolean saveResult = saveChat(senderUserId
                , null
                , team.getId()
                , chatContent
                , ChatTypeEnum.GROUP_CHAT.getType());

        if (!saveResult) {
            // 聊天记录保存失败
            sendError(senderUserId.toString(), "发送失败");
            log.error("聊天结果保存失败");
            return;
        }

        ChatVo chatVo = chatService.getChatVo(senderUser, null, chatContent, ChatTypeEnum.GROUP_CHAT);
        String sendMessage = JsonUtil.G.toJson(chatVo);

        // 获取队伍成员, 并逐发消息
        ConcurrentHashMap<String, WebSocket> teamPlayerWebSocketMap = TEAM_SESSIONS.get(teamId.toString());
        teamPlayerWebSocketMap.forEach((key, value) -> {
            try {
                if (!key.equals(senderUserId.toString())) {
                    value.session.getAsyncRemote().sendText(sendMessage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 将私聊的聊天记录保存到数据库中
     *
     * @param chatVo - 聊天请求响应对象
     * @return 保存结果
     */
    private boolean saveChat(ChatVo chatVo) {
        return saveChat(chatVo.getSenderUser().getId()
                , chatVo.getReceiverUser().getId()
                , null
                , chatVo.getChatContent()
                , chatVo.getChatType());
    }

    /**
     * 将聊天记录保存到数据库中, 通用
     *
     * @param senderId   - 消息发送者 id
     * @param receiverId - 消息接收者 id
     * @param teamId     - 接收消息的队伍 id
     * @param chatType   - 消息类型
     * @return 保存结果
     */
    private boolean saveChat(Long senderId, Long receiverId, Long teamId, String chantContent, Integer chatType) {
        Chat chat = new Chat();
        chat.setSenderId(senderId);
        chat.setReceiverId(receiverId);
        chat.setTeamId(teamId);
        chat.setChatContent(chantContent);
        chat.setChatType(chatType);
        if (teamId != null) {
            List<UserTeam> receiverDataList = userTeamService.getMessageByTeamId(teamId);
            List<Long> receiverIdList = receiverDataList.stream().map(UserTeam::getUserId).collect(Collectors.toList());
            String receiverIds = JsonUtil.G.toJson(receiverIdList);
            chat.setReceiverIds(receiverIds);
        }
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
     * 判断 userId 或 teamId 是否是无效 id
     *
     * @param idStr - userId or teamId
     * @return userId 或 teamId 无效 - true; userId 或 teamId 有效 - false
     */
    private boolean invalidId(String idStr) {
        // 匹配正整数
        String pattern = "^[1-9][0-9]*$";
        Pattern compiledPattern = Pattern.compile(pattern);
        return !compiledPattern.matcher(idStr).matches();
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

package com.guyi.kindredspirits.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.reflect.TypeToken;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.enums.ChatTypeEnum;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.ChatMapper;
import com.guyi.kindredspirits.model.cache.UnreadMessageNumCache;
import com.guyi.kindredspirits.model.domain.Chat;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.ChatHistoryRequest;
import com.guyi.kindredspirits.model.vo.ChatRoomVo;
import com.guyi.kindredspirits.model.vo.ChatVo;
import com.guyi.kindredspirits.model.vo.WebSocketVo;
import com.guyi.kindredspirits.service.ChatService;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UnreadMessageNumService;
import com.guyi.kindredspirits.service.UserService;
import com.guyi.kindredspirits.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 针对表 chat(聊天记录表) 的数据库操作 Service 实现
 *
 * @author 孤诣
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements ChatService {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UnreadMessageNumService unreadMessageNumService;

    /**
     * id 列表对应的 Type 对象, 用于将 JSON 格式的 id 列表转为泛型为 Long 的 List
     */
    private static final Type ID_LIST_TYPE = new TypeToken<List<Long>>() {
    }.getType();

    @Override
    public ChatVo getChatVo(User senderUser, User receiverUser, String chatContent, ChatTypeEnum chatTypeEnum) {
        // 消息发送者签名
        WebSocketVo senderUserLogo = new WebSocketVo();
        BeanUtils.copyProperties(senderUser, senderUserLogo);

        // 消息接收者签名
        WebSocketVo receiverUserLogo = null;
        if (receiverUser != null) {
            receiverUserLogo = new WebSocketVo();
            BeanUtils.copyProperties(receiverUser, receiverUserLogo);
        }

        // 整合数据
        ChatVo chatVo = new ChatVo();
        chatVo.setSenderUser(senderUserLogo);
        chatVo.setReceiverUser(receiverUserLogo);
        chatVo.setChatContent(chatContent);
        chatVo.setChatType(chatTypeEnum.getType());
        DateTime sendTime = DateUtil.date(System.currentTimeMillis());
        chatVo.setSendTime(DateUtil.format(sendTime, "yyyy-MM-dd HH:mm:ss"));
        chatVo.setErrorFlag(false);

        return chatVo;
    }

    @Override
    public List<ChatVo> getPrivateChat(User loginUser, ChatHistoryRequest chatHistoryRequest) {
        // 完成参数校验并获取当前登录用户 id、好友 id
        Long loginUserId = parameterValidation(loginUser, chatHistoryRequest);
        Long friendId = chatHistoryRequest.getFriendId();
        if (friendId == null || friendId < 1 || Objects.equals(loginUserId, friendId)) {
            return Collections.emptyList();
        }

        // 查询对应的聊天记录
        QueryWrapper<Chat> chatQueryWrapper = new QueryWrapper<>();
        chatQueryWrapper.and(privateChatQuery -> privateChatQuery
                .eq("senderId", loginUserId).eq("receiverId", friendId)
                .or()
                .eq("senderId", friendId).eq("receiverId", loginUserId)
        ).eq("chatType", ChatTypeEnum.PRIVATE_CHAT.getType());
        List<Chat> chatList = this.list(chatQueryWrapper);

        // 查询好友数据
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id", "userAccount", "username", "avatarUrl").eq("id", friendId);
        User friend = userService.getOne(userQueryWrapper);

        // 先根据 id 进行排序, 避免因为使用二级索引带来的聊天记录顺序问题; 再对数据进行整合
        return chatList.stream()
                .sorted((chat1, chat2) -> Math.toIntExact(chat1.getId() - chat2.getId()))
                .map(chat -> {
                    // 消息发送者是当前用户
                    if (Objects.equals(chat.getSenderId(), loginUserId)) {
                        return getChatVo(loginUser, friend, chat.getChatContent(), ChatTypeEnum.PRIVATE_CHAT);
                    }

                    // 消息接收者是当前用户
                    return getChatVo(friend, loginUser, chat.getChatContent(), ChatTypeEnum.PRIVATE_CHAT);
                }).collect(Collectors.toList());
    }

    @Override
    public List<ChatVo> getTeamChat(User loginUser, ChatHistoryRequest chatHistoryRequest) {
        // 校验
        parameterValidation(loginUser, chatHistoryRequest);

        // 查询对应聊天记录
        Long teamId = chatHistoryRequest.getTeamId();
        QueryWrapper<Chat> chatQueryWrapper = new QueryWrapper<>();
        chatQueryWrapper.eq("teamId", teamId).eq("chatType", ChatTypeEnum.GROUP_CHAT.getType());
        List<Chat> chatList = this.list(chatQueryWrapper);
        if (CollectionUtils.isEmpty(chatList)) {
            return Collections.emptyList();
        }

        // 查询在队伍内有发过消息的用户
        Set<Long> senderIdSet = chatList.stream().map(Chat::getSenderId).collect(Collectors.toSet());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id", "userAccount", "username", "avatarUrl").in("id", senderIdSet);
        List<User> sendUserList = userService.list(userQueryWrapper);
        Map<Long, List<User>> sendUserSearchMapById = sendUserList.stream().collect(Collectors.groupingBy(user -> {
            User safetyUser = userService.getSafetyUser(user);
            return safetyUser.getId();
        }));

        return chatList.stream().filter(chat -> {
            // 过滤不该当前用户接收的的消息
            String jsonReceiverIds = chat.getReceiverIds();
            List<Long> listReceiverIds = JsonUtil.fromJson(jsonReceiverIds, ID_LIST_TYPE);
            return listReceiverIds.contains(loginUser.getId());
        }).map(chat -> {
            ChatVo chatVo = getChatVo(sendUserSearchMapById.get(chat.getSenderId()).get(0),
                    null,
                    chat.getChatContent(),
                    ChatTypeEnum.GROUP_CHAT);
            chatVo.setSendTime(DateUtil.format(chat.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
            return chatVo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ChatRoomVo> getChatRoomList(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        // 获取用户 id
        Long loginUserId = loginUser.getId();
        List<ChatRoomVo> privateChatRoomVoList = listPrivateChatRoomVos(loginUserId);
        List<ChatRoomVo> teamChatRoomVoList = listTeamChatRoomVos(loginUserId);

        // 整合返回结果
        List<ChatRoomVo> chatRoomVoList = new ArrayList<>();
        chatRoomVoList.addAll(privateChatRoomVoList);
        chatRoomVoList.addAll(teamChatRoomVoList);
        sortChatRoomList(chatRoomVoList);
        String nowDate = DateUtil.format(new Date(), "yy-MM-dd");
        chatRoomVoList.forEach(chatRoomVo -> chatRoomVo.setSendTime(timeFormat(chatRoomVo.getSendTime(), nowDate)));
        return chatRoomVoList;
    }

    /**
     * 获取指定用户的历史私聊会话列表
     *
     * @param userId - 用户 id
     * @return 私聊会话封装对象列表
     */
    private List<ChatRoomVo> listPrivateChatRoomVos(Long userId) {
        // 查询与我相关的私聊消息
        QueryWrapper<Chat> chatQueryWrapper = new QueryWrapper<>();
        chatQueryWrapper.isNull("teamId")
                .and(queryWrapper -> queryWrapper.eq("senderId", userId).or().eq("receiverId", userId));
        List<Chat> chatList = this.list(chatQueryWrapper);

        // 没有私聊的消息
        if (CollectionUtils.isEmpty(chatList)) {
            return Collections.emptyList();
        }

        // 按照好友 id 对私聊消息分组
        Map<Long, List<Chat>> chatGroup = chatList.stream().collect(Collectors.groupingBy(chat -> {
            Long senderId = chat.getSenderId();
            return Objects.equals(senderId, userId) ? chat.getReceiverId() : senderId;
        }));

        // 查询好友信息
        Set<Long> friendIdSet = chatGroup.keySet();
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id", "username", "avatarUrl").in("id", friendIdSet);
        List<User> friendList =
                CollectionUtils.isEmpty(friendIdSet) ? Collections.emptyList() : userService.list(userQueryWrapper);

        // 整合返回结果(历史私聊会话)
        List<ChatRoomVo> chatRoomVoList = new ArrayList<>();
        chatGroup.forEach((key, value) -> {
            // 整合单个私聊会话数据
            ChatRoomVo chatRoomVo = new ChatRoomVo();
            chatRoomVo.setReceiverId(key);
            for (User friend : friendList) {
                // 找到对应用户并设置私聊数据
                if (friend.getId().equals(key)) {
                    chatRoomVo.setAvatarUrl(friend.getAvatarUrl());
                    chatRoomVo.setReceiverName(friend.getUsername());
                    break;
                }
            }
            chatRoomVo.setChatType(ChatTypeEnum.PRIVATE_CHAT.getType());
            // 获取最后一条聊天记录
            Chat lastChat = value.get(value.size() - 1);
            chatRoomVo.setLastRecord(lastChat.getChatContent());
            // 获取消息发送的日期、时间信息
            String sendDateAndTime = DateUtil.format(lastChat.getCreateTime(), "yy-MM-dd HH:mm:ss");
            chatRoomVo.setSendTime(sendDateAndTime);
            // 获取未读消息数
            String sessionName = String.format("private-%s-%s", userId, key);
            UnreadMessageNumCache unreadMessageNum = unreadMessageNumService.getUnreadMessageNumByName(sessionName);

            // 重设未读消息数
            int unreadNum = unreadMessageNum == null ? 0 : unreadMessageNum.getUnreadNum();
            chatRoomVo.setUnreadMessageNum(unreadNum);

            chatRoomVoList.add(chatRoomVo);
        });

        return chatRoomVoList;
    }

    /**
     * 获取指定用户的队伍会话列表
     *
     * @param userId - 用户 id
     * @return 队伍会话封装对象列表
     */
    private List<ChatRoomVo> listTeamChatRoomVos(Long userId) {
        // 查询与我有关的队伍聊天记录
        QueryWrapper<Chat> chatQueryWrapper = new QueryWrapper<>();
        chatQueryWrapper.select("id", "senderId", "teamId", "receiverIds", "chatContent", "createTime")
                .like("receiverIds", userId);
        List<Chat> chatList = this.list(chatQueryWrapper);

        // 没有队伍的消息
        if (CollectionUtils.isEmpty(chatList)) {
            return Collections.emptyList();
        }

        // 按照队伍 id 对队伍消息分组
        Map<Long, List<Chat>> chatGroup = chatList.stream().filter(chat -> {
            // 前面使用模糊查询, 因此这里需要过滤调其他队伍的消息
            String jsonReceiverIds = chat.getReceiverIds();
            List<Long> listReceiverIds = JsonUtil.fromJson(jsonReceiverIds, ID_LIST_TYPE);
            return listReceiverIds.contains(userId);
        }).collect(Collectors.groupingBy(Chat::getTeamId));
        chatGroup.remove(null);

        // 数据筛选
        Map<Long, List<Chat>> validChatGroup = new HashMap<>(chatGroup.size());
        chatGroup.forEach((key, value) -> {
            List<Chat> chats = value.stream().filter(chat -> {
                // 过滤不该当前用户接收的的消息
                String jsonReceiverIds = chat.getReceiverIds();
                List<Long> listReceiverIds = JsonUtil.fromJson(jsonReceiverIds, ID_LIST_TYPE);
                return listReceiverIds.contains(userId);
            }).collect(Collectors.toList());

            // 过滤空数据
            if (CollectionUtils.isNotEmpty(chats)) {
                validChatGroup.put(key, chats);
            }
        });

        // 查询相关队伍信息
        Set<Long> teamIdSet = validChatGroup.keySet();
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.select("id", "name", "avatarUrl").in("id", teamIdSet);
        List<Team> teamList =
                CollectionUtils.isEmpty(teamIdSet) ? Collections.emptyList() : teamService.list(teamQueryWrapper);

        // 整合返回结果(历史队伍会话)
        List<ChatRoomVo> chatRoomVoList = new ArrayList<>();
        chatGroup.forEach((key, value) -> {
            // 整合单个队伍会话数据
            ChatRoomVo chatRoomVo = new ChatRoomVo();
            chatRoomVo.setReceiverId(key);
            for (Team team : teamList) {
                // 找到对应队伍并设置聊天数据
                if (Objects.equals(team.getId(), key)) {
                    chatRoomVo.setAvatarUrl(team.getAvatarUrl());
                    chatRoomVo.setReceiverName(team.getName());
                    break;
                }
            }
            chatRoomVo.setChatType(ChatTypeEnum.GROUP_CHAT.getType());
            // 获取最后一条聊天记录
            Chat lastChat = value.get(value.size() - 1);
            chatRoomVo.setLastRecord(lastChat.getChatContent());
            // 获取消息发送的日期、时间信息
            String sendDateAndTime = DateUtil.format(lastChat.getCreateTime(), "yy-MM-dd HH:mm:ss");
            chatRoomVo.setSendTime(sendDateAndTime);
            // 获取未读消息数
            String sessionName = String.format("team-%s-%s", userId, key);
            UnreadMessageNumCache unreadMessageNum = unreadMessageNumService.getUnreadMessageNumByName(sessionName);

            // 重设未读消息数
            int unreadNum = unreadMessageNum == null ? 0 : unreadMessageNum.getUnreadNum();
            chatRoomVo.setUnreadMessageNum(unreadNum);

            chatRoomVoList.add(chatRoomVo);
        });

        return chatRoomVoList;
    }

    /**
     * 完成参数校验并获取登录用户 id
     *
     * @param loginUser          - 当前登录用户
     * @param chatHistoryRequest - 获取聊天记录请求
     */
    private Long parameterValidation(User loginUser, ChatHistoryRequest chatHistoryRequest) {
        // 登录用户消息校验
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        if (chatHistoryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        Long friendId = chatHistoryRequest.getFriendId();
        if (friendId != null && friendId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        Long teamId = chatHistoryRequest.getTeamId();
        if (teamId != null && teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }

        return loginUser.getId();
    }

    /**
     * 按照时间对历史聊天会话列表进行排序, 会话中的最后一条聊天记录的发送时间里当前时间越近, 排序位置越靠前.
     *
     * @param source 历史聊天会话列表
     */
    private void sortChatRoomList(List<ChatRoomVo> source) {
        source.sort((c1, c2) -> {
            String sendTime1 = c1.getSendTime();
            String sendTime2 = c2.getSendTime();
            return sendTime2.compareTo(sendTime1);
        });
    }

    /**
     * 根据现在的日期, 对时间进行格式化, 假设现在的日期为 2024-01-20:<br/>
     * - "2024-01-20 12:09:01" => "12:09:01"<br/>
     * - "2024-01-19 12:09:01" => "2024-01-19"<br/>
     *
     * @param time - yy-MM-dd HH:mm:ss
     * @param date - yy-MM-dd
     * @return HH:mm:ss 或者 yy-MM-dd 格式的日期字符串
     */
    private String timeFormat(String time, String date) {
        if (time.contains(date)) {
            return time.substring(9);
        }
        return time.substring(0, 8);
    }

}

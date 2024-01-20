package com.guyi.kindredspirits.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.enums.ChatTypeEnum;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.ChatMapper;
import com.guyi.kindredspirits.model.domain.Chat;
import com.guyi.kindredspirits.model.domain.Team;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.request.ChatHistoryRequest;
import com.guyi.kindredspirits.model.request.TeamMyQueryRequest;
import com.guyi.kindredspirits.model.vo.ChatRoomVo;
import com.guyi.kindredspirits.model.vo.ChatVo;
import com.guyi.kindredspirits.model.vo.WebSocketVo;
import com.guyi.kindredspirits.service.ChatService;
import com.guyi.kindredspirits.service.TeamService;
import com.guyi.kindredspirits.service.UserService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 针对表 chat(聊天记录表) 的数据库操作 Service 实现
 *
 * @author 孤诣
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements ChatService {

    @Resource
    private HttpServletRequest httpServletRequest;

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Override
    public ChatVo getChatVo(User senderUser, User receiverUser, String chatContent, ChatTypeEnum chatTypeEnum) {
        WebSocketVo senderUserLogo = new WebSocketVo();
        BeanUtils.copyProperties(senderUser, senderUserLogo);

        WebSocketVo receiverUserLogo = new WebSocketVo();
        if (receiverUser != null) {
            BeanUtils.copyProperties(receiverUser, receiverUserLogo);
        }

        ChatVo chatVo = new ChatVo();
        chatVo.setSenderUser(senderUserLogo);
        chatVo.setReceiverUser(receiverUserLogo);
        chatVo.setChatContent(chatContent);
        chatVo.setChatType(chatTypeEnum.getType());
        DateTime sendTime = DateUtil.date(System.currentTimeMillis());
        chatVo.setSendTime(DateUtil.format(sendTime, "yyyy-MM-dd HH:mm:ss"));

        return chatVo;
    }

    @Override
    public List<ChatVo> getPrivateChat(ChatHistoryRequest chatHistoryRequest) {
        // 完成参数校验并获取当前登录用户 id、好友 id
        Long loginUserId = parameterValidation(chatHistoryRequest);
        Long friendId = chatHistoryRequest.getFriendId();

        // 查询对应的聊天记
        QueryWrapper<Chat> chatQueryWrapper = new QueryWrapper<>();
        chatQueryWrapper.and(privateChatQuery -> privateChatQuery
                .eq("senderId", loginUserId)
                .eq("receiverId", friendId)
                .or()
                .eq("senderId", friendId)
                .eq("receiverId", loginUserId)
        ).eq("chatType", ChatTypeEnum.PRIVATE_CHAT.getType());
        List<Chat> chatList = this.list(chatQueryWrapper);

        List<ChatVo> chatVoList = chatList.stream().map(chat -> {
            User senderUser = userService.getById(chat.getSenderId());
            User receiverUser = userService.getById(chat.getReceiverId());
            return getChatVo(senderUser, receiverUser, chat.getChatContent(), ChatTypeEnum.PRIVATE_CHAT);
        }).collect(Collectors.toList());

        // todo 建立缓存
        log.debug("等待建立缓存");

        return chatVoList;
    }

    @Override
    public List<ChatVo> getTeamChat(ChatHistoryRequest chatHistoryRequest) {
        // 校验
        parameterValidation(chatHistoryRequest);

        // 获取队伍 id
        Long teamId = chatHistoryRequest.getTeamId();

        // 查询对应聊天记录
        QueryWrapper<Chat> chatQueryWrapper = new QueryWrapper<>();
        chatQueryWrapper.eq("teamId", teamId).eq("chatType", ChatTypeEnum.GROUP_CHAT.getType());
        List<Chat> chatList = this.list(chatQueryWrapper);
        List<ChatVo> chatVoList = chatList.stream().map(chat -> {
            User senderUser = userService.getById(chat.getSenderId());
            User receiverUser = userService.getById(chat.getReceiverId());
            ChatVo chatVo = getChatVo(senderUser, receiverUser, chat.getChatContent(), ChatTypeEnum.GROUP_CHAT);
            chatVo.setSendTime(DateUtil.format(chat.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
            return chatVo;
        }).collect(Collectors.toList());

        // todo 建立缓存
        log.debug("等待建立缓存");

        return chatVoList;
    }

    @Override
    public List<ChatRoomVo> getChatRoomList() {
        // 权限校验
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        // 获取用户 id
        Long loginUserId = loginUser.getId();

        // 查询我所在队伍的信息
        TeamMyQueryRequest teamMyQueryRequest = new TeamMyQueryRequest();
        teamMyQueryRequest.setId(loginUserId);
        List<Team> teamList = teamService.listMyJoinTeams(teamMyQueryRequest, loginUser);

        // 获取和当前登录用户相关的所有聊天记录
        QueryWrapper<Chat> chatQueryWrapper = new QueryWrapper<>();
        chatQueryWrapper.eq("senderId", loginUserId).or().eq("receiverId", loginUserId);
        // 获取我所在队伍的 id 列表
        List<Long> teamIdList = teamList.stream().map(Team::getId).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(teamIdList)) {
            chatQueryWrapper.or().in("teamId", teamIdList);
        }
        List<Chat> chatList = this.list(chatQueryWrapper);
        // 将聊天记录按照好友 id 或者队伍 id 进行分组
        Map<Pair<Long, Integer>, List<Chat>> chatGroup = chatList.stream().collect(Collectors.groupingBy(chat -> {
            if (chat.getTeamId() != null) {
                return Pair.of(chat.getTeamId(), ChatTypeEnum.GROUP_CHAT.getType());
            }

            Long senderId = chat.getSenderId();
            if (loginUserId.equals(senderId)) {
                return Pair.of(chat.getReceiverId(), ChatTypeEnum.PRIVATE_CHAT.getType());
            }
            return Pair.of(chat.getSenderId(), ChatTypeEnum.PRIVATE_CHAT.getType());
        }));

        // 有聊天记录的好友的 id
        List<Long> hasChatFriendIdList = new ArrayList<>();
        chatGroup.forEach((key, value) -> {
            if (ChatTypeEnum.PRIVATE_CHAT.getType().equals(key.getSecond())) {
                hasChatFriendIdList.add(key.getFirst());
            }
        });

        // 查询有聊天记录的好友的信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        if (!CollectionUtils.isEmpty(hasChatFriendIdList)) {
            userQueryWrapper.select("id", "username", "avatarUrl");
            userQueryWrapper.in("id", hasChatFriendIdList);
        } else {
            userQueryWrapper.eq("id", 0);
        }
        final List<User> hasChatFriendList = userService.list(userQueryWrapper);

        // 整合返回结果
        List<ChatRoomVo> chatRoomVoList = new ArrayList<>();
        chatGroup.forEach((key, value) -> {
            ChatRoomVo chatRoomVo = new ChatRoomVo();

            Long senderObjectId = key.getFirst();
            chatRoomVo.setReceiverId(senderObjectId);

            // 判断聊天记录类型是否为队伍聊天
            boolean isTeamChat = (ChatTypeEnum.GROUP_CHAT.getType().equals(key.getSecond()));
            if (isTeamChat) {
                teamList.forEach(team -> {
                    if (team.getId().equals(senderObjectId)) {
                        chatRoomVo.setAvatarUrl(team.getAvatarUrl());
                        chatRoomVo.setReceiverName(team.getName());
                    }
                });
                chatRoomVo.setChatType(ChatTypeEnum.GROUP_CHAT.getType());
            } else {
                // 私聊
                for (User friend : hasChatFriendList) {
                    if (friend.getId().equals(senderObjectId)) {
                        chatRoomVo.setAvatarUrl(friend.getAvatarUrl());
                        chatRoomVo.setReceiverName(friend.getUsername());
                        break;
                    }
                }
                chatRoomVo.setChatType(ChatTypeEnum.PRIVATE_CHAT.getType());
            }

            // 获取最后一条聊天记录
            Chat lastChat = value.get(value.size() - 1);
            chatRoomVo.setLastRecord(lastChat.getChatContent());
            // 获取消息发送日期和时间
            String sendDate = DateUtil.format(lastChat.getCreateTime(), "yy-MM-dd");
            String sendTime = DateUtil.format(lastChat.getCreateTime(), "HH:mm:ss");
            // 获取当前日期
            String nowDate = DateUtil.format(new Date(), "yy-MM-dd");
            chatRoomVo.setSendTime((nowDate.equals(sendDate)) ? sendTime : sendDate);
            chatRoomVoList.add(chatRoomVo);
        });
        return chatRoomVoList;
    }

    /**
     * 完成参数校验并获取登录用户 id
     *
     * @param chatHistoryRequest - 获取聊天记录请求
     */
    private Long parameterValidation(ChatHistoryRequest chatHistoryRequest) {
        // 登录用户消息校验
        User loginUser = userService.getLoginUser(httpServletRequest);
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

}

package com.guyi.kindredspirits.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guyi.kindredspirits.KindredSpiritsApplication;
import com.guyi.kindredspirits.common.ErrorCode;
import com.guyi.kindredspirits.common.ProjectProperties;
import com.guyi.kindredspirits.common.contant.RedisConstant;
import com.guyi.kindredspirits.common.contant.UserConstant;
import com.guyi.kindredspirits.exception.BusinessException;
import com.guyi.kindredspirits.mapper.UserMapper;
import com.guyi.kindredspirits.model.domain.User;
import com.guyi.kindredspirits.model.vo.TagVo;
import com.guyi.kindredspirits.util.JsonUtil;
import com.guyi.kindredspirits.util.TagPair;
import com.guyi.kindredspirits.util.redis.RedisQueryReturn;
import com.guyi.kindredspirits.util.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;

import static com.guyi.kindredspirits.common.contant.BaseConstant.RETRIES_MAX_NUMBER;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = KindredSpiritsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class InsetUserTest {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Resource
    private TagService tagService;

    @Resource
    private ProjectProperties projectProperties;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 标签数据
     */
    private Map<TagVo, List<TagVo>> tagData;

    /**
     * 自定义线程池
     * 线程存活时间: 1 分钟
     * 任务处理策略: 默认策略, 拒绝溢出的任务
     */
    private final ExecutorService executorService = new ThreadPoolExecutor(24,
            1000,
            10000,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(10000));

    /**
     * 多线程异步、批量插入
     */
    @Test
    public void doConcurrencyInsertUsers() {
        // 计时工具
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 插入 100000 条数据, 分批 10, 每批 10000 条数据
        final int INSERT_NUMBER = 50000;
        int batchSize = 5000;
        List<CompletableFuture<Void>> futureList = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < INSERT_NUMBER / batchSize; i++) {
            // 数据准备
            List<User> userList = new ArrayList<>();
            for (int j = 0; j < batchSize; j++) {
                userList.add(createUserData());
            }

            // 执行异步操作
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    // () -> userService.saveBatch(userList, userList.size()),
                    () -> {
                        userMapper.batchInsert(userList);
                        System.out.println("*****完成一批数据的存储*****");
                    },
                    executorService
            );
            futureList.add(future);
        }

        // 确保所有异步任务执行完成才会统计时长
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        System.out.println("耗时: " + totalTimeMillis);
    }

    /**
     * 生成用户数据
     *
     * @return 用户
     */
    private User createUserData() {
        User user = new User();

        // 随机用户昵称
        String username = UserConstant.DEFAULT_USERNAME_PRE + RandomUtil.randomString(6);
        user.setUsername(username);

        // 默认头像
        String defaultUserAvatarPath = projectProperties.getDefaultUserAvatarPath();
        user.setAvatarUrl(defaultUserAvatarPath);

        // 性别: 男女各一半
        int gender = RandomUtil.randomInt(1, 51) % 2;
        user.setGender(gender);

        // 热点用户: 热点用户数占 1 / 3
        int isHotUser = RandomUtil.randomInt(1, 103) % 3 == 0 ? 1 : 0;
        user.setIsHot(isHotUser);

        // 密码: 12345678
        user.setUserPassword("570cf6b8c84dfa32047d348046798560");

        // 随机设置标签
        String tagDataRandom = getTagDataRandom();
        user.setTags(tagDataRandom);

        // 设置身份
        user.setUserRole(0);

        // 生成账号
        String lockKey = String.format(RedisConstant.KEY_PRE, "user", "register", "lock");
        RLock lock = redissonClient.getLock(lockKey);
        for (int i = 0; i < RETRIES_MAX_NUMBER; i++) {
            try {
                if (lock.tryLock(0, 30L, TimeUnit.SECONDS)) {
                    // 从缓存中取账号信息最新用户的 userAccount
                    RedisQueryReturn<String> redisQueryReturn =
                            RedisUtil.getValue(RedisConstant.MAX_ID_USER_ACCOUNT_KEY, String.class);
                    String maxUserAccount;
                    if (redisQueryReturn == null || redisQueryReturn.isExpiration()) {
                        // 查询缓存出现异常或者缓存过期
                        maxUserAccount = getMaxUserAccount();
                    } else {
                        // 缓存未过期
                        maxUserAccount = redisQueryReturn.getData();
                        if (maxUserAccount == null || maxUserAccount.compareTo("100001") < 0) {
                            // 缓存中的数据有误
                            maxUserAccount = getMaxUserAccount();
                        }
                    }

                    //  将取到的 userAccount 加 1 作为新用户的 userAccount
                    long newUserAccount = Long.parseLong(maxUserAccount) + 1;
                    String newUserAccountValue = String.valueOf(newUserAccount);
                    user.setUserAccount(newUserAccountValue);

                    //  新用户数据插入数据库
                    userService.save(user);
                    if (user.getId() == null) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
                    }

                    //  将新用户的 userAccount 写入缓存： kindred-spirits:user:max:id
                    RedisUtil.setValue(RedisConstant.MAX_ID_USER_ACCOUNT_KEY, newUserAccountValue,
                            10L, TimeUnit.MINUTES);
                    user = userService.getById(user.getId());
                    user = userService.getSafetyUser(user);
                }
            } catch (Exception e) {
                log.debug("用户注册异常, 异常信息如下: \n" + e);
            } finally {
                // 只释放当前线程加的锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        return user;
    }

    /**
     * 随机获取标签数据
     *
     * @return 标签数据
     */
    private String getTagDataRandom() {
        if (tagData == null) {
            List<List<TagVo>> tagGroup = tagService.getTagGroup();
            Map<TagVo, List<TagVo>> tagMap = new HashMap<>();
            tagGroup.forEach(tagVos -> {
                List<TagVo> childTagList = new ArrayList<>();
                tagVos.forEach(tagVo -> {
                    if (tagVo.getIsParent() == 0) {
                        childTagList.add(tagVo);
                    } else {
                        tagMap.put(tagVo, childTagList);
                    }
                });
            });
            tagData = tagMap;
        }
        // System.out.println(tagData);

        Map<String, List<TagPair>> resultMap = new HashMap<>();
        tagData.forEach((key, value) -> {
            resultMap.put(key.getId().toString(), new ArrayList<>());
            int tagNum = RandomUtil.randomInt(4);
            for (int i = 1; i <= tagNum; i++) {
                int size = value.size();
                int randomInt = RandomUtil.randomInt(size);
                TagVo tagVo = value.get(randomInt);
                if (tagVo.getIsParent() == 1) {
                    continue;
                }
                TagPair tagPair = new TagPair(tagVo.getTagName(), tagVo.getWeights());
                resultMap.get(key.getId().toString()).add(tagPair);
            }
        });

        return JsonUtil.G.toJson(resultMap);
    }

    /**
     * 获取最大用户账号
     *
     * @return 用户账号
     */
    private String getMaxUserAccount() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        User user = userService.page(new Page<>(1, 1), queryWrapper).getRecords().get(0);
        return user == null ? "100001" : user.getUserAccount();
    }

}

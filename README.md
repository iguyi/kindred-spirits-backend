# 道友

> 文档完善中...



# 一、项目介绍

"道友" 是一个前后端分离的**伙伴匹配系统**。前端主要使用 **Vite + Vue3 + Vant3**; 后端基于 **Spring Boot** 框架, 利用**余弦相似度算法+用户标签**实现匹配功能, 使用 **WebSocket** 实现实时通讯。



## 二、项目体验

### 一）项目获取

* 前端源码获取地址: [https://github.com/iguyi/kindred-spirits-frontend](https://github.com/iguyi/kindred-spirits-frontend)
* 后端源码获取地址: [https://github.com/iguyi/kindred-spirits-backend](https://github.com/iguyi/kindred-spirits-backend)



### 二）在线体验

在线体验地址: [http://121.40.141.242](http://121.40.141.242)

> 可以自己注册账号, 聊天内容请**遵循中华人民共和国有关法律规定**。



### 三）项目启动流程

TODO



### 四）项目上线流程

TODO



## 三、技术选型

### 一）技术栈

#### 1、前端

- Vue 3
- Vant UI 组件库
- Vite 脚手架
- Axios 请求库
- ······



#### 2、后端

- Java 8
- Spring + Spring MVC + MyBatis + MyBatis-Plus

- Spring Boot 2.7
- MySQL 数据库
- MyBatis X 插件
- Reids 缓存、Redisson 分布式锁
- Spring Scheduler 定时任务
- WebSocket
- 余弦相似度算法
- Gson、Hutool 等工具库
- Swagger + Knife4j 接口文档



### 二）应用架构

TODO



### 三）数据库架构

TODO



## 四、项目亮点

1. 使用 Redis 实现分布式 Session, 解决集群登录态同步问题。

2. 使用 Redis 缓存重点用户信息列表, 提高接口响应速度, 并通过将 Java 对象转为 JSON 字符串的方式来避免空间浪费、数据乱码的问题。

3. 为解决首次访问系统的用户首页加载慢的问题, 使用 Spring Scheduler 定时任务来实现缓存预热, 并通过 Redis 分布式锁保证在多机部署环境下, 不会重复执行定时任务。

4. 为解决同一用户重复加入队伍、入队人数超限的问题, 使用 Redis 分布式锁来实现互斥操作, 保证接口的幂等性; 并通过设计 Lock Key 来缩小锁的范围, 避免一个用户加入队伍时阻塞其他用户加入其他队伍的请求, 从而提高性能。

5. 使用 WebSocket 在单个 TCP 连接上进行全双工通信, 创建持久性的连接, 实现队伍聊天室中的实时聊天。

6. 使用余弦相似度算法实现根据用户标签推荐最相似用户的功能，并使用自己实现的数据结构来减少 TOP N 运算过程中的内存占用。

7. 使用 Java 8 的 Stream API 和 Lambda 表达式来处理复杂的集合操作, 以简化代码。

8. 利用 Java 反射 API 编写工具类, 以检查数据对象和传输对象之间是否 "逻辑相等", 避免不必要的数据更新操作。

   > "逻辑相等" 说明
   >
   > 进行数据更新时, 将 `User` 和 `UserRequest` 之间共有的、不为 `null` 的属性进行比较, 如果对应属性的值是一致, 那么认为它们在逻辑上是相等的, 没必要进行 `update` 操作。

9. 使用 Knife4j + Swagger 自动生成后端接口文档, 避免了人工编写维护文档的麻烦。



## 五、功能介绍

TODO
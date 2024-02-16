package com.guyi.kindredspirits.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 项目配置
 *
 * @author 孤诣
 */
@ConfigurationProperties("project")
@Data
public class ProjectProperties {

    /**
     * 用户头像存放位置
     */
    private String userAvatarPath;

    /**
     * 队伍头像存放位置
     */
    private String teamAvatarPath;

    /**
     * 头像对应 url 前缀
     */
    private String urlPrefix;

    /**
     * 用户默认头像访问地址
     */
    private String defaultUserAvatarPath;

    /**
     * 队伍默认头像访问地址
     */
    private String defaultTeamAvatarPath;

}

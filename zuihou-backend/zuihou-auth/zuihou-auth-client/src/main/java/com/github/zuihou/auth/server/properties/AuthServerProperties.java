package com.github.zuihou.auth.server.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.github.zuihou.auth.server.properties.AuthServerProperties.PREFIX;

/**
 * 认证服务端 属性
 *
 * @author zuihou
 * @date 2018/11/20
 */
@ConfigurationProperties(prefix = PREFIX)
@Data
@NoArgsConstructor
public class AuthServerProperties {
    public static final String PREFIX = "authentication";

    private TokenInfo user;

    @Data
    public static class TokenInfo {
        /**
         * 过期时间
         */
        private Integer expire = 7200;
        /**
         * 加密 admin服务使用
         */
        private String priKey;
        /**
         * 解密  admin 服务也需要解密
         */
        private String pubKey;
    }

}

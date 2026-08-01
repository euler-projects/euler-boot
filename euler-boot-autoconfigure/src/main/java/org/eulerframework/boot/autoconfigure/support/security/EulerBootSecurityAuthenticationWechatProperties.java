/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.boot.autoconfigure.support.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WeChat login settings.
 *
 * <p>Sign-in with WeChat is an authentication mechanism in its own
 * right, sibling to {@code euler.security.authentication.otp}
 * and {@code euler.security.authentication.app-attest}. The
 * authorization server exposes it as the WeChat authorization-code
 * grant when {@link #isEnabled() enabled}.
 *
 * <pre>
 * euler:
 *   security:
 *     authentication:
 *       wechat:
 *         enabled: true
 *         appid: wx0123456789abcdef
 *         secret: ${WECHAT_APP_SECRET}
 * </pre>
 */
@ConfigurationProperties(prefix = "euler.security.authentication.wechat")
public class EulerBootSecurityAuthenticationWechatProperties {

    /**
     * Whether WeChat login is enabled. Default is {@code false}.
     */
    private boolean enabled = false;

    /**
     * WeChat code2session endpoint exchanging an authorization code for
     * an open ID.
     */
    private String code2SessionEndpoint = "https://api.weixin.qq.com/sns/jscode2session";

    /**
     * WeChat mini-program or official-account app ID.
     */
    private String appid;

    /**
     * WeChat app secret paired with the app ID.
     */
    private String secret;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCode2SessionEndpoint() {
        return code2SessionEndpoint;
    }

    public void setCode2SessionEndpoint(String code2SessionEndpoint) {
        this.code2SessionEndpoint = code2SessionEndpoint;
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}

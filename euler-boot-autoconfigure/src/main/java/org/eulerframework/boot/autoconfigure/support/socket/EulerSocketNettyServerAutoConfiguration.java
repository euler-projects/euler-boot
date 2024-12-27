/*
 * Copyright 2013-2024 the original author or authors.
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

package org.eulerframework.boot.autoconfigure.support.socket;

import io.netty.channel.ChannelHandler;
import org.eulerframework.socket.annotation.NettyHandler;
import org.eulerframework.socket.configurers.EulerSocketServerConfiguration;
import org.eulerframework.socket.dispatcher.DefaultMessageDispatcher;
import org.eulerframework.socket.dispatcher.MessageDispatcher;
import org.eulerframework.socket.netty.NettyServer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

@AutoConfiguration
@EnableConfigurationProperties({
        EulerProtoSocketServerProperties.class
})
@ConditionalOnBean(EulerSocketNettyServerAutoConfiguration.NettyServerImporter.class)
public class EulerSocketNettyServerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(NettyServer.class)
    public NettyServer nettyServer(
            ApplicationContext applicationContext,
            EulerProtoSocketServerProperties socketServerProperties,
            MessageDispatcher<?> messageDispatcher) {
        EulerSocketServerConfiguration.setupMessageDispatcher(messageDispatcher, applicationContext);
        NettyServer.Builder builder = NettyServer.builder();
        if (socketServerProperties.isEnableSession()) {
            builder.enableSession();
        }
        return builder
                .port(socketServerProperties.getPort())
                .messageDispatcher(messageDispatcher)
                .addChannelHandlersAtLast(() -> applicationContext.getBeansWithAnnotation(NettyHandler.class)
                        .values()
                        .stream()
                        .filter(rawBean -> rawBean instanceof ChannelHandler)
                        .map(rawBean -> (ChannelHandler) rawBean)
                        .sorted(AnnotationAwareOrderComparator.INSTANCE)
                        .toArray(ChannelHandler[]::new))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(MessageDispatcher.class)
    public DefaultMessageDispatcher messageDispatcher() {
        return new DefaultMessageDispatcher();
    }

    public static class NettyServerImporter {
    }
}

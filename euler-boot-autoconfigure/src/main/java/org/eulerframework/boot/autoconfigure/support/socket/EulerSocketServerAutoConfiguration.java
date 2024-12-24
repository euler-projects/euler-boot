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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties({
        EulerProtoSocketServerProperties.class
})
@ConditionalOnBean(EulerSocketServerAutoConfiguration.NettyServerImporter.class)
public class EulerSocketServerAutoConfiguration {
    //    @Bean
//    public List<MessageHandler> messageHandlers(ApplicationContext applicationContext) {
//        Map<String, Object> socketControllers = applicationContext.getBeansWithAnnotation(EulerSocketController.class);
//
//    }
//
//
//    @NettyHandler
//    public static class MessageDispatchHandler extends ChannelInboundHandlerAdapter { // (1)
//
//        private final MessageDispatcher<?> messageDispatcher;
//
//        public MessageDispatchHandler(MessageDispatcher<?> messageDispatcher, ApplicationContext applicationContext) {
//            this.messageDispatcher = messageDispatcher;
//        }
//    }
//
//    @Bean
//    public EventLoopGroup nettyServer(ApplicationContext applicationContext, List<ChannelHandler> channelHandlers) {
//        Map<String, Object> nettyHandlers = applicationContext.getBeansWithAnnotation(NettyHandler.class);
//        List<Object> handlers = nettyHandlers.values().stream()
//                .sorted(AnnotationAwareOrderComparator.INSTANCE)
//                .toList();
//        return new NioEventLoopGroup();
//    }

    public static class NettyServerImporter {
    }
}

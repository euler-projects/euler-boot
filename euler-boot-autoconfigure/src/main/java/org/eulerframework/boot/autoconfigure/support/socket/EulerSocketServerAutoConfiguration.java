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

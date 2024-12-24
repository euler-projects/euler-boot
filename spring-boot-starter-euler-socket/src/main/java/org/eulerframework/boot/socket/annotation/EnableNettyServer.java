package org.eulerframework.boot.socket.annotation;

import org.eulerframework.boot.autoconfigure.support.socket.EulerSocketServerAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({EulerSocketServerAutoConfiguration.NettyServerImporter.class})
public @interface EnableNettyServer {
}

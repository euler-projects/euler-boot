package org.eulerframework.boot.autoconfigure.support.socket;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "euler.socket.server")
public class EulerProtoSocketServerProperties {
    private int port = 18080;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

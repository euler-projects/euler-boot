package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.eulerframework.security.core.userdetails.provisioning.EulerUserDetailsManager;
import org.springframework.core.Ordered;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.function.Supplier;

public class EulerUserDetailsManagerSupplier<U extends UserDetails, T extends EulerUserDetailsManager> implements Supplier<T> {
    private final Class<U> supportType;
    private final T userDetailsManager;
    public int order = Ordered.LOWEST_PRECEDENCE;

    public EulerUserDetailsManagerSupplier(Class<U> supportType, T userDetailsManager) {
        this.supportType = supportType;
        this.userDetailsManager = userDetailsManager;
    }


    @Override
    public T get() {
        return this.userDetailsManager;
    }

    public Class<U> getSupportType() {
        return this.supportType;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }
}

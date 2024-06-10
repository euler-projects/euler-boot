package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.eulerframework.security.core.userdetails.provisioning.EulerUserDetailsManager;
import org.springframework.core.Ordered;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.function.Supplier;

public class DelegateEulerUserDetailsManagerSupplier<U extends UserDetails, T extends EulerUserDetailsManager> implements Supplier<T> {
    private final Class<U> userDetailsType;
    private final T delegateEulerUserDetailsManager;
    public int order = Ordered.LOWEST_PRECEDENCE;

    public DelegateEulerUserDetailsManagerSupplier(Class<U> userDetailsType, T delegateEulerUserDetailsManager) {
        this.userDetailsType = userDetailsType;
        this.delegateEulerUserDetailsManager = delegateEulerUserDetailsManager;
    }


    @Override
    public T get() {
        return this.delegateEulerUserDetailsManager;
    }

    public Class<U> getUserDetailsType() {
        return this.userDetailsType;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }
}

package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.function.Supplier;

class DelegatedAuthenticationManager implements AuthenticationManager {
    private AuthenticationManager delegate;
    private Supplier<AuthenticationManager> delegateSupplier;

    DelegatedAuthenticationManager(Supplier<AuthenticationManager> delegateSupplier) {
        this.delegateSupplier = delegateSupplier;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (this.delegate != null) {
            return this.delegate.authenticate(authentication);
        }
        synchronized (this) {
            if (this.delegate == null) {
                this.delegate = this.delegateSupplier.get();
                this.delegateSupplier = null;
            }
        }
        return this.delegate.authenticate(authentication);
    }
}

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

package org.eulerframework.boot.autoconfigure.support.security.login;

import org.eulerframework.security.web.login.RegisteredLoginMethod;

import java.util.List;

/**
 * Contributes the {@link RegisteredLoginMethod}s declared for one login
 * method type to the repository backing the runtime.
 *
 * <p>This is the seam a login method type is added through, without the
 * framework being touched. A type ships three things, all of them its
 * own: a {@code LoginMethodHandler} serving it, a
 * {@code @ConfigurationProperties} class binding its declarations under
 * {@code euler.security.login-method.<method-type>}, and an
 * implementation of this interface mapping the one onto the other.
 * Publishing it as a bean is what puts the type on offer.
 *
 * <p>Declarations shared by every type are carried by
 * {@link BaseLoginMethodConfiguration}, which a type's own settings
 * class extends.
 *
 * <p>Contributions are collected once, while the repository bean is
 * built. Implementations are therefore free to read their properties
 * eagerly, and must return a stable order: it decides the order the
 * login page presents the methods in.
 */
public interface LoginMethodConfiguration {

    /**
     * Returns the registrations declared for this type, in the order
     * they are to be offered in; empty when none is declared.
     */
    List<RegisteredLoginMethod> provideRegisteredLoginMethods();
}

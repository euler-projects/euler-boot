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

/**
 * Settings every login method declaration carries, whatever its
 * type. The declaration key is the method id: registry-local,
 * never shown to users, and the fallback for
 * {@code method-name}.
 */
public abstract class BaseLoginMethodConfiguration {

    /**
     * Name identifying this method towards clients, unique
     * across all declared methods. Defaults to the declaration
     * key; declare it to address the method under a name of its
     * own, e.g. when the key is an opaque id.
     */
    private String methodName;

    /**
     * Whether this method belongs to the expanded (primary)
     * group of the login page rather than the secondary group.
     * Several methods may be primary; when none declares it, the
     * first declared method is treated as primary.
     */
    private boolean primary = false;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}

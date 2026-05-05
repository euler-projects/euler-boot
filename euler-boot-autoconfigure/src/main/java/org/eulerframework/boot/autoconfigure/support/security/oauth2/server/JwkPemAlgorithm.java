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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.server;

/**
 * JWS algorithms accepted by the declarative
 * {@code euler.security.oauth2.authorizationserver.jwk.keys.*.alg} property.
 * The enum value maps directly to the JOSE {@code alg} header that will be
 * carried by JWS tokens signed with the corresponding pre-configured PEM key.
 *
 * <ul>
 *   <li>RSA: {@code RS256 / RS384 / RS512} (PKCS#8 PEM, key size derived from the PEM).</li>
 *   <li>EC: {@code ES256 / ES384 / ES512} bound to curves {@code P-256 / P-384 / P-521}.</li>
 * </ul>
 */
public enum JwkPemAlgorithm {

    RS256, RS384, RS512,
    ES256, ES384, ES512;

    /**
     * @return the JOSE algorithm name..
     */
    public String joseName() {
        return name();
    }
}

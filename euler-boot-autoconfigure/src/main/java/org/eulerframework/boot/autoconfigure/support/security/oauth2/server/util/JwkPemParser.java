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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.server.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;

/**
 * Minimal PEM → {@link JWK} adapter. Given the textual contents of a PEM
 * document, returns the Nimbus {@link JWK} produced by
 * {@link JWK#parseFromPEMEncodedObjects}. Resource resolution (Spring
 * {@code ResourceLoader}, classpath, filesystem, etc.) is the caller's
 * responsibility &mdash; this class deliberately does not depend on Spring.
 *
 * <p>Supports only what Nimbus' PEM parser supports out of the box: RSA
 * ({@code RS256/RS384/RS512}) and EC ({@code ES256/ES384/ES512}). Ed25519 PEMs
 * are intentionally <em>not</em> supported here to avoid pulling BouncyCastle
 * bridging into the framework; runtime key generation for Ed25519 remains
 * available at the application layer.
 *
 * <p>Algorithm validation, JWK metadata enrichment ({@code kid/alg/use/iat})
 * and lifecycle status handling live in the caller (typically
 * {@link JwkEntryParser}).
 */
public final class JwkPemParser {

    private JwkPemParser() {
    }

    /**
     * Parse a PEM payload into a Nimbus {@link JWK} (RSA or EC).
     *
     * @throws IllegalStateException when the PEM cannot be parsed
     */
    public static JWK parse(String pemContent) {
        try {
            return JWK.parseFromPEMEncodedObjects(pemContent);
        } catch (JOSEException ex) {
            throw new IllegalStateException(
                    "Failed to parse PEM (supported algorithms: RSA, EC; Ed25519 PEMs are not supported)", ex);
        }
    }

}

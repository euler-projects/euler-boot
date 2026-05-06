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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.eulerframework.boot.autoconfigure.support.security.oauth2.server.EulerBootAuthorizationServerJwkProperties.KeyDefinition;
import org.eulerframework.security.jwk.JwkEntry;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Materialize a {@link KeyDefinition} into a fully-populated {@link JwkEntry}
 * by loading the declared PEM resource via {@link JwkPemParser} and enriching
 * the parsed Nimbus {@link JWK} with {@code kid / alg / use / iat}.
 *
 * <p>Per-entry validation (algorithm ↔ key material match, the {@code use=sig}
 * constraint, etc.) is enforced here; cross-entry aggregate invariants
 * (e.g. exactly one ACTIVE key per algorithm) are deferred to the repository
 * at load time. The instance keeps a single {@link ResourceLoader} reference
 * so all helper methods can resolve PEM locations uniformly without it being
 * threaded through method signatures.
 */
public class JwkEntryParser {

    private final ResourceLoader loader;

    public JwkEntryParser(ResourceLoader resourceLoader) {
        this.loader = resourceLoader;
    }

    /**
     * Convert a single {@link KeyDefinition} into a fully-populated
     * {@link JwkEntry}. Mandatory fields ({@code kid}, {@code alg},
     * {@code use}, {@code status}, {@code iat}, {@code pem}) are guaranteed
     * non-blank by the property setters; the only remaining check here is
     * that the caller has already resolved the {@code kid} (either explicitly
     * set on the definition or defaulted to the enclosing map key).
     *
     * @param def declarative key entry
     * @return the constructed {@link JwkEntry}; never {@code null}
     * @throws IllegalStateException when the PEM cannot be resolved/parsed,
     *                               when the parsed key material does not
     *                               match {@code alg}, or when the JWK cannot
     *                               be enriched with the management metadata
     */
    public JwkEntry parse(KeyDefinition def) {
        Assert.hasText(def.getKid(), "kid must not be blank");

        JWK parsed = loadPem(def);
        JWSAlgorithm alg = JWSAlgorithm.parse(def.getAlg().joseName());
        KeyUse keyUse = parseKeyUse(def);
        validateAlgorithmMatchesKey(def, alg, parsed);
        JWK enriched = enrich(def, alg, keyUse, parsed);
        return new JwkEntry(enriched, def.getStatus());
    }

    /**
     * Resolve {@link KeyDefinition#getPem()} via the configured
     * {@link ResourceLoader} ({@code file:...} / {@code classpath:...} and
     * plain filesystem paths are honoured uniformly) and parse it into a
     * Nimbus {@link JWK} via {@link JwkPemParser}.
     */
    private JWK loadPem(KeyDefinition def) {
        String location = def.getPem();
        Resource resource = loader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Entry " + def.getKid() + ": PEM resource not found at '" + location + "'");
        }
        try (InputStream in = resource.getInputStream()) {
            return JwkPemParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        catch (IOException ex) {
            throw new IllegalStateException(
                    "Entry " + def.getKid() + ": unable to read PEM at '" + location + "'", ex);
        }
    }

    /**
     * Parse {@link KeyDefinition#getUse()} into a Nimbus {@link KeyUse}.
     * Only {@code sig} is supported by the JWK subsystem; encryption keys
     * have no role in the OAuth2 token signing pipeline and are rejected
     * eagerly to surface configuration mistakes at startup.
     */
    private static KeyUse parseKeyUse(KeyDefinition def) {
        String raw = def.getUse();
        try {
            KeyUse keyUse = KeyUse.parse(raw);
            if (!KeyUse.SIGNATURE.equals(keyUse)) {
                throw new IllegalStateException(
                        "Entry " + def.getKid() + ": only use=sig is supported, got " + raw);
            }
            return keyUse;
        }
        catch (ParseException ex) {
            throw new IllegalStateException(
                    "Entry " + def.getKid() + ": invalid 'use' value '" + raw + "'", ex);
        }
    }

    /**
     * Verify that the parsed key material matches the declared JWS algorithm.
     * Supported: {@code RS256/384/512}, {@code ES256/384/512} (curves
     * {@code P-256/384/521}).
     */
    private static void validateAlgorithmMatchesKey(KeyDefinition def, JWSAlgorithm alg, JWK jwk) {
        String name = alg.getName();
        if (name.startsWith("RS")) {
            if (!(jwk instanceof RSAKey)) {
                throw new IllegalStateException(
                        "Entry " + def.getKid() + ": alg=" + name + " expects an RSA PEM");
            }
            return;
        }
        if (name.startsWith("ES")) {
            if (!(jwk instanceof ECKey ec)) {
                throw new IllegalStateException(
                        "Entry " + def.getKid() + ": alg=" + name + " expects an EC PEM");
            }
            Curve expected = switch (name) {
                case "ES256" -> Curve.P_256;
                case "ES384" -> Curve.P_384;
                case "ES512" -> Curve.P_521;
                default -> throw new IllegalStateException(
                        "Entry " + def.getKid() + ": unsupported EC alg " + name);
            };
            if (!expected.equals(ec.getCurve())) {
                throw new IllegalStateException("Entry " + def.getKid() + ": alg=" + name
                        + " requires curve " + expected.getName() + " but PEM uses " + ec.getCurve());
            }
            return;
        }
        throw new IllegalStateException("Entry " + def.getKid() + ": unsupported alg " + name
                + " (supported: RS256/RS384/RS512, ES256/ES384/ES512)");
    }

    /**
     * Enrich the parsed JWK with the management-layer metadata
     * ({@code kid / alg / use / iat}). The original parsed JWK is not mutated;
     * a re-parsed copy carrying the additions is returned.
     */
    private static JWK enrich(KeyDefinition def, JWSAlgorithm alg, KeyUse keyUse, JWK parsed) {
        Map<String, Object> json = new HashMap<>(parsed.toJSONObject());
        json.put("kid", def.getKid());
        json.put("alg", alg.getName());
        json.put("use", keyUse.identifier());
        json.put("iat", def.getIat().getEpochSecond());
        try {
            return JWK.parse(json);
        }
        catch (ParseException ex) {
            throw new IllegalStateException(
                    "Entry " + def.getKid() + ": failed to enrich JWK with kid/alg/use/iat", ex);
        }
    }
}


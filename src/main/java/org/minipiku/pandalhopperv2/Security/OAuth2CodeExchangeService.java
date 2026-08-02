package org.minipiku.pandalhopperv2.Security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.minipiku.pandalhopperv2.DTOs.AuthDTO.LoginResponseDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Backs the OAuth2 redirect with a short-lived, single-use code instead of the
 * JWT itself.
 *
 * <p>The previous flow redirected to the frontend with {@code ?token=<JWT>},
 * which persisted a live credential into browser history, {@code Referer}
 * headers, and any proxy or analytics log along the way. Here the redirect
 * carries only an opaque code; the frontend POSTs it to {@code /auth/exchange}
 * to obtain the JWT in a response body, and the code is destroyed on first use.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2CodeExchangeService {

    private static final String KEY_PREFIX = "oauth2:code:";
    private static final Duration CODE_TTL = Duration.ofSeconds(60);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    /**
     * Stores the issued JWT against a fresh random code and returns the code.
     */
    public String issueCode(LoginResponseDTO login) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // Value shape: "<userId>:<jwt>" - avoids pulling in a serializer for a
        // two-field payload that never leaves this class.
        String payload = login.getUserId() + ":" + login.getJwt();
        redisTemplate.opsForValue().set(KEY_PREFIX + code, payload, CODE_TTL);

        return code;
    }

    /**
     * Atomically consumes a code and returns the credentials it stood for, or
     * {@code null} if the code is unknown, already used, or expired.
     */
    public LoginResponseDTO redeemCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        // getAndDelete makes redemption single-use even if two requests race.
        String payload = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);
        if (payload == null) {
            return null;
        }

        int separator = payload.indexOf(':');
        if (separator < 0) {
            log.error("Malformed OAuth2 exchange payload for code ending ...{}",
                    code.substring(Math.max(0, code.length() - 4)));
            return null;
        }

        Long userId = Long.valueOf(payload.substring(0, separator));
        String jwt = payload.substring(separator + 1);
        return new LoginResponseDTO(jwt, userId);
    }
}

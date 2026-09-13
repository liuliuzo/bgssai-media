package com.bgssai.media.common.oauth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthStateStore {

    private static final long TTL_MS = 10 * 60 * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final class Entry {
        public final String provider;
        public final String codeVerifier;
        public final long createdAt;

        Entry(String provider, String codeVerifier, long createdAt) {
            this.provider = provider;
            this.codeVerifier = codeVerifier;
            this.createdAt = createdAt;
        }
    }

    private final Map<String, Entry> pending = new ConcurrentHashMap<>();

    public String issue(String provider, String codeVerifier) {
        sweep();
        String state = randomToken(24);
        pending.put(state, new Entry(OAuthSettings.normalize(provider), codeVerifier, System.currentTimeMillis()));
        return state;
    }

    public Entry consume(String state) {
        sweep();
        if (state == null || state.isBlank()) {
            return null;
        }
        return pending.remove(state);
    }

    public static String randomToken(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().createdAt > TTL_MS) {
                it.remove();
            }
        }
    }
}

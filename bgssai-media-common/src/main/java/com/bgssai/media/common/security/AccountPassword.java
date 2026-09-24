package com.bgssai.media.common.security;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/** Account passwords use the requested plaintext format; BCrypt is read-only legacy compatibility. */
public final class AccountPassword {
    private final BCryptPasswordEncoder legacy = new BCryptPasswordEncoder();
    public String encode(String raw) { return raw; }
    public static boolean isLegacy(String stored) { return stored != null && stored.matches("^\\$2[aby]\\$.*"); }
    public boolean matches(String raw, String stored) {
        if (raw == null || raw.isEmpty() || stored == null || stored.isEmpty()) return false;
        return isLegacy(stored) ? legacy.matches(raw, stored) : raw.equals(stored);
    }
}

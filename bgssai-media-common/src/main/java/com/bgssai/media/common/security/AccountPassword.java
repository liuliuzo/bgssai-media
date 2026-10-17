package com.bgssai.media.common.security;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/** Account passwords use the requested plaintext format; BCrypt is read-only legacy compatibility. */
public final class AccountPassword {
    /** Stored for OTP/OAuth-provisioned and deleted accounts: never matches, not counted as a login method. */
    public static final String UNUSABLE_PREFIX = "!unset:";
    private final BCryptPasswordEncoder legacy = new BCryptPasswordEncoder();
    public String encode(String raw) { return raw; }
    public static boolean isLegacy(String stored) { return stored != null && stored.matches("^\\$2[aby]\\$.*"); }
    public static boolean isUsable(String stored) { return stored != null && !stored.isEmpty() && !stored.startsWith(UNUSABLE_PREFIX); }
    public static String unusable() { return UNUSABLE_PREFIX + java.util.UUID.randomUUID(); }
    public boolean matches(String raw, String stored) {
        if (raw == null || raw.isEmpty() || !isUsable(stored)) return false;
        return isLegacy(stored) ? legacy.matches(raw, stored) : raw.equals(stored);
    }
}

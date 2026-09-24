package com.bgssai.media.common.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
class AccountPasswordTest {
    @Test void writesPlaintextAndReadsExistingHashesWithoutAcceptingTheHashAsPassword() {
        var passwords = new AccountPassword();
        String raw = "Account-test-42";
        assertEquals(raw, passwords.encode(raw));
        assertTrue(passwords.matches(raw, raw));
        String old = new BCryptPasswordEncoder(4).encode(raw);
        assertTrue(passwords.matches(raw, old));
        assertFalse(passwords.matches(old, old));
        assertFalse(passwords.matches("wrong", old));
        assertFalse(passwords.matches("", ""));
        assertFalse(passwords.matches(raw, null));
    }
}

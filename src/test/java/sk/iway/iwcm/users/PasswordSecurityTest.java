package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.security.crypto.bcrypt.BCrypt;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.users.crypto.Bcrypt;

class PasswordSecurityTest {

    static Stream<String> boundaryPasswords() {
        return Stream.of("Ab9!".repeat(18), "Ab9!é".repeat(12), "😀".repeat(18));
    }

    @ParameterizedTest
    @MethodSource("boundaryPasswords")
    void shouldAcceptExactly72BytesAndRejectWrongPassword(String password) {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getString("passwordHashAlgorithm")).thenReturn("bcrypt");
            String salt = "bcrypt:" + BCrypt.gensalt(4);
            String hash = PasswordSecurity.calculateHash(password, salt);

            assertTrue(PasswordSecurity.isPasswordCorrect(password, salt, hash));
            assertFalse(PasswordSecurity.isPasswordCorrect("wrong-password", salt, hash));
            assertFalse(PasswordSecurity.isPasswordCorrect(password + " ", salt, hash),
                    "Trailing whitespace must count towards the byte limit");
        }
    }

    @ParameterizedTest
    @MethodSource("boundaryPasswords")
    void shouldRejectBothOriginalAndCollidingOverlongPasswords(String prefix) {
        String salt = BCrypt.gensalt(4);
        // Hashing the 72-byte prefix reproduces the legacy hash of prefix + "X".
        // Current Spring Security rejects overlong input when creating a new hash.
        String storedHash = new Bcrypt().calculateHash(prefix, salt);
        String oldHash = storedHash.substring("bcrypt:".length());
        assertTrue(BCrypt.checkpw(prefix + "Y", oldHash),
                "BCrypt.checkpw still accepts truncated passwords for legacy compatibility");

        assertFalse(PasswordSecurity.isPasswordCorrect(prefix + "X", "bcrypt:" + salt, storedHash));
        assertFalse(PasswordSecurity.isPasswordCorrect(prefix + "Y", "bcrypt:" + salt, storedHash));
        assertFalse(new Bcrypt().isPasswordCorrect(prefix + "Y", salt, oldHash),
                "Direct wrapper calls must enforce the same limit");
    }

    @ParameterizedTest
    @MethodSource("boundaryPasswords")
    void shouldRejectHashingOverlongBcryptPasswords(String prefix) {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getString("passwordHashAlgorithm")).thenReturn("bcrypt");
            String salt = "bcrypt:" + BCrypt.gensalt(4);

            assertThrows(IllegalArgumentException.class,
                    () -> PasswordSecurity.calculateHash(prefix + "X", salt));
        }
    }

    @Test
    void shouldAcceptRepeatedCharactersWithinByteLimit() {
        String password = "A".repeat(65);
        String salt = "bcrypt:" + BCrypt.gensalt(4);
        String hash = new Bcrypt().calculateHash(password, salt);

        assertTrue(PasswordSecurity.isPasswordCorrect(password, salt, hash),
                "Repeated characters are not the cause of CVE-2025-22228");
    }

    @Test
    void shouldPreserveLongLegacySha512Passwords() {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getString("passwordHashAlgorithm")).thenReturn("bcrypt");
            constants.when(() -> Constants.getInt("passwordUseHashIterations")).thenReturn(1);
            String password = "A".repeat(80);
            String salt = "1234567890abcdef";
            String hash = PasswordSecurity.calculateHash(password, salt);

            assertTrue(PasswordSecurity.isPasswordCorrect(password, salt, hash));
            assertFalse(PasswordSecurity.isPasswordCorrect(password + "X", salt, hash));
        }
    }

    @Test
    void shouldVerifyStoredBcryptLimitIndependentlyOfCurrentConfiguration() {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getString("passwordHashAlgorithm")).thenReturn("sha512");
            String prefix = "Ab9!".repeat(18);
            String salt = BCrypt.gensalt(4);
            String hash = "bcrypt:" + BCrypt.hashpw(prefix, salt);

            assertTrue(PasswordSecurity.isPasswordCorrect(prefix, "bcrypt:" + salt, hash));
            assertFalse(PasswordSecurity.isPasswordCorrect(prefix + "X", "bcrypt:" + salt, hash));
        }
    }
}

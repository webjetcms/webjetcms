package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        // Hash creation is unchanged; the guard only rejects overlong input during verification.
        String storedHash = new Bcrypt().calculateHash(prefix + "X", salt);
        String oldHash = storedHash.substring("bcrypt:".length());
        assertTrue(BCrypt.checkpw(prefix + "Y", oldHash), "The library alone still truncates passwords");

        assertFalse(PasswordSecurity.isPasswordCorrect(prefix + "X", "bcrypt:" + salt, storedHash));
        assertFalse(PasswordSecurity.isPasswordCorrect(prefix + "Y", "bcrypt:" + salt, storedHash));
        assertFalse(new Bcrypt().isPasswordCorrect(prefix + "Y", salt, oldHash),
                "Direct wrapper calls must enforce the same limit");
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
            String password = "Ab9!".repeat(18) + "X";
            String salt = BCrypt.gensalt(4);
            String hash = "bcrypt:" + BCrypt.hashpw(password, salt);

            assertFalse(PasswordSecurity.isPasswordCorrect(password, "bcrypt:" + salt, hash));
        }
    }
}

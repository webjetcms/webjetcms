package sk.iway.iwcm.dmail.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import sk.iway.iwcm.dmail.DmailUtil;
import sk.iway.iwcm.test.BaseWebjetTest;

class EmailsRestControllerTest extends BaseWebjetTest {

    @ParameterizedTest
    @MethodSource("provideEmailsForTesting")
    void testParseEmailFromNameEmailFormat(String email, String expected) {
        String actual = DmailUtil.parseEmailFromNameEmailFormat(email);
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> provideEmailsForTesting() {
        return Stream.of(
            Arguments.of("John Doe <john.doe@example.com>", "john.doe@example.com"),
            Arguments.of("Jane Doe <j@e.com>", "j@e.com"),
            Arguments.of("Test User <user@example>", "user@example"),
            Arguments.of("test.user@example.com", "test.user@example.com"),
            Arguments.of("Ferko Tester", "Ferko Tester")
        );
    }

}
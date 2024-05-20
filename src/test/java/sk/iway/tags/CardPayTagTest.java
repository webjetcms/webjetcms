package sk.iway.tags;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CardPayTagTest {
    @Test
    void testGetSign() throws Exception {
        // Arrange
        String toHash = "exampleToHash";
        String key = "0E329232";
        String expectedSign = "ECA1EBFAFE9A1AD8"; // Replace with the expected sign value

        // Act
        String actualSign = CardPayTag.getSign(null, toHash, key);

        // Assert
        assertEquals(expectedSign, actualSign);
    }
}

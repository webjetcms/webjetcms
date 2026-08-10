package sk.iway.iwcm.components.ai.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.ai.dto.InputDataDTO;

class IncludesHandlerTest {

    @Test
    void restoresProtectedIncludeSplitAcrossStreamFragments() throws Exception {
        String include = "!INCLUDE(/components/banner.jsp)!";
        InputDataDTO inputData = inputData("Before " + include + " after");
        IncludesHandler handler = IncludesHandler.protectIncludes(inputData);
        String token = inputData.getInputValue().substring("Before ".length(), inputData.getInputValue().length() - " after".length());
        StringWriter output = new StringWriter();
        BufferedWriter writer = new BufferedWriter(output);

        int split = token.length() / 2;
        handler.handleLine("Before " + token.substring(0, split), writer);
        handler.handleLine(token.substring(split) + " after", writer);
        handler.finish(writer);

        String expected = "Before " + include + " after";
        assertEquals(expected, output.toString());
        assertEquals(expected, handler.getWholeResponse());
    }

    @Test
    void leavesBackendStructuredInputUntouched() {
        String original = "{\"text\":\"!INCLUDE(/components/banner.jsp)!\"}";
        InputDataDTO inputData = inputData(original);
        inputData.setStructuredInput(true);

        IncludesHandler handler = IncludesHandler.protectIncludes(inputData);

        assertFalse(handler.hasIncludes());
        assertEquals(original, inputData.getInputValue());
    }

    private static InputDataDTO inputData(String inputValue) {
        InputDataDTO inputData = new InputDataDTO();
        inputData.setInputValue(inputValue);
        return inputData;
    }
}

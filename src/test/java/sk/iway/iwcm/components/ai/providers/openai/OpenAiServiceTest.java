package sk.iway.iwcm.components.ai.providers.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;

class OpenAiServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void textRequestUsesSystemInputAndUntrustedUserContent() throws Exception {
        Constants.setString("ai_openAiAuthKey", "test-key");

        OpenAiService service = new OpenAiService();
        AssistantDefinitionEntity assistant = createAssistant("openai-test");
        assistant.setUseStreaming(true);

        InputDataDTO inputData = new InputDataDTO();
        inputData.setInputValueType(InputDataDTO.InputValueType.TEXT);
        inputData.setInputValue("Improve this text.");

        String instructions = "Fix grammar.";

        HttpPost post = (HttpPost) service.getResponseRequest(instructions, inputData, assistant, null);
        JsonNode root = MAPPER.readTree(EntityUtils.toString(post.getEntity()));
        assertTextRequestBody(root, "openai-test");

        HttpPost streamPost = (HttpPost) service.getStremResponseRequest(instructions, inputData, assistant, null);
        JsonNode streamRoot = MAPPER.readTree(EntityUtils.toString(streamPost.getEntity()));
        assertTextRequestBody(streamRoot, "openai-test");
        assertTrue(streamRoot.path("stream").asBoolean());
    }

    @Test
    void imageRequestIncludesTaskInstructionsAndUntrustedUserContent() throws Exception {
        Constants.setString("ai_openAiAuthKey", "test-key");

        OpenAiService service = new OpenAiService();
        AssistantDefinitionEntity assistant = createAssistant("gpt-image-1");

        InputDataDTO inputData = new InputDataDTO();
        inputData.setInputValueType(InputDataDTO.InputValueType.TEXT);
        inputData.setInputValue("Draw a blue icon.");
        inputData.setImageCount(2);
        inputData.setImageSize("1024x1536");
        inputData.setImageQuality("high");

        String instructions = "Create an image.";
        HttpPost post = (HttpPost) service.getImageResponseRequest(instructions, inputData, assistant, null, null);
        JsonNode root = MAPPER.readTree(EntityUtils.toString(post.getEntity()));
        String prompt = root.path("prompt").asText();

        assertEquals("https://api.openai.com/v1/images/generations", post.getURI().toString());
        assertEquals("gpt-image-1", root.path("model").asText());
        assertEquals(2, root.path("n").asInt());
        assertEquals("1024x1536", root.path("size").asText());
        assertEquals("high", root.path("quality").asText());
        assertTrue(prompt.contains("[AI_PROMPT_SECURITY_RULES_BEGIN]"));
        assertTrue(prompt.contains("[TASK_INSTRUCTIONS_BEGIN]"));
        assertTrue(prompt.contains("Create an image."));
        assertTrue(prompt.contains("[BEGIN_UNTRUSTED_INPUT_TEXT]"));
    }

    @Test
    void imageEditRequestIncludesTaskInstructionsUserPromptAndImageFile() throws Exception {
        Constants.setString("ai_openAiAuthKey", "test-key");

        OpenAiService service = new OpenAiService();
        AssistantDefinitionEntity assistant = createAssistant("gpt-image-1");

        byte[] imageBytes = new byte[] { 1, 2, 3, 4 };
        Path imageFile = tempDir.resolve("source.png");
        Files.write(imageFile, imageBytes);

        InputDataDTO inputData = new InputDataDTO();
        inputData.setInputValueType(InputDataDTO.InputValueType.IMAGE);
        inputData.setInputFile(imageFile.toFile());
        inputData.setUserPrompt("Keep transparent edges.");

        String instructions = "Take provided image and remove background.";
        HttpPost post = (HttpPost) service.getImageResponseRequest(instructions, inputData, assistant, null, null);
        String body = getMultipartBody(post);

        assertEquals("https://api.openai.com/v1/images/edits", post.getURI().toString());
        assertTrue(body.contains("name=\"model\""));
        assertTrue(body.contains("gpt-image-1"));
        assertTrue(body.contains("name=\"prompt\""));
        assertTrue(body.contains("[AI_PROMPT_SECURITY_RULES_BEGIN]"));
        assertTrue(body.contains("[TASK_INSTRUCTIONS_BEGIN]"));
        assertTrue(body.contains("remove background"));
        assertTrue(body.contains("[BEGIN_UNTRUSTED_USER_PROMPT]"));
        assertTrue(body.contains("name=\"image\"; filename=\"source.png\""));
    }

    private AssistantDefinitionEntity createAssistant(String model) {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setModel(model);
        assistant.setUseTemporal(false);
        assistant.setUseStreaming(false);
        return assistant;
    }

    private void assertTextRequestBody(JsonNode root, String model) {
        JsonNode input = root.path("input");

        assertEquals(model, root.path("model").asText());
        assertEquals(2, input.size());
        assertEquals("system", input.get(0).path("role").asText());
        assertTrue(input.get(0).path("content").asText().contains("[AI_PROMPT_SECURITY_RULES_BEGIN]"));
        assertTrue(input.get(0).path("content").asText().contains("[TASK_INSTRUCTIONS_BEGIN]"));
        assertFalse(input.get(0).path("content").asText().contains("[BEGIN_UNTRUSTED_INPUT_TEXT]"));
        assertEquals("user", input.get(1).path("role").asText());
        assertTrue(input.get(1).path("content").asText().contains("[BEGIN_UNTRUSTED_INPUT_TEXT]"));
        assertTrue(root.path("store").asBoolean());
    }

    private String getMultipartBody(HttpPost post) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        post.getEntity().writeTo(output);
        return new String(output.toByteArray(), StandardCharsets.ISO_8859_1);
    }
}

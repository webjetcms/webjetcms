package sk.iway.iwcm.components.ai.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.providers.openai.OpenAiService;
import sk.iway.iwcm.i18n.Prop;

@RestController
@RequestMapping("/admin/rest/ai/assistant/")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class AssistantController {

    private final OpenAiService openAiService;

    @Autowired
    public AssistantController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping(value = "/response/")
    public AssistantResponseDTO getAiReponse(@RequestParam("assistantName") String assistantName, @RequestParam("inputData") String inputData, HttpServletRequest request) {
        AssistantResponseDTO response = null;
        String exceptionMessage = null;
        try {
            response = openAiService.getAiResponse(assistantName, inputData, Prop.getInstance(request));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            exceptionMessage = e.getLocalizedMessage();
        }
        if (response == null) {
            response = new AssistantResponseDTO();
            response.setError("Something went wrong, please try again later: " + exceptionMessage);
        }

        return response;
    }
}
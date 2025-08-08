package sk.iway.iwcm.kokos;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.i18n.Prop;

@RestController
@RequestMapping("/admin/rest/openai/")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class OpenAiController {

    private final OpenAiService openAiService;

    @Autowired
    public OpenAiController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @GetMapping(value = "doc-perex", produces = "text/plain")
    public String getGeneratedDocPerex(@RequestParam("assistantName") String assistantName, @RequestParam("inputData") String inputData, HttpServletRequest request) {
        try {
            String answer = openAiService.getAiResponse(assistantName, inputData, Prop.getInstance(request));
            return answer;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return "Something went wrong, please try again later.";
    }
}
package sk.iway.iwcm.components.ai.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;

@RestController
@RequestMapping("/admin/rest/ai/assistant/")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_ai_tools')")
public class AssistantController {

    private final AiService aiService;
    private final AiStatRepository statRepo;
    private final AssistantDefinitionRepository assistantRepo;

    @Autowired
    public AssistantController(AiService aiService, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo) {
        this.aiService = aiService;
        this.statRepo = statRepo;
        this.assistantRepo = assistantRepo;
    }

    @PostMapping(value = "/response/")
    public AssistantResponseDTO getAiReponse(@RequestParam("assistantName") String assistantName, @RequestParam("inputData") String inputData, HttpServletRequest request) {
        AssistantResponseDTO response = null;
        String exceptionMessage = null;
        try {
            response = aiService.getAiResponse(assistantName, inputData, Prop.getInstance(request), statRepo, assistantRepo);
        } catch (Exception e) {
            e.printStackTrace();
            exceptionMessage = e.getLocalizedMessage();
        }

        if (response == null) {
            response = new AssistantResponseDTO();
            response.setError("Something went wrong, please try again later: " + exceptionMessage);
        }

        return response;
    }

    @PostMapping(value = "/response-image/")
    public AssistantResponseDTO getAiImageReponse(@RequestParam("assistantName") String assistantName,  @RequestParam("inputData") String inputData, HttpServletRequest request) {
        AssistantResponseDTO response = null;
        String exceptionMessage = null;

        try {
            response = aiService.getAiImageResponse(assistantName, inputData, Prop.getInstance(request), statRepo, assistantRepo);
        } catch (Exception e) {
            e.printStackTrace();
            exceptionMessage = e.getLocalizedMessage();
        }

        if (response == null) {
            response = new AssistantResponseDTO();
            response.setError("Something went wrong, please try again later: " + exceptionMessage);
        }

        return response;
    }

    @PostMapping("/response-stream/")
    public void streamData(@RequestBody Map<String, String> data, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter writer = response.getWriter();
        String assistantName = data.get("assistantName");
        String inputData = data.get("inputData");

        AssistantResponseDTO responseDto = null;
        String exceptionMessage = null;

        try {
            responseDto = aiService.getAiStreamResponse(assistantName, inputData, Prop.getInstance(request), statRepo, assistantRepo, writer);
        } catch(Exception e) {
            e.printStackTrace();
            exceptionMessage = e.getLocalizedMessage();
        } finally {

            if (responseDto == null) {
                responseDto = new AssistantResponseDTO();
                responseDto.setError("Something went wrong, please try again later: " + exceptionMessage);
            }

            writer.write(responseDto.toJsonString());
            writer.flush();
            writer.close();
        }
    }


    @GetMapping("/file/binary/")
	public void execute(@RequestParam String fileName, HttpServletRequest request, HttpServletResponse response) {
        try {
            AiTempFileStorage.downloadFile(fileName, request, response);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }
    }

    @PostMapping("/save-temp-file/")
    public String saveTempFile(@RequestParam("tempFileName") String tempFileName, @RequestParam("imageName") String imageName, @RequestParam("imageLocation") String imageLocation, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return AiTempFileStorage.saveTempFile(tempFileName, imageName, imageLocation);
    }
}
package sk.iway.iwcm.components.ai.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
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

    @PostMapping(value = "/response/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssistantResponseDTO getAiReponse(@RequestBody InputDataDTO inputData, HttpServletRequest request) {
        AssistantResponseDTO response = null;
        String exceptionMessage = null;
        try {
            response = aiService.getAiResponse(inputData, statRepo, assistantRepo, request);
        } catch (Exception e) {
            e.printStackTrace();
            exceptionMessage = e.getLocalizedMessage();
        }

        if (response == null) {
            response = new AssistantResponseDTO();
            response.setError(getErrMsg(exceptionMessage, request));
        }

        return response;
    }

    @PostMapping(value = "/response-image/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssistantResponseDTO getAiImageReponse(@RequestBody InputDataDTO inputData, HttpServletRequest request) {
        AssistantResponseDTO response = null;
        String exceptionMessage = null;

        try {
            response = aiService.getAiImageResponse(inputData, statRepo, assistantRepo, request);
        } catch (Exception e) {
            e.printStackTrace();
            exceptionMessage = e.getLocalizedMessage();
        }

        if (response == null) {
            response = new AssistantResponseDTO();
            response.setError(getErrMsg(exceptionMessage, request));
        }

        return response;
    }

    @PostMapping("/response-stream/")
    public void streamData(@RequestBody Map<String, String> data, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter writer = response.getWriter();
        AssistantResponseDTO responseDto = null;
        String exceptionMessage = null;

        try {
            responseDto = aiService.getAiStreamResponse(new InputDataDTO(data), statRepo, assistantRepo, writer, request);
        } catch(Exception e) {
            e.printStackTrace();
            exceptionMessage = e.getLocalizedMessage();
        } finally {

            if (responseDto == null) {
                responseDto = new AssistantResponseDTO();
                responseDto.setError(getErrMsg(exceptionMessage, request));
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
        return AiTempFileStorage.saveTempFile(tempFileName, imageName, imageLocation, request);
    }

    @GetMapping(value = "/bonus-content/", produces = "text/plain; charset=UTF-8")
    public String getBonusContent(@RequestParam("assistantName") String assistantName, HttpServletRequest request, HttpServletResponse response) {
        return aiService.getBonusHtml(assistantName, assistantRepo, request);
    }

    @GetMapping("/new-image-location/")
    public String getNewImageLocation(@RequestParam("docId") Integer docId, @RequestParam("groupId") Integer groupId, @RequestParam("title") String title) {
        return UploadFileTools.getPageUploadSubDir(docId, groupId, title, "/images");
    }

    private String getErrMsg(String errMsg, HttpServletRequest request) {
        return Prop.getInstance(request).getText("html_area.insert_image.error_occured") + " : " + errMsg;
    }
}
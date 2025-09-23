package sk.iway.iwcm.components.ai.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import sk.iway.iwcm.system.datatable.json.DataTableAi;
import sk.iway.iwcm.system.datatable.json.DataTableColumn;

/**
 * REST controller for AI assistants - handles XHR requests from UI
 */
@RestController
@RequestMapping("/admin/rest/ai/assistant/")
@PreAuthorize("@WebjetSecurityService.isAdmin()") //AI assistants can be in any module, so check just for admin perms
public class AssistantController {

    private final AiService aiService;
    private final AiStatRepository statRepo;
    private final AssistantDefinitionRepository assistantRepo;

    private final AiTaskRegistry aiTaskRegistry;

    @Autowired
    public AssistantController(AiService aiService, AiStatRepository statRepo, AssistantDefinitionRepository assistantRepo, AiTaskRegistry aiTaskRegistry) {
        this.aiService = aiService;
        this.statRepo = statRepo;
        this.assistantRepo = assistantRepo;
        this.aiTaskRegistry = aiTaskRegistry;
    }

    @PostMapping(value = "/response/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssistantResponseDTO getAiReponse(@RequestBody InputDataDTO inputData, HttpServletRequest request) {
        AssistantResponseDTO responseDto = null;
        String exceptionMessage = null;
        try {
            responseDto = aiService.getAiResponse(inputData, statRepo, assistantRepo, request);
        } catch (Exception e) {
            e.printStackTrace();
            exceptionMessage = e.getLocalizedMessage();
        }

        if (responseDto == null) {
            responseDto = new AssistantResponseDTO();
            responseDto.setError(getErrMsg(exceptionMessage, request));
        }

        return responseDto;
    }

    @PostMapping(value = "/response-image/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssistantResponseDTO getAiImageReponse(@RequestBody InputDataDTO inputData, HttpServletRequest request) {
        AssistantResponseDTO responseDto = null;
        String exceptionMessage = null;

        try {
            responseDto = aiService.getAiImageResponse(inputData, statRepo, assistantRepo, request);
        } catch (Exception e) {
            e.printStackTrace();
            exceptionMessage = e.getLocalizedMessage();
        }

        if (responseDto == null) {
            responseDto = new AssistantResponseDTO();
            responseDto.setError(getErrMsg(exceptionMessage, request));
        }

        return responseDto;
    }

    @PostMapping("/response-stream/")
    public void streamData(@RequestBody Map<String, String> data, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=utf-8");
        response.setCharacterEncoding("UTF-8");

        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)
        );

        //5 hours of debugging, if this is not here, that writer.flush later will cause 401/403 error for REST calls
        writer.write("");
        writer.flush();

        AssistantResponseDTO responseDto = null;
        String exceptionMessage = null;

        try {
            InputDataDTO inputData = new InputDataDTO(data);
            responseDto = aiService.getAiStreamResponse(inputData, statRepo, assistantRepo, writer, request);
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
    public AssistantResponseDTO saveTempFile(@RequestParam("tempFileName") String tempFileName, @RequestParam("imageName") String imageName, @RequestParam("imageLocation") String imageLocation, HttpServletRequest request) throws IOException {
        AssistantResponseDTO response = new AssistantResponseDTO();
        try {
            String result = AiTempFileStorage.saveTempFile(tempFileName, imageName, imageLocation, request);
            response.setResponse(result);
        } catch (Exception e) {
            response.setError(e.getMessage());
        }
        return response;
    }

    @GetMapping(value = "/bonus-content/", produces = "text/plain; charset=UTF-8")
    public String getBonusContent(@RequestParam("assistantId") Long assistantId, HttpServletRequest request, HttpServletResponse response) {
        return aiService.getBonusHtml(assistantId, assistantRepo, request);
    }

    @PostMapping("/new-image-location/")
    public String getNewImageLocation(@RequestParam("docId") Integer docId, @RequestParam("groupId") Integer groupId, @RequestParam("title") String title) {
        return UploadFileTools.getPageUploadSubDir(docId, groupId, title, "/images");
    }

    private String getErrMsg(String errMsg, HttpServletRequest request) {
        return errMsg;
    }

    @GetMapping("/check-name-location/")
    public AssistantResponseDTO checkFileNamesLocationCombo(@RequestParam("fileLocation") String fileLocation, @RequestParam("currentName") String currentName, @RequestParam("generatedName") String generatedName) {
        AssistantResponseDTO response = new AssistantResponseDTO();
        try {
            response.setResponse( aiService.checkExistance(fileLocation, currentName, generatedName) );
        } catch (IOException ex) {
            response.setError( ex.getMessage() );
        }
        return response;
    }

    @PostMapping("/other-button-column/")
    public DataTableColumn getOtherButtonData(@RequestParam String fieldName, @RequestParam String javaClassName, @RequestParam String renderFormat, HttpServletRequest request) {
        //create fake DatatableColumn for passing to service
        DataTableColumn column = new DataTableColumn();
        column.setName(fieldName);
        column.setRenderFormat(renderFormat);

        List<DataTableAi> ai = AiService.getAiAssistantsForField(fieldName, javaClassName, column, sk.iway.iwcm.i18n.Prop.getInstance(request));
        column.setAi(ai); //set also to column for future use

        return column;
    }

    @GetMapping(value = "/stop-assistant/")
    public boolean stopAssistant(@RequestParam("assistantId") Long assistantId, @RequestParam("timestamp") Long timestamp, HttpServletRequest request) {
        return aiTaskRegistry.cancel(assistantId, timestamp, request);
    }
}
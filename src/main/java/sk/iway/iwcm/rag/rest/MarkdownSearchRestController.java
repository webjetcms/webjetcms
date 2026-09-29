package sk.iway.iwcm.rag.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.rag.search.MarkdownSearchService;
import sk.iway.iwcm.rag.search.MarkdownSearchService.SearchResponse;

/** Read-only search API shared by public and authenticated Docsify documentation. */
@RestController
@RequestMapping("/rest/rag/markdown")
public class MarkdownSearchRestController {

    private final MarkdownSearchService searchService;

    public MarkdownSearchRestController(MarkdownSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Searches authorized documentation roots, generates an answer when ragAnswerAllowed is enabled,
     * and disables caching of the response.
     * Login is controlled by ragMarkdownSearchRequireLogin and does not require an administrator role.
     *
     * @param query question or search terms
     * @param language documentation language code
     * @param directory optional viewer directory including its language, such as /sk/admin/; descendants are always included
     * @param request request used for access checks and AI provider calls
     * @param response response whose cache policy is set to no-store
     * @return matching sources and an optional plain-text answer
     * @throws ResponseStatusException if input is invalid or access to the documentation is denied
     */
    @GetMapping("/search")
    public SearchResponse search(@RequestParam("query") String query, @RequestParam("language") String language,
            @RequestParam(value = "directory", required = false) String directory,
            HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        try {
            return searchService.search(query, language, directory, CloudToolsForCore.getDomainId(), request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}

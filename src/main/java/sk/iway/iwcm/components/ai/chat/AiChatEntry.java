package sk.iway.iwcm.components.ai.chat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.ai.tools.AiToolProtocol.Call;
import sk.iway.iwcm.components.ai.tools.AiToolProtocol.Result;

/** Full message text and provider resource ownership for one generation attempt. */
@Getter
@Setter
public class AiChatEntry {
    private String id;
    private String requestId;
    private String turnId;
    private long sortOrder;
    private String role;
    private String text;
    private String inputValue;
    /** Indicates that the input is a selected rich-text fragment rather than a complete page. */
    private boolean inputIsSelectedTextInEditor;
    private String status;
    private Date created;
    private Date updated;
    private int totalTokens;
    private String errorCategory;
    private Integer providerStatus;
    private Boolean retryable;
    /** Outstanding endpoint call and results acknowledged for this saved answer. */
    private Call pendingToolCall;
    private List<Result> toolResults = List.of();
    private boolean toolWriteIssued;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<WriteOutcome> writeOutcomes = new ArrayList<>();
    private String providerResponseId;
    /** Earlier provider rounds for this answer that still need remote deletion. */
    private List<String> earlierProviderResponseIds = new ArrayList<>();
    private boolean storageUnknown;
    private boolean providerDeleted;
    private List<AiChatAttachment> attachments = List.of();

    public record WriteOutcome(String name, int status, boolean uncertain, boolean rejected, List<Long> ids) {
        public static WriteOutcome pending(String name) {
            return new WriteOutcome(name, 0, true, false, List.of());
        }

        public static WriteOutcome from(Result observation) {
            Map<?, ?> body = observation.result() instanceof Map<?, ?> map ? map : Map.of();
            boolean rejected = observation.status() < 200 || observation.status() >= 300 ||
                (body.get("error") instanceof String error && !error.isEmpty()) ||
                (body.get("fieldErrors") instanceof List<?> errors && !errors.isEmpty()) ||
                Boolean.FALSE.equals(body.get("result")) ||
                (body.get("notify") instanceof List<?> notices && notices.stream()
                    .anyMatch(notice -> notice instanceof Map<?, ?> item && "error".equals(item.get("type"))));
            List<?> rows = body.get("data") instanceof List<?> data ? data : List.of(body);
            List<Long> ids = new ArrayList<>();
            for (Object row : rows) {
                if (row instanceof Map<?, ?> record && record.get("id") instanceof Number id && id.longValue() > 0
                        && id.longValue() <= 9_007_199_254_740_991L && id.doubleValue() == id.longValue()) {
                    ids.add(id.longValue());
                }
            }
            boolean uncertain = observation.uncertain() || observation.status() == 0 || observation.status() >= 500;
            return new WriteOutcome(observation.name(), observation.status(), uncertain, rejected, ids);
        }
    }
}

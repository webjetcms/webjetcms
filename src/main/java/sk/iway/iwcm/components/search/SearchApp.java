package sk.iway.iwcm.components.search;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.rag.search.RagService;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@WebjetComponent("sk.iway.iwcm.components.search.SearchApp")
@WebjetAppStore(
    nameKey = "components.search.title",
    descKey = "components.search.desc",
    itemKey = "cmp_search",
    imagePath = "/components/search/editoricon.png",
    galleryImages = "/components/search/",
    componentPath = "/components/search/search.jsp,/components/search/lucene_search.jsp",
    customHtml = "/apps/search/admin/editor-component.html"
)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "basic", selected = true),
    @DataTableTab(id = "semanticSearch", title = "components.semantic_settings.title"),
    @DataTableTab(id = "hybridSearch", title = "components.hybrid_settings.title"),
    @DataTableTab(id = "ragAnswer", title = "components.rag_settings.title"),
})
@Getter
@Setter
public class SearchApp extends WebjetComponentAbstract {

    @JsonIgnore
    private AssistantDefinitionRepository adr;

    @Autowired
    public SearchApp(AssistantDefinitionRepository adr) {
        this.adr = adr;
    }

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.search.searchType",
        tab = "basic"
    )
    private String searchType;

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.search.allow_rag_answer",
        tab = "basic"
    )
    private String answerAllowed;

    @DataTableColumn(
        inputType = DataTableColumnType.JSON,
        title = "components.news.groupids",
        tab = "basic",
        sortAfter = "editorFields.groupDetails",
        className = "dt-tree-group-array"
    )
    private List<GroupDetails> rootGroup;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title="components.search.results_per_page",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "placeholder", value = "components.search.results_per_page"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.search.display_style"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                }
            )
        }
    )
    private int perpage = 10;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.search.check_duplicty",
        tab = "basic"
    )
    private Boolean checkDuplicity;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.formsimple.placeholder",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                message = "components.formsimple.placeholderComment",
                attr = {
                    @DataTableColumnEditorAttr(key = "placeholder", value = "components.search.title")
                }
            )
        }
    )
    private String inputText = "";

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "basic",
        title="components.search.order_by",
        editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.search.relevance", value = "sortPriority"),
                @DataTableColumnEditorAttr(key = "components.search.file_name", value = "title"),
                @DataTableColumnEditorAttr(key = "components.search.file_change", value = "lastUpdate")
            }
        )
    })
    private String orderType = "sortPriority";

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.search.search_type",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.inquiry.orderAsc", value = "asc"),
                    @DataTableColumnEditorAttr(key = "components.inquiry.orderDesc", value = "desc")
                }
            )
        }
    )
    private String order = "asc";

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "editor.paste",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.search.text_field", value = "form"),
                    @DataTableColumnEditorAttr(key = "components.search.results", value = "results"),
                    @DataTableColumnEditorAttr(key = "components.search.search_complete", value = "complete")
                }
            )
        }
    )
    @JsonProperty("sForm")
    private String sForm = "complete";

    /* SEMANTIC SEARCH */

    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        title = "components.semantic_settings.desc",
        tab = "semanticSearch"
    )
    private String semanticSearchDesc;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.semantic_search_min_similarity",
        tab = "semanticSearch",
        renderFormat = "dt-format-number--decimal"
    )
    private BigDecimal semanticSearchMinSimilarity;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.semantic_search_min_results",
        tab = "semanticSearch"
    )
    private Integer semanticSearchMinResults;

    /* HYBRID SEARCH */

    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        title = "components.hybrid_settings.desc",
        tab = "hybridSearch"
    )
    private String hybridSearchDesc;

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.search.hybrid_search_mode",
        tab = "hybridSearch"
    )
    private String hybridSearchMode;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.hybrid_short_query_max_chars",
        tab = "hybridSearch"
    )
    private Integer hybridShortQueryMaxChars;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.hybrid_short_query_max_terms",
        tab = "hybridSearch"
    )
    private Integer hybridShortQueryMaxTerms;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.hybrid_fallback_top_similarity",
        tab = "hybridSearch",
        renderFormat = "dt-format-number--decimal"
    )
    private BigDecimal hybridFallbackTopSimilarity;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.hybrid_vector_weight",
        tab = "hybridSearch",
        renderFormat = "dt-format-number--decimal"
    )
    private BigDecimal hybridVectorWeight;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.hybrid_fts_weight",
        tab = "hybridSearch",
        renderFormat = "dt-format-number--decimal"
    )
    private BigDecimal hybridFtsWeight;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.hybrid_chunk_fetch_multiplier",
        tab = "hybridSearch"
    )
    private Integer hybridChunkFetchMultiplier;

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.search.hybrid_fts_use_ilike_fallback",
        tab = "hybridSearch",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(
                        key = "components.option_auto",
                        value = "auto"),
                    @DataTableColumnEditorAttr(
                        key = "components.search.radio_option.true",
                        value = "trueValue"),
                    @DataTableColumnEditorAttr(
                        key = "components.search.radio_option.false",
                        value = "falseValue")
                }
            )
        }
    )
    private String hybridFtsUseIlikeFallback;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.hybrid_rrf_k",
        tab = "hybridSearch"
    )
    private Integer hybridRrfK;

    /* RAG ANSWER */

    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        title = "components.rag_settings.desc",
        tab = "ragAnswer"
    )
    private String ragAnswerDesc;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.search.rag_assistant",
        tab = "ragAnswer"
    )
    private Integer ragAssistantId;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.rag_answer_min_similarity",
        tab = "ragAnswer",
        renderFormat = "dt-format-number--decimal"
    )
    private BigDecimal ragAnswerMinSimilarity;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.rag_answer_top_k",
        tab = "ragAnswer"
    )
    private Integer ragAnswerTopK;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.rag_answer_max_chunk_gap",
        tab = "ragAnswer"
    )
    private Integer ragAnswerMaxChunkGap;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.rag_answer_max_blocks",
        tab = "ragAnswer"
    )
    private Integer ragAnswerMaxBlocks;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.rag_answer_max_characters",
        tab = "ragAnswer"
    )
    private Integer ragAnswerMaxCharacters;

    @Nullable
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "components.search.rag_answer_max_merged_block_characters",
        tab = "ragAnswer"
    )
    private Integer ragAnswerMaxMergedBlockCharacters;

    @Override
    public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {
        if(Tools.isEmpty(this.searchType)) this.searchType = "auto";
        if(Tools.isEmpty(this.hybridSearchMode)) this.hybridSearchMode = "auto";
        if(Tools.isEmpty(this.answerAllowed)) this.answerAllowed = "auto";
        if(Tools.isEmpty(this.hybridFtsUseIlikeFallback)) this.hybridFtsUseIlikeFallback = "auto";
    }

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Prop prop = Prop.getInstance(request);
        Map<String, List<OptionDto>> options = new HashMap<>();

        boolean isSemanticSearchAllowed = Constants.getBoolean("ragSemanticSearchEnabled");
        boolean isHybridSearchAllowed = Constants.getBoolean("ragHybridSearchEnabled");
        boolean isRagAnswerAlloowed = Constants.getBoolean("ragAnswerAllowed");

        options.put("searchType", getSupportedSearchTypes(isSemanticSearchAllowed, isHybridSearchAllowed, prop));
        options.put("hybridSearchMode", getHybridSearchModeOptions(prop));
        options.put("answerAllowed", getRagAnswerAllowedOptions(isSemanticSearchAllowed, isRagAnswerAlloowed, prop));
        options.put("ragAssistantId", getRagAssistantsOptions(prop));

        options.put("allowedOptions", getAllowedOptions(isSemanticSearchAllowed, (isSemanticSearchAllowed && isHybridSearchAllowed), (isSemanticSearchAllowed && isRagAnswerAlloowed)));

        return options;
    }

    private List<OptionDto> getSupportedSearchTypes(boolean isSemanticSearchAllowed, boolean isHybridSearchAllowed, Prop prop) {
        List<OptionDto> searchTypes = new ArrayList<>();

        searchTypes.add( new OptionDto(prop.getText("components.search.searchType.auto"), "auto", null) );
        searchTypes.add( new OptionDto(prop.getText("components.search.searchType.db"), "db", null) );
        searchTypes.add( new OptionDto(prop.getText("components.search.searchType.lucene"), "lucene", null) );

        String notAllowed = " (" + prop.getText("components.search.not_allowed") + ")";
        searchTypes.add( new OptionDto(prop.getText("components.search.searchType.semantic") + (isSemanticSearchAllowed ? "" : notAllowed), "semantic", null) );
        searchTypes.add( new OptionDto(prop.getText("components.search.searchType.hybrid") + (isSemanticSearchAllowed && isHybridSearchAllowed ? "" : notAllowed), "hybrid", null) );

        return searchTypes;
    }

    private List<OptionDto> getHybridSearchModeOptions(Prop prop) {
        List<OptionDto> searchModes = new ArrayList<>();

        List<String> hybridSearchModeOptions = List.of("auto", "off", "always", "short_query_only", "fallback_on_low_vector");
        for(String hybridSearchModeOption : hybridSearchModeOptions) {
            searchModes.add( new OptionDto(prop.getText("components.search.hybrid_search_mode." + hybridSearchModeOption), hybridSearchModeOption, null) );
        }

        return searchModes;
    }

    private List<OptionDto> getRagAssistantsOptions(Prop prop) {
        List<OptionDto> assistantsList = new ArrayList<>();

        assistantsList.add( new OptionDto(prop.getText("components.search.rag_assistant.default"), "-1", null) );

        for(AssistantDefinitionEntity assistant : adr.findAllByClassNameAndDomainId(RagService.class.getName(), CloudToolsForCore.getDomainId())) {
            StringBuilder sb = new StringBuilder();
            sb.append( prop.getText(assistant.getDescription()) ).append(" (").append(assistant.getName()).append(" - ").append(assistant.getProvider()).append(")");

            assistantsList.add(
                new OptionDto(
                    sb.toString(),
                    String.valueOf(assistant.getId()),
                    null
                )
            );
        }

        return assistantsList;
    }

    private List<OptionDto> getRagAnswerAllowedOptions(boolean isSemanticSearchAllowed, boolean isRagAnswerAlloowed, Prop prop) {
        List<OptionDto> options = new ArrayList<>();

        String notAllowed = " (" + prop.getText("components.search.not_allowed") + ")";
        options.add( new OptionDto(prop.getText("components.option_auto"), "auto", null) );
        options.add( new OptionDto(prop.getText("components.search.radio_option.true") + (isSemanticSearchAllowed && isRagAnswerAlloowed ? "" : notAllowed), "trueValue", null) );
        options.add( new OptionDto(prop.getText("components.search.radio_option.false"), "falseValue", null) );

        return options;
    }

    private List<OptionDto> getAllowedOptions(boolean isSemanticSearchAllowed, boolean isHybridSearchAllowed, boolean isRagAnswerAlloowed) {
        List<OptionDto> options = new ArrayList<>();

        options.add( new OptionDto("semanticSearch", String.valueOf(isSemanticSearchAllowed), null) );
        options.add( new OptionDto("hybridSearch", String.valueOf(isHybridSearchAllowed), null) );
        options.add( new OptionDto("ragAnswer", String.valueOf(isRagAnswerAlloowed), null) );

        return options;
    }
}
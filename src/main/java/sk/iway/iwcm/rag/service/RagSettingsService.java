package sk.iway.iwcm.rag.service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;

/**
 * Resolves RAG settings from global constants with optional per-component
 * overrides stored in {@link PageParams}. Empty or "auto" values keep the
 * global configuration so existing search apps keep their default behavior.
 */
public class RagSettingsService {

    private RagSettingsService() {}

    /* RAG - SEMANTIC SEARCH */

    public static double getSemanticMinimumSimilarity(PageParams pageParams) {
        double minimumSimilarity = Tools.getDoubleValue(Constants.getString("ragSemanticSearchMinSimilarity"), 0);
        if(pageParams != null) {
            minimumSimilarity = Tools.getDoubleValue(pageParams.getValue("semanticSearchMinSimilarity", ""), minimumSimilarity);
        }
        return minimumSimilarity;
    }

    public static int getSemanticMinimumResults(PageParams pageParams) {
        int minimumResults = Constants.getInt("ragSemanticSearchMinResults");
        if(pageParams != null) {
            minimumResults = Tools.getIntValue(pageParams.getValue("semanticSearchMinResults", ""), minimumResults);
        }
        return minimumResults;
    }

    /**
     * Resolves the text-match weight, clamping finite values and replacing invalid values with the default.
     *
     * @return weight in the range zero to one, or 0.15 for invalid or non-finite input
     */
    public static double getRerankLexicalWeight() {
        double weight = Tools.getDoubleValue(Constants.getString("ragRerankLexicalWeight"), 0.15d);
        return Double.isFinite(weight) ? Math.max(0d, Math.min(1d, weight)) : 0.15d;
    }

    /* RAG - HYBRID SEARCH */

    /**
     * Resolves the hybrid retrieval mode, using global configuration for empty or automatic component values.
     *
     * @param pageParams optional component settings
     * @return trimmed mode, or an empty string when no value is available
     */
    public static String getHybridSearchMode(PageParams pageParams) {
        String hybridSearchMode = pageParams == null ? null : pageParams.getValue("hybridSearchMode", "");
        if(Tools.isEmpty(hybridSearchMode) || "auto".equalsIgnoreCase(hybridSearchMode)) {
            hybridSearchMode = Constants.getString("ragHybridSearchMode");
        }
        return Tools.getStringValue(hybridSearchMode, "").trim();
    }

    public static int getHybridShortQueryMaxChars(PageParams pageParams) {
        int hybridShortQueryMaxChars = Constants.getInt("ragHybridShortQueryMaxChars");
        if(pageParams != null) {
            hybridShortQueryMaxChars = Tools.getIntValue(pageParams.getValue("hybridShortQueryMaxChars", ""), hybridShortQueryMaxChars);
        }
        return hybridShortQueryMaxChars;
    }

    public static int getHybridShortQueryMaxTerms(PageParams pageParams) {
        int hybridShortQueryMaxTerms = Constants.getInt("ragHybridShortQueryMaxTerms");
        if(pageParams != null) {
            hybridShortQueryMaxTerms = Tools.getIntValue(pageParams.getValue("hybridShortQueryMaxTerms", ""), hybridShortQueryMaxTerms);
        }
        return hybridShortQueryMaxTerms;
    }

    public static double getHybridFallbackTopSimilarity(PageParams pageParams) {
        double hybridFallbackTopSimilarity = Tools.getDoubleValue(Constants.getString("ragHybridFallbackTopSimilarity"), 0);
        if(pageParams != null) {
            hybridFallbackTopSimilarity = Tools.getDoubleValue(pageParams.getValue("hybridFallbackTopSimilarity", ""), hybridFallbackTopSimilarity);
        }
        return hybridFallbackTopSimilarity;
    }

    public static double getHybridVectorWeight(PageParams pageParams) {
        double hybridVectorWeight = Tools.getDoubleValue(Constants.getString("ragHybridVectorWeight"), 0);
        if(pageParams != null) {
            hybridVectorWeight = Tools.getDoubleValue(pageParams.getValue("hybridVectorWeight", ""), hybridVectorWeight);
        }
        return hybridVectorWeight;
    }

    public static double getHybridFtsWeight(PageParams pageParams) {
        double hybridFtsWeight = Tools.getDoubleValue(Constants.getString("ragHybridFtsWeight"), 0);
        if(pageParams != null) {
            hybridFtsWeight = Tools.getDoubleValue(pageParams.getValue("hybridFtsWeight", ""), hybridFtsWeight);
        }
        return hybridFtsWeight;
    }

    public static int getHybridChunkFetchMultiplier(PageParams pageParams) {
        int hybridChunkFetchMultiplier = Constants.getInt("ragHybridChunkFetchMultiplier");
        if(pageParams != null) {
            hybridChunkFetchMultiplier = Tools.getIntValue(pageParams.getValue("hybridChunkFetchMultiplier", ""), hybridChunkFetchMultiplier);
        }
        return hybridChunkFetchMultiplier;
    }

    /**
     * Resolves whether full-text retrieval may fall back to a database-specific text-pattern search.
     * Recognizes component values {@code true}, {@code trueValue}, {@code false}, and {@code falseValue};
     * other values retain the global setting.
     *
     * @param pageParams optional component settings
     * @return whether the full-text fallback is enabled
     */
    public static boolean getHybridFtsUseIlikeFallback(PageParams pageParams) {
        String value = pageParams == null ? null : pageParams.getValue("hybridFtsUseIlikeFallback", "");
        if("true".equalsIgnoreCase(value) || "trueValue".equalsIgnoreCase(value)) return true;
        if("false".equalsIgnoreCase(value) || "falseValue".equalsIgnoreCase(value)) return false;
        return Constants.getBoolean("ragHybridFtsUseIlikeFallback");
    }

    public static int getHybridRrfK(PageParams pageParams) {
        int hybridRrfK = Constants.getInt("ragHybridRrfK");
        if(pageParams != null) {
            hybridRrfK = Tools.getIntValue(pageParams.getValue("hybridRrfK", ""), hybridRrfK);
        }
        return hybridRrfK;
    }

    /* RAG - ANSWER */

    public static double getRagAnswerMinSimilarity(PageParams pageParams) {
        double ragAnswerMinSimilarity = Tools.getDoubleValue(Constants.getString("ragAnswerMinSimilarity"), 0);
        if(pageParams != null) {
            ragAnswerMinSimilarity = Tools.getDoubleValue(pageParams.getValue("ragAnswerMinSimilarity", ""), ragAnswerMinSimilarity);
        }
        return ragAnswerMinSimilarity;
    }

    public static int getRagAnswerTopK(PageParams pageParams) {
        int ragAnswerTopK = Constants.getInt("ragAnswerTopK");
        if(pageParams != null) {
            ragAnswerTopK = Tools.getIntValue(pageParams.getValue("ragAnswerTopK", ""), ragAnswerTopK);
        }
        return ragAnswerTopK;
    }

    public static int getRagAnswerMaxChunkGap(PageParams pageParams) {
        int ragAnswerMaxChunkGap = Constants.getInt("ragAnswerMaxChunkGap", 1);
        if(pageParams != null) {
            ragAnswerMaxChunkGap = Tools.getIntValue(pageParams.getValue("ragAnswerMaxChunkGap", ""), ragAnswerMaxChunkGap);
        }
        return ragAnswerMaxChunkGap;
    }

    public static int getRagAnswerMaxBlocks(PageParams pageParams) {
        int ragAnswerMaxBlocks = Constants.getInt("ragAnswerMaxBlocks");
        if(pageParams != null) {
            ragAnswerMaxBlocks = Tools.getIntValue(pageParams.getValue("ragAnswerMaxBlocks", ""), ragAnswerMaxBlocks);
        }
        return ragAnswerMaxBlocks;
    }

    public static int getRagAnswerMaxCharacters(PageParams pageParams) {
        int ragAnswerMaxCharacters = Constants.getInt("ragAnswerMaxCharacters");
        if(pageParams != null) {
            ragAnswerMaxCharacters = Tools.getIntValue(pageParams.getValue("ragAnswerMaxCharacters", ""), ragAnswerMaxCharacters);
        }
        return ragAnswerMaxCharacters;
    }

    public static int getRagAnswerMaxMergedBlockCharacters(PageParams pageParams) {
        int ragAnswerMaxMergedBlockCharacters = Constants.getInt("ragAnswerMaxMergedBlockCharacters");
        if(pageParams != null) {
            ragAnswerMaxMergedBlockCharacters = Tools.getIntValue(pageParams.getValue("ragAnswerMaxMergedBlockCharacters", ""), ragAnswerMaxMergedBlockCharacters);
        }
        return ragAnswerMaxMergedBlockCharacters;
    }

    /**
     * Combines the global hybrid-search switch with the component's retrieval mode and search type.
     * An {@code off} mode disables hybrid retrieval; empty or automatic search types retain the global switch.
     *
     * @param pageParams optional component settings
     * @return {@code true} when hybrid search is globally enabled and the component selection permits it
     */
    public static boolean isHybridSearchEnabled(PageParams pageParams) {
        if(Constants.getBoolean("ragHybridSearchEnabled") == false || "off".equalsIgnoreCase(getHybridSearchMode(pageParams))) {
            return false;
        }
        String searchType = pageParams == null ? "" : Tools.getStringValue(pageParams.getValue("searchType", ""), "").trim();
        return Tools.isEmpty(searchType) || "auto".equalsIgnoreCase(searchType) || "hybrid".equalsIgnoreCase(searchType);
    }

    /**
     * Applies the global answer-generation gate and the component's optional answer preference.
     * Empty or automatic preferences retain the global setting; unrecognized preferences disable answers.
     *
     * @param pageParams optional component settings
     * @return {@code true} when answer generation is enabled globally and allowed by the component
     */
    public static boolean isAnswerAllowed(PageParams pageParams) {
        if(Constants.getBoolean("ragAnswerAllowed") == false) return false;
        String answerAllowed = pageParams == null ? "" : pageParams.getValue("answerAllowed", "");
        return Tools.isEmpty(answerAllowed) || "auto".equalsIgnoreCase(answerAllowed)
            || "true".equalsIgnoreCase(answerAllowed) || "trueValue".equalsIgnoreCase(answerAllowed);
    }
}

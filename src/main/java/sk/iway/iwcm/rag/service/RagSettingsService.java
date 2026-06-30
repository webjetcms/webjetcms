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

    /* RAG - HYBRID SEARCH */

    public static String getHybridSearchMode(PageParams pageParams) {
        String hybridSearchMode = null;
        if(pageParams != null) {
            hybridSearchMode = pageParams.getValue("hybridSearchMode", "");
        }
        String mode;

        if(Tools.isEmpty(hybridSearchMode) || "auto".equalsIgnoreCase(hybridSearchMode)) {
            // use value from settings - auto mode
            mode = Constants.getString("ragHybridSearchMode");
        } else {
            // user specified mode
            mode = hybridSearchMode;
        }

        return Tools.getStringValue(mode, "").trim();
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

    public static boolean getHybridFtsUseIlikeFallback(PageParams pageParams) {
        boolean hybridFtsUseIlikeFallback = Constants.getBoolean("ragHybridFtsUseIlikeFallback");

        if(pageParams != null) {
            String val = pageParams.getValue("hybridFtsUseIlikeFallback", "");
            if(Tools.isNotEmpty(val)) {
                if("true".equalsIgnoreCase(val) || "trueValue".equalsIgnoreCase(val)) {
                    hybridFtsUseIlikeFallback = true;
                } else if("false".equalsIgnoreCase(val) || "falseValue".equalsIgnoreCase(val)) {
                    hybridFtsUseIlikeFallback = false;
                }
            }
        }

        return hybridFtsUseIlikeFallback;
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

    public static boolean isHybridSearchEnabled(PageParams pageParams) {
        if(Constants.getBoolean("ragHybridSearchEnabled") == false) {
            // Does not matter what user selected, if hybrid search is disabled in settings, it should not be allowed
            return false;
        }

        if("off".equalsIgnoreCase(getHybridSearchMode(pageParams))) {
            return false;
        }

        if(pageParams == null) {
            return true;
        }

        String searchType = Tools.getStringValue(pageParams.getValue("searchType", ""), "").trim();
        if(Tools.isEmpty(searchType) || "auto".equalsIgnoreCase(searchType)) {
            // auto mode
            return true;
        }

        // User specified search type
        return "hybrid".equalsIgnoreCase(searchType);
    }

    public static boolean isAnswerAllowed(PageParams pageParams) {
        if(Constants.getBoolean("ragAnswerAllowed") == false) {
            // Does not matter what user selected, if RAG answer is disabled in settings, it should not be allowed
            return false;
        }

        if(pageParams == null) {
            return true;
        }

        String answerAllowed = pageParams.getValue("answerAllowed", "");
        if(Tools.isEmpty(answerAllowed) || "auto".equalsIgnoreCase(answerAllowed)) {
            // auto mode
            return true;
        } else if("true".equalsIgnoreCase(answerAllowed) || "trueValue".equalsIgnoreCase(answerAllowed)) {
            return true;
        } else if("false".equalsIgnoreCase(answerAllowed) || "falseValue".equalsIgnoreCase(answerAllowed)) {
            return false;
        }

        return false;
    }
}

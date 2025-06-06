package sk.iway.iwcm.system.translation;

public abstract class TranslationEngine {
    public abstract String engineName();
    public abstract Long numberOfFreeCharacters();
    public abstract String translate(String text, String fromLanguage, String toLanguage);
    public abstract boolean isConfigured();
}
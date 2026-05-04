package sk.iway.iwcm.tags;

public enum ScriptTypeEnum {
    JAVASCRIPT("javascript-source-to-flush"),
    CSS_STYLE("css-source-to-flush"),
    UNKNOWN("unknown-source-to-flush");

    private String toString;

    private ScriptTypeEnum(String toString)
    {
        this.toString = toString;
    }

    public String getToString() {
        return toString;
    }

    public void setToString(String toString) {
        this.toString = toString;
    }

}

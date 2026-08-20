package sk.iway.iwcm.system.spring;

public enum WebjetBootstrapMode {

    SETUP("setup"),
    PRODUCTION("production");

    public static final String PROPERTY_NAME = "webjet.bootstrap.mode";
    public static final String SETUP_VALUE = "setup";
    public static final String PRODUCTION_VALUE = "production";

    private final String propertyValue;

    WebjetBootstrapMode(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    String getPropertyValue() {
        return propertyValue;
    }
}

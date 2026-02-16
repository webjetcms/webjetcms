package sk.iway.iwcm;

public class ResponseHeaderBean {
    private String url;
    private String name;
    private String value;

    public ResponseHeaderBean(String headerConfLine) {
        String[] line = Tools.getTokens(headerConfLine, ":");

        if(line != null && line.length == 3) {
            setUrl(line[0]);
            setName(line[1]);
            setValue(line[2]);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

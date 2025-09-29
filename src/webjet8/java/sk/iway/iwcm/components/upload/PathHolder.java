package sk.iway.iwcm.components.upload;

public class PathHolder {
    private String fileName;
    private String tempPath;
    private long timestamp;

    public PathHolder(String fileName, String tempPath, long timestamp)
    {
        this.fileName = fileName;
        this.timestamp = timestamp;
        this.tempPath = tempPath;
    }
    public String getFileName()
    {
        return fileName;
    }
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
    public long getTimestamp()
    {
        return timestamp;
    }
    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }
    public String getTempPath()
    {
        return tempPath;
    }
    public void setTempPath(String tempPath)
    {
        this.tempPath = tempPath;
    }
}

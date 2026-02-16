package sk.iway.iwcm;


import java.sql.Timestamp;

public interface AdminlogCustomLogger
{
    public void addLog(int logType, RequestBean rb, String message, Timestamp localDateTime);
}

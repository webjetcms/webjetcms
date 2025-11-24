package sk.iway.iwcm.components.ai.rest;

public class AiTempFileRemover {

    private static final Integer REMOVE_OLDER_THAN_HOURS = 24;

    public static void main(String[] args)
	{
        try {
            AiTempFileStorage.removeOldTempFiles(REMOVE_OLDER_THAN_HOURS);
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

}

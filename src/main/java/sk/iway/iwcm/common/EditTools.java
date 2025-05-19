package sk.iway.iwcm.common;

public class EditTools {
    public static boolean parseLine(String line)
    {
        if (line == null)
        {
            return (true);
        }
        byte[] poleB = line.getBytes();
        for (int i = 0; i < poleB.length; i++)
        {
            if (poleB[i] > 0 && poleB[i] < 8)
            {
                return (false);
            }
            // result = result && (poleB[i] > 8);
        }
        return true;
    }
}

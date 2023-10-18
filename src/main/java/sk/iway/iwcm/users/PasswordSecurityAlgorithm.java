package sk.iway.iwcm.users;

public interface PasswordSecurityAlgorithm {
    
    public String generateSalt();

    public  String calculateHash(String password, String salt);

    public boolean isPasswordCorrect (String password, String salt, String hash);
}

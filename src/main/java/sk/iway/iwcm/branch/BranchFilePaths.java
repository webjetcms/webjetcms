package sk.iway.iwcm.branch;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.HexFormat;

/** Logical file identity under the two supported editorial roots. Physical paths are server-owned. */
public final class BranchFilePaths {
    private BranchFilePaths() { }

    public static String normalize(String input) {
        if (input==null || !input.startsWith("/") || input.startsWith("//") || input.length()>2048
                || input.indexOf('\\')>=0 || input.indexOf('%')>=0 || input.indexOf('?')>=0 || input.indexOf('#')>=0
                || input.chars().anyMatch(c->c<32 || c==127))throw new IllegalArgumentException("Invalid logical file path");
        ArrayDeque<String> parts=new ArrayDeque<>();
        for (String part:input.split("/")) {
            if (part.isEmpty() || part.equals("."))continue;
            if (part.equals("..")) {
                if (parts.size()<=1)throw new IllegalArgumentException("Path escapes its editorial root");
                parts.removeLast();
            } else parts.addLast(part);
        }
        if (parts.size()<2 || !(parts.getFirst().equals("images") || parts.getFirst().equals("files")))
            throw new IllegalArgumentException("Only entries below /images or /files are supported");
        return "/"+String.join("/",parts);
    }

    public static String key(String normalized) {
        if (!normalize(normalized).equals(normalized))throw new IllegalArgumentException("Path must be normalized");
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
}

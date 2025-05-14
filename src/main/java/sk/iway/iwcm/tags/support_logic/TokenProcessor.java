package sk.iway.iwcm.tags.support_logic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class TokenProcessor {

    private long previous;

    private static TokenProcessor instance = new TokenProcessor();

    public static final String TRANSACTION_TOKEN_KEY = "org.apache.struts.action.TOKEN";
    public static final String TOKEN_KEY = "org.apache.struts.taglib.html.TOKEN";

    protected TokenProcessor() {
        super();
    }

    public static TokenProcessor getInstance() {
        return instance;
    }

    public synchronized void saveToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = generateToken(request);

        if (token != null) {
            session.setAttribute(TRANSACTION_TOKEN_KEY, token);
        }
    }

    public synchronized String generateToken(HttpServletRequest request) {
        HttpSession session = request.getSession();

        return generateToken(session.getId());
    }

    public synchronized String generateToken(String id) {
        try {
            long current = System.currentTimeMillis();

            if (current == previous) {
                current++;
            }

            previous = current;

            byte[] now = Long.toString(current).getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");

            md.update(id.getBytes());
            md.update(now);

            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String toHex(byte[] buffer) {
        StringBuffer sb = new StringBuffer(buffer.length * 2);

        for (int i = 0; i < buffer.length; i++) {
            sb.append(Character.forDigit((buffer[i] & 0xf0) >> 4, 16));
            sb.append(Character.forDigit(buffer[i] & 0x0f, 16));
        }

        return sb.toString();
    }

    public synchronized boolean isTokenValid(HttpServletRequest request) {
        return this.isTokenValid(request, false);
    }

    public synchronized boolean isTokenValid(HttpServletRequest request,
        boolean reset) {
        // Retrieve the current session for this request
        HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        // Retrieve the transaction token from this session, and
        // reset it if requested
        String saved =
            (String) session.getAttribute(TRANSACTION_TOKEN_KEY);

        if (saved == null) {
            return false;
        }

        if (reset) {
            this.resetToken(request);
        }

        // Retrieve the transaction token included in this request
        String token = request.getParameter(TOKEN_KEY);

        if (token == null) {
            return false;
        }

        return saved.equals(token);
    }

    public synchronized void resetToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        session.removeAttribute(TRANSACTION_TOKEN_KEY);
    }
}

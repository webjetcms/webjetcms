package sk.iway.iwcm.components.ai.tools;

import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A named skill reference usable in plain text and inside a browser provider's JSON string. */
public final class AiSkillLink {
    private static final Pattern RESERVED = Pattern.compile("[%()\"\\\\{}<>\\p{Cntrl}]");
    private static final Pattern ESCAPE = Pattern.compile("%([0-9a-fA-F]{2})");

    private AiSkillLink() {}

    public static String token(String name) {
        return "@skill(" + encode(name) + ")";
    }

    private static String encode(String name) {
        return RESERVED.matcher(name).replaceAll(match -> "%" + HexFormat.of().withUpperCase().toHexDigits((byte) match.group().charAt(0)));
    }

    static String decode(String name) {
        return ESCAPE.matcher(name).replaceAll(match -> Matcher.quoteReplacement(
                Character.toString(Integer.parseInt(match.group(1), 16))));
    }
}

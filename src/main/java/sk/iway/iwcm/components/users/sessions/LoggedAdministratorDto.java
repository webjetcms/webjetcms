package sk.iway.iwcm.components.users.sessions;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** Public administrator summary; session identifiers and account credentials are never exposed. */
@Getter
@Setter
public class LoggedAdministratorDto {
    private int userId;
    private String fullName;
    private String email;
    private String login;
    private boolean current;
    private int sessionCount;
    private long lastActivity;
    private List<String> clients;
}

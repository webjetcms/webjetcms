package sk.iway.iwcm.users;

import lombok.Getter;
import lombok.Setter;

/**
 * Basic DTO object for userId to name mapping
 */
@Getter
@Setter
public class UserBasicDto {
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String photo;
    private int userId;
    private String login;
}

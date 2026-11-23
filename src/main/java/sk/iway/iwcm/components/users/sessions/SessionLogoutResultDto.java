package sk.iway.iwcm.components.users.sessions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Reports whether logout was accepted and whether another cluster node must still process it. */
@Getter
@AllArgsConstructor
public class SessionLogoutResultDto {
    private final boolean success;
    private final boolean pending;
}

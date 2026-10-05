package sk.iway.iwcm.users.devices;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Identifies one browser token within an account and domain. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DeviceId implements Serializable {
    private static final long serialVersionUID = 1L;

    private int domainId;
    private int userId;
    private String tokenHash;
}

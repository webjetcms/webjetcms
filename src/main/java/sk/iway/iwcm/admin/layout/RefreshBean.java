package sk.iway.iwcm.admin.layout;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Bean pre prenos refresher dat
 */
@Getter
@Setter
public class RefreshBean {
    long timestamp;
    List<Integer> messages;
}

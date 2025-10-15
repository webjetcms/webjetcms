package sk.iway.iwcm.components.ai.stat.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DaysUsageDTO {
    public DaysUsageDTO(Date dayDate, Integer usage, Integer tokens) {
        this.dayDate = dayDate;
        this.usage = usage;
        this.tokens = tokens;
    }

    Date dayDate;
    Integer usage;
    Integer tokens;
}

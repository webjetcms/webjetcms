package sk.iway.iwcm.components.ai.stat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
@NoArgsConstructor
public class TokenUsersDTO {

    public TokenUsersDTO(String userName, Integer usedTokens) {
        this.userName = userName;
        this.usedTokens = usedTokens;
    }

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.inquiry.inquiry_statistics.user_name")
    private String userName;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.ai_assistants.stats.used_tokens")
    private Integer usedTokens;
}
package sk.iway.iwcm.components.seo.jpa;


import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class NumberKeywordsDTO {

    public NumberKeywordsDTO() {}

    public NumberKeywordsDTO(Integer order, String name, Integer numberOfPages, Integer numberOfKeywords, Integer numberOfKeywordsSubstring) {
        this.order = order;
        this.name = name;
        this.numberOfPages = numberOfPages;
        this.numberOfKeywords = numberOfKeywords;
        this.numberOfKeywordsSubstring = numberOfKeywordsSubstring;
    }
    
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_top.order"
    )
	private Integer order;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.seo.keywords.name",
        renderFormatLinkTemplate = "javascript:getSearchAll({{order}});"
    )
	private String name;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.seo.keywords.number.pages"
    )
	private Integer numberOfPages;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.seo.keywords.number.keywords"
    )
	private Integer numberOfKeywords;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.seo.keywords.number.keywords_substring"
    )
	private Integer numberOfKeywordsSubstring;
}

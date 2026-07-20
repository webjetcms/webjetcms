package sk.iway.iwcm.components.redirects;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 * Serializable read-only DataTable row describing one operation in a redirect
 * clearing preview. A database redirect can occur at most once in a plan.
 */
@Getter
@Setter
public class RedirectClearingAction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Operations produced by redirect clearing analysis inside one domain
     * scope. Regular-expression redirects and redirects with a publication or
     * validity end date are excluded from analysis and never receive an action.
     */
    public enum ActionType {
        /**
         * Removes an obsolete target superseded by the newest target in the same
         * domain. For example, two unconditional redirects in {@code example.com},
         * {@code /a -> /b} followed by {@code /a -> /c}, result in deletion of
         * {@code /a -> /b}. A redirect without a domain does not compete with
         * either record, and a dated redirect is excluded from the comparison.
         */
        DELETE_OLD,

        /**
         * Removes the newest redirect step responsible for a cycle inside one
         * domain. For example, same-domain redirects {@code /a -> /b} and
         * {@code /b -> /a} form a cycle, while the same URLs split between a
         * named and unnamed domain do not.
         */
        DELETE_CYCLE,

        /**
         * Removes a duplicate while preserving the oldest record in the same
         * domain. For example, duplicate {@code /a -> /b} records in
         * {@code example.com} are reduced to the oldest one, but an equal redirect
         * without a domain is preserved as a separate record.
         */
        DELETE_DUPLICATE,

        /**
         * Shortens a redirect chain by updating only its target URL. For example,
         * same-domain {@code /a -> /b -> /c} is optimized to {@code /a -> /c}.
         * If {@code /a -> /b} belongs to a named domain and {@code /b -> /c} has
         * no domain, neither redirect is changed because chains never cross domains.
         */
        UPDATE_OPTIMIZE
    }

    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.redirect.clearing.action",
        hiddenEditor = true,
        sortAfter = "id"
    )
    private ActionType action;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.redirect.admin_list.stare_url",
        hiddenEditor = true,
        sortAfter = "action"
    )
    private String oldUrl;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.redirect.clearing.currentNewUrl",
        hiddenEditor = true,
        sortAfter = "oldUrl"
    )
    private String currentNewUrl;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.redirect.clearing.proposedNewUrl",
        hiddenEditor = true,
        sortAfter = "currentNewUrl"
    )
    private String proposedNewUrl;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "groupedit.domain",
        hiddenEditor = true,
        sortAfter = "proposedNewUrl"
    )
    private String domainName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT_NUMBER,
        title = "components.redirect.admin_list.presmerovaci_kod",
        hiddenEditor = true,
        sortAfter = "domainName"
    )
    private Integer redirectCode;

    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "components.banner.dateFrom",
        hiddenEditor = true,
        sortAfter = "redirectCode"
    )
    private Date publishDate;

    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "components.banner.dateTo",
        hiddenEditor = true,
        sortAfter = "publishDate"
    )
    private Date validTo;

    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "components.redirect.admin_list.datum_vlozenia",
        hiddenEditor = true,
        sortAfter = "validTo"
    )
    private Date insertDate;

    /**
     * Creates an empty action required by DataTable and cached snapshot deserialization.
     */
    public RedirectClearingAction() {
        // Required for deserialization.
    }

    /**
     * Creates a complete redirect clearing action snapshot.
     *
     * @param id database redirect identifier
     * @param action operation to execute
     * @param oldUrl source URL
     * @param currentNewUrl target URL at analysis time
     * @param proposedNewUrl optimized target URL, or {@code null} for delete actions
     * @param domainName original redirect domain
     * @param redirectCode HTTP redirect status code
     * @param publishDate validity interval start
     * @param validTo validity interval end
     * @param insertDate redirect creation timestamp
     */
    public RedirectClearingAction(
            Long id,
            ActionType action,
            String oldUrl,
            String currentNewUrl,
            String proposedNewUrl,
            String domainName,
            Integer redirectCode,
            Date publishDate,
            Date validTo,
            Date insertDate) {
        this.id = id;
        this.action = action;
        this.oldUrl = oldUrl;
        this.currentNewUrl = currentNewUrl;
        this.proposedNewUrl = proposedNewUrl;
        this.domainName = domainName;
        this.redirectCode = redirectCode;
        this.publishDate = publishDate;
        this.validTo = validTo;
        this.insertDate = insertDate;
    }

    /**
     * Indicates whether this action removes the redirect instead of updating its target URL.
     *
     * @return {@code true} for all delete actions, {@code false} for
     *         {@link ActionType#UPDATE_OPTIMIZE}
     */
    public boolean isDelete() {
        return action != ActionType.UPDATE_OPTIMIZE;
    }
}

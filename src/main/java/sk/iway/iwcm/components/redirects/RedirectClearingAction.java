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
     * Operations produced by redirect clearing analysis. Global redirects take
     * precedence over domain-specific redirects for every operation.
     */
    public enum ActionType {
        /**
         * Removes a redirect target that loses precedence to the selected target.
         * For example, if global {@code /a -> /b} and newer domain-specific
         * {@code /a -> /c} records exist, the domain-specific record is removed.
         * Within the same scope, the newest target takes precedence.
         */
        DELETE_OLD,

        /**
         * Removes a redirect step responsible for a cycle while protecting global redirects.
         * For example, in a mixed global/local cycle {@code /a -> /b -> /a},
         * the newest local step is removed. The newest global step is removed only
         * when the cycle contains no local step.
         */
        DELETE_CYCLE,

        /**
         * Removes a duplicate while preferring a global redirect over a domain-specific one.
         * For example, if global and domain-specific {@code /a -> /b} records exist,
         * the domain-specific record is removed. Among records with the same scope,
         * the oldest record is preserved.
         */
        DELETE_DUPLICATE,

        /**
         * Shortens a redirect chain by updating only its target URL.
         * For example, local {@code /a -> /b} followed by global {@code /b -> /c}
         * is optimized to local {@code /a -> /c}. Global {@code /a -> /b} followed
         * by local {@code /b -> /c} remains unchanged because a local redirect must
         * not influence a global redirect.
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
     * Creates an empty action required by DataTable and session deserialization.
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

package sk.iway.iwcm.components.redirects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.redirects.RedirectCleaningAction.ActionType;
import sk.iway.iwcm.components.redirects.RedirectCleaningPlanCoordinator.MissingPlanException;
import sk.iway.iwcm.components.redirects.RedirectCleaningPlanCoordinator.OperationInProgressException;
import sk.iway.iwcm.components.redirects.RedirectCleaningPlanCoordinator.OperationType;
import sk.iway.iwcm.components.redirects.RedirectCleaningService.ExecutionResult;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Read-only DataTable controller for redirect cleaning previews.
 * <p>
 * Analysis stores one immutable plan in the application cache for the current
 * domain ID. All authorized administrators in that domain share the preview,
 * while paging, filtering, and sorting operate on the cached snapshot.
 * Execution applies the complete snapshot regardless of the visible page or
 * selected rows.
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/redirect-cleaning")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
public class RedirectCleaningRestController extends DatatableRestControllerV2<RedirectCleaningAction, Long> {

    private final RedirectCleaningPlanCoordinator coordinator;

    @Autowired
    public RedirectCleaningRestController(RedirectCleaningPlanCoordinator coordinator) {
        super(null);
        this.coordinator = coordinator;
    }

    @Override
    public Page<RedirectCleaningAction> getAllItems(Pageable pageable) {
        RedirectCleaningPlan plan = coordinator.getPlan( CloudToolsForCore.getDomainId() );
        List<RedirectCleaningAction> actions = plan == null ? List.of() : plan.getActions();
        return createPage(actions, pageable, Map.of(), plan);
    }

    @Override
    public Page<RedirectCleaningAction> searchItem(Map<String, String> params, Pageable pageable, RedirectCleaningAction search) {
        RedirectCleaningPlan plan = coordinator.getPlan( CloudToolsForCore.getDomainId() );
        List<RedirectCleaningAction> actions = plan == null ? List.of() : plan.getActions();
        return createPage(actions, pageable, params, plan);
    }

    @Override
    public void getOptions(DatatablePageImpl<RedirectCleaningAction> page) {
        List<LabelValue> options = List.of(
            new LabelValue(getProp().getText("components.redirect.cleaning.action.deleteOld"), ActionType.DELETE_OLD.name()),
            new LabelValue(getProp().getText("components.redirect.cleaning.action.deleteCycle"), ActionType.DELETE_CYCLE.name()),
            new LabelValue(getProp().getText("components.redirect.cleaning.action.deleteDuplicate"), ActionType.DELETE_DUPLICATE.name()),
            new LabelValue(getProp().getText("components.redirect.cleaning.action.updateOptimize"), ActionType.UPDATE_OPTIMIZE.name())
        );
        page.addOptions("action", options, "label", "value", false);
    }

    @Override
    public boolean processAction(RedirectCleaningAction entity, String action) {
        if ("analyze".equals(action)) return analyze();
        if ("execute".equals(action)) return execute();
        return false;
    }

    @Override
    public RedirectCleaningAction insertItem(RedirectCleaningAction entity) {
        throwError("components.redirect.cleaning.unsupported");
        return null;
    }

    @Override
    public RedirectCleaningAction editItem(RedirectCleaningAction entity, long id) {
        throwError("components.redirect.cleaning.unsupported");
        return null;
    }

    @Override
    public boolean deleteItem(RedirectCleaningAction entity, long id) {
        throwError("components.redirect.cleaning.unsupported");
        return false;
    }

    /**
     * Analyzes redirects for the selected domain and optionally for the unnamed
     * scope, then atomically replaces the shared cached plan.
     *
     * @return {@code true} when analysis completes
     */
    private boolean analyze() {
        try {
            boolean includeUnnamed = Tools.getBooleanValue(getRequest().getParameter("customData"), false);
            RedirectCleaningPlan plan = coordinator.analyze(CloudToolsForCore.getDomainId(), includeUnnamed);
            addNotify(new NotifyBean(
                getProp().getText("components.redirect.cleaning.title"),
                getProp().getText(
                    "components.redirect.cleaning.analyzeSummary",
                    String.valueOf(plan.getUpdateCount()),
                    String.valueOf(plan.getDeleteCount()),
                    String.valueOf(plan.getIgnoredRecords())
                ),
                plan.isEmpty() ? NotifyType.INFO : NotifyType.SUCCESS,
                15000
            ));
            setForceReload(true);
            return true;
        } catch (OperationInProgressException exception) {
            throwBusyError(exception.getActiveOperation());
            return false;
        } catch (RuntimeException exception) {
            Logger.error(RedirectCleaningRestController.class, "Redirect cleaning analysis failed", exception);
            throwError("components.redirect.cleaning.analyzeError");
            return false;
        }
    }

    /**
     * Executes the exact shared plan. The coordinator removes it and refreshes
     * the redirect cache after successful execution.
     *
     * @return {@code true} when execution completes
     */
    private boolean execute() {
        ExecutionResult result;
        try {
            result = coordinator.execute( CloudToolsForCore.getDomainId() );
        } catch (OperationInProgressException exception) {
            throwBusyError(exception.getActiveOperation());
            return false;
        } catch (MissingPlanException exception) {
            throwError("components.redirect.cleaning.noPlan");
            return false;
        } catch (RuntimeException exception) {
            Logger.error(RedirectCleaningRestController.class, "Redirect cleaning execution failed", exception);
            throwError("components.redirect.cleaning.executeError");
            return false;
        }

        addNotify(new NotifyBean(
            getProp().getText("components.redirect.cleaning.title"),
            getProp().getText(
                "components.redirect.cleaning.executeSummary",
                String.valueOf(result.getUpdated()),
                String.valueOf(result.getDeleted()),
                String.valueOf(result.getSkipped())
            ),
            NotifyType.SUCCESS,
            15000
        ));
        setForceReload(true);
        return true;
    }

    /**
     * Reports which operation currently owns the domain lock.
     *
     * @param activeOperation operation started by another administrator
     */
    private void throwBusyError(OperationType activeOperation) {
        String key = activeOperation == OperationType.ANALYZE
            ? "components.redirect.cleaning.busyAnalyze"
            : "components.redirect.cleaning.busyExecute";
        throwError(key);
    }

    /**
     * Applies in-memory filtering, sorting, and paging to a plan snapshot.
     * Summary values always describe the complete plan, not only filtered rows.
     *
     * @param source complete action list
     * @param pageable requested page and sort
     * @param params DataTable search parameters
     * @param plan source plan, or {@code null} for an empty response
     * @return requested DataTable page
     */
    private Page<RedirectCleaningAction> createPage(
        List<RedirectCleaningAction> source,
        Pageable pageable,
        Map<String, String> params,
        RedirectCleaningPlan plan
    ) {
        List<RedirectCleaningAction> filtered = source.stream()
            .filter(action -> matchesAll(action, params))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        Comparator<RedirectCleaningAction> comparator = buildComparator(pageable.getSort());
        if (comparator != null) filtered.sort(comparator);

        int from = Math.toIntExact(Math.min(pageable.getOffset(), filtered.size()));
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        DatatablePageImpl<RedirectCleaningAction> page = new DatatablePageImpl<>(
            filtered.subList(from, to),
            pageable,
            filtered.size()
        );
        page.addSummary("updates", plan == null ? 0L : (long) plan.getUpdateCount());
        page.addSummary("deletes", plan == null ? 0L : (long) plan.getDeleteCount());
        page.addSummary("planAvailable", plan == null ? 0L : 1L);
        page.addSummary("includeUnnamed", plan != null && plan.isIncludeUnnamed() ? 1L : 0L);
        return page;
    }

    /** Filter/sort value type driving the matcher and comparator. */
    private enum FilterType { TEXT, NUMBER, DATE }

    /** Filterable and sortable column; the extractor serves both. */
    private record Column(FilterType type, Function<RedirectCleaningAction, ? extends Comparable<?>> extractor) {}

    /** Single source of truth for supported columns, keyed by DataTable property name. */
    private static final Map<String, Column> COLUMNS = Map.of(
        "id", new Column(FilterType.NUMBER, RedirectCleaningAction::getId),
        "action", new Column(FilterType.TEXT, RedirectCleaningAction::getAction),
        "oldUrl", new Column(FilterType.TEXT, RedirectCleaningAction::getOldUrl),
        "currentNewUrl", new Column(FilterType.TEXT, RedirectCleaningAction::getCurrentNewUrl),
        "proposedNewUrl", new Column(FilterType.TEXT, RedirectCleaningAction::getProposedNewUrl),
        "domainName", new Column(FilterType.TEXT, RedirectCleaningAction::getDomainName),
        "redirectCode", new Column(FilterType.NUMBER, RedirectCleaningAction::getRedirectCode),
        "insertDate", new Column(FilterType.DATE, RedirectCleaningAction::getInsertDate)
    );

    /** Tests a row against all filters; unknown keys never exclude a row. */
    private boolean matchesAll(RedirectCleaningAction action, Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = getCleanKey(entry.getKey());
            String value = entry.getValue();
            if (Tools.isEmpty(value) || "page".equals(key) || "size".equals(key) || "sort".equals(key)) continue;

            Column column = COLUMNS.get(key);
            if (column != null && !matches(column, action, value)) return false;
        }
        return true;
    }

    /** Applies one column filter using the type-appropriate matcher. */
    private static boolean matches(Column column, RedirectCleaningAction action, String value) {
        Comparable<?> field = column.extractor().apply(action);
        return switch (column.type()) {
            case TEXT -> matchesText(field == null ? null : String.valueOf(field), value);
            case NUMBER -> matchesNumber((Number) field, value);
            case DATE -> matchesDate((Date) field, value);
        };
    }

    /**
     * Matches text using DataTable exact, prefix, suffix, contains, or
     * {@code regex:} syntax.
     *
     * @param fieldValue value to test
     * @param searchValue encoded DataTable search value
     * @return whether the value matches
     */
    private static boolean matchesText(String fieldValue, String searchValue) {
        if (fieldValue == null) return "null".equals(searchValue);

        String cleanValue = getCleanValue(searchValue);
        String actual = fieldValue.toLowerCase(Locale.ROOT);
        String expected = cleanValue.toLowerCase(Locale.ROOT);
        if (searchValue.startsWith("regex:")) {
            try {
                return Pattern.compile(cleanValue, Pattern.CASE_INSENSITIVE).matcher(fieldValue).find();
            } catch (PatternSyntaxException ignored) {
                return false;
            }
        }
        if (searchValue.startsWith("^") && searchValue.endsWith("$")) return actual.equals(expected);
        if (searchValue.startsWith("^")) return actual.startsWith(expected);
        if (searchValue.endsWith("$")) return actual.endsWith(expected);
        return actual.contains(expected);
    }

    /**
     * Matches a number using text syntax or the inclusive {@code range:from-to} syntax.
     *
     * @param number value to test
     * @param value encoded DataTable search value
     * @return whether the value matches
     */
    private static boolean matchesNumber(Number number, String value) {
        if (number == null) return "null".equals(value);
        if (!value.startsWith("range:")) return matchesText(String.valueOf(number), value);

        String range = value.substring("range:".length());
        int separator = range.indexOf('-');
        String fromValue = separator < 0 ? range : range.substring(0, separator);
        String toValue = separator < 0 ? "" : range.substring(separator + 1);
        long actual = number.longValue();
        if (Tools.isNotEmpty(fromValue) && actual < Tools.getLongValue(fromValue, Long.MIN_VALUE)) return false;
        return Tools.isEmpty(toValue) || actual <= Tools.getLongValue(toValue, Long.MAX_VALUE);
    }

    /**
     * Matches a date using epoch text syntax or the half-open
     * {@code daterange:from-to} timestamp range.
     *
     * @param date value to test
     * @param value encoded DataTable search value
     * @return whether the value matches
     */
    private static boolean matchesDate(Date date, String value) {
        if (!value.startsWith("daterange:")) return matchesText(date == null ? null : String.valueOf(date.getTime()), value);
        if (date == null) return false;

        String range = value.substring("daterange:".length());
        int separator = range.indexOf('-');
        String fromValue = separator < 0 ? range : range.substring(0, separator);
        String toValue = separator < 0 ? "" : range.substring(separator + 1);
        long timestamp = date.getTime();
        if (Tools.isNotEmpty(fromValue) && timestamp < Tools.getLongValue(fromValue, Long.MIN_VALUE)) return false;
        return Tools.isEmpty(toValue) || timestamp < Tools.getLongValue(toValue, Long.MAX_VALUE);
    }

    /** Builds a stable comparator chain from the requested sort, nulls first. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparator<RedirectCleaningAction> buildComparator(Sort sort) {
        Comparator<RedirectCleaningAction> result = null;
        for (Sort.Order order : sort) {
            Column column = COLUMNS.get(order.getProperty());
            if (column == null) continue;
            Comparator<RedirectCleaningAction> field = Comparator.comparing(column.extractor(), Comparator.nullsFirst((Comparator) Comparator.naturalOrder()));
            if (order.isDescending()) field = field.reversed();
            result = result == null ? field : result.thenComparing(field);
        }
        return result;
    }
}

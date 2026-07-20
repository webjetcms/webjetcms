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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.redirects.RedirectClearingAction.ActionType;
import sk.iway.iwcm.components.redirects.RedirectClearingPlanCoordinator.MissingPlanException;
import sk.iway.iwcm.components.redirects.RedirectClearingPlanCoordinator.OperationInProgressException;
import sk.iway.iwcm.components.redirects.RedirectClearingPlanCoordinator.OperationType;
import sk.iway.iwcm.components.redirects.RedirectClearingService.ExecutionResult;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Read-only DataTable controller for redirect clearing previews.
 * <p>
 * Analysis stores one immutable plan in the application cache for the current
 * domain ID. All authorized administrators in that domain share the preview,
 * while paging, filtering, and sorting operate on the cached snapshot.
 * Execution applies the complete snapshot regardless of the visible page or
 * selected rows.
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/redirect-clearing")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
public class RedirectClearingRestController extends DatatableRestControllerV2<RedirectClearingAction, Long> {

    private final RedirectClearingPlanCoordinator coordinator;

    @Autowired
    public RedirectClearingRestController(RedirectClearingPlanCoordinator coordinator) {
        super(null);
        this.coordinator = coordinator;
    }

    @Override
    public Page<RedirectClearingAction> getAllItems(Pageable pageable) {
        RedirectClearingPlan plan = coordinator.getPlan( CloudToolsForCore.getDomainId() );
        List<RedirectClearingAction> actions = plan == null ? List.of() : plan.getActions();
        return createPage(actions, pageable, Map.of(), plan);
    }

    @Override
    public Page<RedirectClearingAction> searchItem(Map<String, String> params, Pageable pageable, RedirectClearingAction search) {
        RedirectClearingPlan plan = coordinator.getPlan( CloudToolsForCore.getDomainId() );
        List<RedirectClearingAction> actions = plan == null ? List.of() : plan.getActions();
        return createPage(actions, pageable, params, plan);
    }

    @Override
    public void getOptions(DatatablePageImpl<RedirectClearingAction> page) {
        List<LabelValue> options = List.of(
            new LabelValue(getProp().getText("components.redirect.clearing.action.deleteOld"), ActionType.DELETE_OLD.name()),
            new LabelValue(getProp().getText("components.redirect.clearing.action.deleteCycle"), ActionType.DELETE_CYCLE.name()),
            new LabelValue(getProp().getText("components.redirect.clearing.action.deleteDuplicate"), ActionType.DELETE_DUPLICATE.name()),
            new LabelValue(getProp().getText("components.redirect.clearing.action.updateOptimize"), ActionType.UPDATE_OPTIMIZE.name())
        );
        page.addOptions("action", options, "label", "value", false);
    }

    @Override
    public boolean processAction(RedirectClearingAction entity, String action) {
        if ("analyze".equals(action)) return analyze();
        if ("execute".equals(action)) return execute();
        return false;
    }

    @Override
    public RedirectClearingAction insertItem(RedirectClearingAction entity) {
        throwError("components.redirect.clearing.unsupported");
        return null;
    }

    @Override
    public RedirectClearingAction editItem(RedirectClearingAction entity, long id) {
        throwError("components.redirect.clearing.unsupported");
        return null;
    }

    @Override
    public boolean deleteItem(RedirectClearingAction entity, long id) {
        throwError("components.redirect.clearing.unsupported");
        return false;
    }

    /**
     * Analyzes redirects for the selected domain and optionally for the unnamed
     * scope, then atomically replaces the shared cached plan.
     *
     * @return {@code true} when analysis completes
     */
    @Transactional(transactionManager = "webjet2022TransactionManager", readOnly = true)
    private boolean analyze() {
        try {
            boolean includeUnnamed = Tools.getBooleanValue(getRequest().getParameter("customData"), false);
            RedirectClearingPlan plan = coordinator.analyze(CloudToolsForCore.getDomainId(), includeUnnamed);
            addNotify(new NotifyBean(
                getProp().getText("components.redirect.clearing.title"),
                getProp().getText(
                    "components.redirect.clearing.analyzeSummary",
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
            Logger.error(RedirectClearingRestController.class, "Redirect clearing analysis failed", exception);
            throwError("components.redirect.clearing.analyzeError");
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
            throwError("components.redirect.clearing.noPlan");
            return false;
        } catch (RuntimeException exception) {
            Logger.error(RedirectClearingRestController.class, "Redirect clearing execution failed", exception);
            throwError("components.redirect.clearing.executeError");
            return false;
        }

        addNotify(new NotifyBean(
            getProp().getText("components.redirect.clearing.title"),
            getProp().getText(
                "components.redirect.clearing.executeSummary",
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
            ? "components.redirect.clearing.busyAnalyze"
            : "components.redirect.clearing.busyExecute";
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
    private Page<RedirectClearingAction> createPage(
        List<RedirectClearingAction> source,
        Pageable pageable,
        Map<String, String> params,
        RedirectClearingPlan plan
    ) {
        List<RedirectClearingAction> filtered = source.stream()
            .filter(action -> matchesAll(action, params))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        Comparator<RedirectClearingAction> comparator = buildComparator(pageable.getSort());
        if (comparator != null) filtered.sort(comparator);

        int from = Math.toIntExact(Math.min(pageable.getOffset(), filtered.size()));
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        DatatablePageImpl<RedirectClearingAction> page = new DatatablePageImpl<>(
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
    private record Column(FilterType type, Function<RedirectClearingAction, ? extends Comparable<?>> extractor) {}

    /** Single source of truth for supported columns, keyed by DataTable property name. */
    private static final Map<String, Column> COLUMNS = Map.of(
        "id", new Column(FilterType.NUMBER, RedirectClearingAction::getId),
        "action", new Column(FilterType.TEXT, RedirectClearingAction::getAction),
        "oldUrl", new Column(FilterType.TEXT, RedirectClearingAction::getOldUrl),
        "currentNewUrl", new Column(FilterType.TEXT, RedirectClearingAction::getCurrentNewUrl),
        "proposedNewUrl", new Column(FilterType.TEXT, RedirectClearingAction::getProposedNewUrl),
        "domainName", new Column(FilterType.TEXT, RedirectClearingAction::getDomainName),
        "redirectCode", new Column(FilterType.NUMBER, RedirectClearingAction::getRedirectCode),
        "insertDate", new Column(FilterType.DATE, RedirectClearingAction::getInsertDate)
    );

    /** Tests a row against all filters; unknown keys never exclude a row. */
    private boolean matchesAll(RedirectClearingAction action, Map<String, String> params) {
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
    private static boolean matches(Column column, RedirectClearingAction action, String value) {
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
    private static Comparator<RedirectClearingAction> buildComparator(Sort sort) {
        Comparator<RedirectClearingAction> result = null;
        for (Sort.Order order : sort) {
            Column column = COLUMNS.get(order.getProperty());
            if (column == null) continue;
            Comparator<RedirectClearingAction> field = Comparator.comparing(column.extractor(), Comparator.nullsFirst((Comparator) Comparator.naturalOrder()));
            if (order.isDescending()) field = field.reversed();
            result = result == null ? field : result.thenComparing(field);
        }
        return result;
    }
}

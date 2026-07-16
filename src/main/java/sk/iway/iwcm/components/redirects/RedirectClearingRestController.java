package sk.iway.iwcm.components.redirects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import sk.iway.iwcm.components.redirects.RedirectClearingAction.ActionType;
import sk.iway.iwcm.components.redirects.RedirectClearingService.ExecutionResult;
import sk.iway.iwcm.system.UrlRedirectDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Read-only DataTable controller for redirect clearing previews.
 * <p>
 * Analysis stores one immutable plan in the current HTTP session. Paging,
 * filtering, and sorting operate on that snapshot, while execution applies the
 * complete snapshot regardless of the visible page or selected rows.
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/redirect-clearing")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
public class RedirectClearingRestController extends DatatableRestControllerV2<RedirectClearingAction, Long> {

    private static final String SESSION_PLAN_KEY = RedirectClearingRestController.class.getName() + ".plan";

    private final RedirectClearingService clearingService;

    @Autowired
    public RedirectClearingRestController(RedirectClearingService clearingService) {
        super(null);
        this.clearingService = clearingService;
    }

    @Override
    public Page<RedirectClearingAction> getAllItems(Pageable pageable) {
        RedirectClearingPlan plan = getCurrentPlan();
        List<RedirectClearingAction> actions = plan == null ? List.of() : plan.getActions();
        return createPage(actions, pageable, Map.of(), plan);
    }

    @Override
    public Page<RedirectClearingAction> searchItem(Map<String, String> params, Pageable pageable, RedirectClearingAction search) {
        RedirectClearingPlan plan = getCurrentPlan();
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
     * Analyzes redirects for the selected domain, replaces the session plan, and
     * requests a DataTable reload.
     *
     * @return {@code true} when analysis completes
     */
    private boolean analyze() {
        try {
            RedirectClearingPlan plan = clearingService.analyze(getCurrentDomain());
            getRequest().getSession().setAttribute(SESSION_PLAN_KEY, plan);
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
        } catch (RuntimeException exception) {
            Logger.error(RedirectClearingRestController.class, "Redirect clearing analysis failed", exception);
            throwError("components.redirect.clearing.analyzeError");
            return false;
        }
    }

    /**
     * Executes the exact session plan, removes it after success, and refreshes
     * the redirect cache once.
     *
     * @return {@code true} when execution completes
     */
    private boolean execute() {
        RedirectClearingPlan plan = getCurrentPlan();
        if (plan == null || plan.isEmpty()) {
            throwError("components.redirect.clearing.noPlan");
            return false;
        }

        ExecutionResult result;
        try {
            result = clearingService.execute(plan, getCurrentDomain());
        } catch (RuntimeException exception) {
            Logger.error(RedirectClearingRestController.class, "Redirect clearing execution failed", exception);
            throwError("components.redirect.clearing.executeError");
            return false;
        }

        getRequest().getSession().removeAttribute(SESSION_PLAN_KEY);
        try {
            UrlRedirectDB.refreshCache();
        } catch (RuntimeException exception) {
            Logger.error(RedirectClearingRestController.class, "Redirect cache refresh failed after clearing", exception);
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
     * Returns the plan for the currently selected domain. A plan created for a
     * different domain is removed from the session.
     *
     * @return current plan, or {@code null} when absent or invalid
     */
    private RedirectClearingPlan getCurrentPlan() {
        Object value = getRequest().getSession().getAttribute(SESSION_PLAN_KEY);
        if (!(value instanceof RedirectClearingPlan plan)) return null;

        if (!Objects.equals(plan.getAnalyzedDomain(), RedirectClearingService.normalizeDomain(getCurrentDomain()))) {
            getRequest().getSession().removeAttribute(SESSION_PLAN_KEY);
            return null;
        }
        return plan;
    }

    /**
     * @return normalized currently selected domain
     */
    private String getCurrentDomain() {
        return RedirectClearingService.normalizeDomain(CloudToolsForCore.getDomainName());
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
        return page;
    }

    /**
     * Tests a preview row against every supported DataTable filter.
     *
     * @param action preview row
     * @param params DataTable search parameters
     * @return {@code true} when all supported filters match
     */
    private boolean matchesAll(RedirectClearingAction action, Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = getCleanKey(entry.getKey());
            String value = entry.getValue();
            if (Tools.isEmpty(value) || "page".equals(key) || "size".equals(key) || "sort".equals(key)) continue;

            boolean matches = switch (key) {
                case "id" -> matchesNumber(action.getId(), value);
                case "action" -> action.getAction() != null && action.getAction().name().equals(getCleanValue(value));
                case "oldUrl" -> matchesText(action.getOldUrl(), value);
                case "currentNewUrl" -> matchesText(action.getCurrentNewUrl(), value);
                case "proposedNewUrl" -> matchesText(action.getProposedNewUrl(), value);
                case "domainName" -> matchesText(action.getDomainName(), value);
                case "redirectCode" -> matchesNumber(action.getRedirectCode(), value);
                case "publishDate" -> matchesDate(action.getPublishDate(), value);
                case "validTo" -> matchesDate(action.getValidTo(), value);
                case "insertDate" -> matchesDate(action.getInsertDate(), value);
                default -> true;
            };
            if (!matches) return false;
        }
        return true;
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

    /**
     * Builds a stable comparator chain from the requested DataTable sort.
     *
     * @param sort requested sort fields
     * @return comparator, or {@code null} when no supported field is requested
     */
    private static Comparator<RedirectClearingAction> buildComparator(Sort sort) {
        Comparator<RedirectClearingAction> result = null;
        for (Sort.Order order : sort) {
            Comparator<RedirectClearingAction> fieldComparator = comparatorFor(order.getProperty());
            if (fieldComparator == null) continue;
            if (order.isDescending()) fieldComparator = fieldComparator.reversed();
            result = result == null ? fieldComparator : result.thenComparing(fieldComparator);
        }
        return result;
    }

    /**
     * Resolves a nullable comparator for a supported DTO property.
     *
     * @param property DTO property name
     * @return property comparator, or {@code null} for an unsupported property
     */
    private static Comparator<RedirectClearingAction> comparatorFor(String property) {
        return switch (property) {
            case "id" -> nullableComparator(RedirectClearingAction::getId);
            case "action" -> nullableComparator(RedirectClearingAction::getAction);
            case "oldUrl" -> nullableComparator(RedirectClearingAction::getOldUrl);
            case "currentNewUrl" -> nullableComparator(RedirectClearingAction::getCurrentNewUrl);
            case "proposedNewUrl" -> nullableComparator(RedirectClearingAction::getProposedNewUrl);
            case "domainName" -> nullableComparator(RedirectClearingAction::getDomainName);
            case "redirectCode" -> nullableComparator(RedirectClearingAction::getRedirectCode);
            case "publishDate" -> nullableComparator(RedirectClearingAction::getPublishDate);
            case "validTo" -> nullableComparator(RedirectClearingAction::getValidTo);
            case "insertDate" -> nullableComparator(RedirectClearingAction::getInsertDate);
            default -> null;
        };
    }

    /**
     * Creates a natural-order comparator that places {@code null} values first.
     *
     * @param extractor property extractor
     * @param <V> comparable property type
     * @return nullable property comparator
     */
    private static <V extends Comparable<? super V>> Comparator<RedirectClearingAction> nullableComparator(
        Function<RedirectClearingAction, V> extractor
    ) {
        return Comparator.comparing(extractor, Comparator.nullsFirst(Comparator.naturalOrder()));
    }
}

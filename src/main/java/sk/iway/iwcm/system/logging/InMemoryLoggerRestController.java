package sk.iway.iwcm.system.logging;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * REST controller pre zobrazenie poslednych log sprav z pamate
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/in-memory-logging")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_in-memory-logging')")
public class InMemoryLoggerRestController extends DatatableRestControllerV2<InMemoryLoggingEvent, Long> {

    @Override
    public Page<InMemoryLoggingEvent> getAllItems(Pageable pageable) {
        Queue<InMemoryLoggingEvent> queue = null;
        if (ClusterDB.isServerRunningInClusterMode()) {
            String node = getRequest().getParameter("node");
            if (Tools.isNotEmpty(node) && !node.equals(getCurrentNode().getValue())) {
                queue = getLoggingFromNode(node);
            }
        }

        if (queue == null) {
            queue = InMemoryLoggingDB.getInstance().getQueue();
        }

        DatatablePageImpl<InMemoryLoggingEvent> pages = new DatatablePageImpl<>(new ArrayList<>(queue));
        pages.addOptions("level", getTypes(), "label", "value", false);

        if (ClusterDB.isServerRunningInClusterMode()) {
            LabelValueDetails currentNode = getCurrentNode();
            pages.addOption("currentNode", currentNode.getLabel(), currentNode.getValue(), false);
            List<LabelValueDetails> nodes = getNodes();
            if (!nodes.isEmpty()) {
                pages.addOptions("nodes", nodes, "label", "value", false);
            }
        }

        return pages;
    }

    private CircularFifoQueue<InMemoryLoggingEvent> getLoggingFromNode(String node) {
        ClusterDB.addRefreshClusterMonitoring(node, InMemoryLoggingDB.class);
        InMemoryLoggingDB inMemoryLoggingDB = InMemoryLoggingDB.getInstance();
        CircularFifoQueue<InMemoryLoggingEvent> result = null;
        for (int i = 0; i < 30; i++) {
            result = inMemoryLoggingDB.readLogging(node);

            if (result == null) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException ignored) {
                    //neriesime nic
                }
            }
        }

        return result;
    }

    private LabelValueDetails getCurrentNode() {
        LabelValueDetails labelValueDetails = new LabelValueDetails();
        labelValueDetails.setLabel(Constants.getString("clusterMyNodeName"));
        labelValueDetails.setValue(Constants.getString("clusterMyNodeName"));
        return labelValueDetails;
    }

    private List<LabelValueDetails> getNodes() {
        List<LabelValueDetails> result = new ArrayList<>();

        for (String nodeName : ClusterDB.getClusterNodeNamesExpandedAuto()) {
            LabelValueDetails labelValueDetail = new LabelValueDetails();
            labelValueDetail.setLabel(nodeName);
            labelValueDetail.setValue(nodeName);
            result.add(labelValueDetail);
        }

        return result;
    }

    private List<LabelValueDetails> getTypes() {
        List<LabelValueDetails> types = new ArrayList<>();
        String[] levels = {"DEBUG", "ERROR", "INFO", "TRACE", "WARN"};

        for (String level : levels) {
            LabelValueDetails lvd = new LabelValueDetails();
            lvd.setValue(level);
            lvd.setLabel(level);
            types.add(lvd);
        }

        return types;
    }

    @Override
    public InMemoryLoggingEvent editItem(InMemoryLoggingEvent entity, long logId) {
        throwError("datatables.error.recordIsNotEditable");
        return null;
    }

    @Override
    public boolean beforeDelete(InMemoryLoggingEvent entity) {
        return false;
    }
}

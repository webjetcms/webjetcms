package sk.iway.iwcm.components.monitoring.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.monitoring.jpa.ExecutionEntry;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.monitoring.ExecutionTimeMonitor;
import sk.iway.iwcm.system.monitoring.MonitoringDB;

public class MonitoringNodeService {

    //What kind page is calling
    private static enum ShowType {
        COMPONENTS, //server_monitoring/admin/components/
        DOCUMENTS, //server_monitoring/admin/documents/
        SQL, //server_monitoring/admin/sql/
        UNKNOW //something wrong
	}

    private ShowType showType;
    private String node;

    public MonitoringNodeService(HttpServletRequest request) {
        showType = getShowType(Tools.getRequestParameter(request, "showType"));
        node = Tools.getRequestParameter(request, "selectedNode");
    }

    /**
     * Transform string text to one of ShowType enum values.
     * IF text is invalid or doesn't match enum values, return ShowType.UNKNOW value (compare is using toLowerCase).
     * @param showType
     * @return ShowType enum value
     */
    private static ShowType getShowType(String showType) {
        if(showType == null || showType.isEmpty()) return ShowType.UNKNOW;
        else if(showType.toLowerCase().equals("components")) return ShowType.COMPONENTS;
        else if(showType.toLowerCase().equals("documents")) return ShowType.DOCUMENTS;
        else if(showType.toLowerCase().equals("sql")) return ShowType.SQL;
        else return ShowType.UNKNOW;
    }

    /**
     * Based on request values "showType" and "selectedNode", return list of corresponding data's.
     * @return List<ExecutionEntry>
     */
    public List<ExecutionEntry> getAll() {
        if(isNodeSupported(node) && !isNodeActual(node)) {
            return diffNodeData(showType, node);
        }
        else return currentNodeData(showType);
    }

    /**
     * By inserted param's value's, return List<ExecutionEntry> data of current node (local data).
     * In case any problem return just empty list new ArrayList<>().
     * @param showType
     * @param node
     * @return
     */
    private static List<ExecutionEntry> diffNodeData(ShowType showType, String node) {
        if(showType == ShowType.COMPONENTS)
           return MonitoringDB.getComponentStatsFor(node);
        else if(showType == ShowType.DOCUMENTS)
            return MonitoringDB.getDocumentStatsFor(node);
        else if(showType == ShowType.SQL)
            return MonitoringDB.getSqlStatsFor(node);
        else
            return new ArrayList<>();
    }

    /**
     * Returns last update of data for remote nodes or NULL if there are no data or node is local
     * @return
     */
    public Date getLastUpdate() {
        Date date = null;
        String sql = "SELECT created_at FROM cluster_monitoring WHERE type=? AND node=?";
        if(isNodeSupported(node) && !isNodeActual(node)) {
            String type = "sql";
            if (showType == ShowType.COMPONENTS) type="component";
            else if (showType == ShowType.DOCUMENTS) type="document";

            date = (new SimpleQuery()).forDate(sql, type, node);
        }
        return date;
    }

    /**
     * By inserted param's value's, return List<ExecutionEntry> data of NOT current node (data from DB).
     * In case any problem return just empty list new ArrayList<>().
     * @param showType
     * @return
     */
    private static List<ExecutionEntry> currentNodeData(ShowType showType) {
        if(showType == ShowType.COMPONENTS)
            return ExecutionTimeMonitor.statsForComponents();
        else if(showType == ShowType.DOCUMENTS)
            return ExecutionTimeMonitor.statsForDocuments();
        else if(showType == ShowType.SQL)
            return ExecutionTimeMonitor.statsForSqls();
        else
            return new ArrayList<>();
    }

    /**
     * Get list of all supported node's and check, if value from input is in this list.
     * @param node
     * @return True - value is inside list of supported nodes, else False
     */
    private static boolean isNodeSupported(String node) {
        if(node == null || node.isEmpty()) return false;
        List<String> allNodes = ClusterDB.getClusterNodeNamesExpandedAuto();
        if(allNodes != null) {
            for(String oneNode : allNodes) {
                if(oneNode.equals(node)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get actual node and check, if if value from input equal actual node.
     * @param node
     * @return True - value is equal to actual node, else False
     */
    private static boolean isNodeActual(String node) {
        String actualNode = Constants.getString("clusterMyNodeName");
        return actualNode.equals(node) ? true : false;
    }

    /**
     * Return list of all nodes, only if isServerRunningInClusterMode.
     * The current node will be always on 1st place.
     * @return
     */
    public static List<String> getAllNodes() {
        List<String> response = new ArrayList<>();
        if(ClusterDB.isServerRunningInClusterMode()) {
            String actualNode = Constants.getString("clusterMyNodeName");
            response.add(actualNode);

            List<String> allNodes = ClusterDB.getClusterNodeNamesExpandedAuto();
            if(allNodes != null) {
                for(String node : allNodes) {
                    if(actualNode.equals(node)) continue;
                    response.add(node);
                }
            }
        }
        return response;
    }

    /**
     * Do reset data (remove local data), ONLY for actual node, and for selected show type (node is checked that is current).
     * @param actualShowType
     * @param selectedNode
     */
    public static void resetData(String actualShowType, String selectedNode) {
        //BE check, reset data ONLY if its current node
        ShowType showType = getShowType(actualShowType);
        if(isNodeSupported(selectedNode) && isNodeActual(selectedNode)) {
            if(showType == ShowType.COMPONENTS)
                ExecutionTimeMonitor.resetComponentMeasurements();
            else if(showType == ShowType.DOCUMENTS)
                ExecutionTimeMonitor.resetDocumentMeasurements();
            else if(showType == ShowType.SQL)
                ExecutionTimeMonitor.resetSqlMeasurements();
        }
    }

    /**
     * Refresh data for specific node, only if node ISN'T actual
     * @param selectedNode
     */
    public static void refreshData(String selectedNode) {
        if(isNodeSupported(selectedNode) && !isNodeActual(selectedNode))
            ClusterDB.addRefreshClusterMonitoring(selectedNode, MonitoringDB.class);
    }
}

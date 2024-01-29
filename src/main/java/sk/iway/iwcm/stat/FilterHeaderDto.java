package sk.iway.iwcm.stat;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupsDB;

@Getter
@Setter
public class FilterHeaderDto {
    private Date dateFrom;
    private Date dateTo;
    private Integer rootGroupId;
    private Boolean filterBotsOut;
    private ChartType chartType;
    private String rootGroupIdQuery;
    private String url;
    private String searchEngineName;
    private Integer webPageId;
    private String statType;

    public FilterHeaderDto() {
        this.dateFrom = null;
        this.dateTo = null;
        this.rootGroupId = -1;
        this.filterBotsOut = false;
        this.chartType = ChartType.NOT_CHART;
        this.rootGroupIdQuery = "";
        this.searchEngineName = "";
        this.webPageId = -1;
        this.statType = "days";
    }

    public void setRootGroupId(Integer rootGroupId) {
        this.rootGroupId = rootGroupId;
        if(rootGroupId != null && rootGroupId != -1)
            this.rootGroupIdQuery = groupIdToQuery(rootGroupId);
    }

    //Process rootGroupId into query + subTrees
    public static String groupIdToQuery(Integer groupId) {
        if(groupId == null || groupId == -1)
            return "";
        else {
            GroupsDB groupsDB = GroupsDB.getInstance();
            StringBuilder query = new StringBuilder();

            int[] rootGroups = {groupId};
            query.append(" AND group_id IN(" + groupId + ", ");
            int[] childsGroupIds = groupsDB.expandGroupIdsToChilds(rootGroups);
            for(int i = 0; i < childsGroupIds.length; i++)
                if(i == childsGroupIds.length - 1)
                    query.append(childsGroupIds[i] + "");
                else
                    query.append(childsGroupIds[i] + ", ");
            query.append(") ");

            return query.toString();
        }
    }

    public String toString() {
        return "from="+Tools.formatDate(dateFrom)+" to="+Tools.formatDate(dateTo)+" rootGroup="+rootGroupId+" filterBots="+filterBotsOut;
    }
}

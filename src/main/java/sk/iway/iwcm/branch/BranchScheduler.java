package sk.iway.iwcm.branch;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.users.UsersDB;

/** Existing WebJET cron entry point for due packages on administrative nodes. */
public final class BranchScheduler {
    private BranchScheduler() { }

    public static void main(String[] args) {
        if(!Constants.getBoolean("branchesEnabled") || !Constants.getBoolean("enableStaticFilesExternalDir") || ClusterDB.isPublicNode())return;
        try {
            // Stagger dynamic admin nodes; the database claim remains the protection against duplicate execution.
            Thread.sleep(ThreadLocalRandom.current().nextInt(10000));
        }catch(InterruptedException ex){Thread.currentThread().interrupt();return;}
        RequestBean previous=SetCharacterEncodingFilter.getCurrentRequestBean();
        try(Connection c=DBPool.getConnection()) {
            BranchReadiness.verify(c);
            List<Due> due=new ArrayList<>();
            try(PreparedStatement s=c.prepareStatement("SELECT id,domain_id,revision,scheduled_by FROM branches WHERE state='SCHEDULED' AND publish_at<=? ORDER BY publish_at,id")) {
                s.setTimestamp(1,new Timestamp(System.currentTimeMillis()));
                try(ResultSet r=s.executeQuery()){while(r.next())due.add(new Due(r.getLong(1),r.getInt(2),r.getLong(3),r.getInt(4)));}
            }
            for(Due item:due) {
                try {
                    String domain = BranchDomains.name(item.domainId);
                    var details=UsersDB.getUser(item.actor);
                    if(details==null||!details.isAdmin()||!details.isAuthorized())throw new IllegalStateException(Prop.getInstance().getText("branches.schedule.user_unavailable"));
                    Identity user=new Identity(details);
                    if(!user.isEnabledItem(BranchAccess.PERMISSION))throw new IllegalStateException(Prop.getInstance().getText("branches.schedule.permission"));
                    RequestBean bean=new RequestBean();bean.setUser(user);bean.setDomain(domain);
                    SetCharacterEncodingFilter.setCurrentRequestBean(bean);
                    try(BranchContexts.Scope ignored=BranchContexts.live(null,domain,item.actor)) {
                        BranchMergeService merge=new BranchMergeService();
                        var report=merge.prepare(item.id,domain,user,item.revision);
                        merge.claimScheduled(item.id,domain,user,item.revision,report.preparationHash());
                        new BranchPublishService().kickoff(item.id,item.actor);
                    }
                }catch(Exception ex){attention(item,ex);}
                finally{SetCharacterEncodingFilter.setCurrentRequestBean(previous);BranchView.invalidate();}
            }
        }catch(Exception ex){Logger.error(BranchScheduler.class,ex);}
        finally {SetCharacterEncodingFilter.setCurrentRequestBean(previous);}
    }

    private static void attention(Due item,Exception error) {
        String reason=error instanceof ResponseStatusException response?Objects.toString(response.getReason(),Prop.getInstance().getText("branches.schedule.permission")):Objects.toString(error.getMessage(),error.getClass().getSimpleName());
        try(Connection c=DBPool.getConnection();PreparedStatement s=c.prepareStatement("UPDATE branches SET state='NEEDS_ATTENTION',revision=revision+1,last_error=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND revision=? AND state='SCHEDULED' AND publish_at<=?")) {
            s.setString(1,reason);s.setLong(2,item.id);s.setLong(3,item.revision);s.setTimestamp(4,new Timestamp(System.currentTimeMillis()));s.executeUpdate();
        }catch(SQLException ex){Logger.error(BranchScheduler.class,ex);}
    }

    private record Due(long id,int domainId,long revision,int actor) { }
}

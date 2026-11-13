package sk.iway.iwcm.branch;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.users.UsersDB;

/** Fixed WebJET cron task that publishes queued requests and due packages on administrative nodes. */
public final class BranchScheduler {
    private static final AtomicBoolean RUNNING=new AtomicBoolean();
    private BranchScheduler() { }

    public static void main(String[] args) {
        if(!BranchAccess.isEnabled() || ClusterDB.isPublicNode())return;
        if(!RUNNING.compareAndSet(false,true))return;
        try {publish();}
        finally {RUNNING.set(false);}
    }

    private static void publish() {
        try {
            // Stagger dynamic admin nodes; the database claim remains the protection against duplicate execution.
            Thread.sleep(ThreadLocalRandom.current().nextInt(10000));
        }catch(InterruptedException ex){Thread.currentThread().interrupt();return;}
        RequestBean previous=SetCharacterEncodingFilter.getCurrentRequestBean();
        try {
            BranchReadiness.verify();
            BranchService branches=BranchService.getInstance();
            for(BranchEntity item:branches.publicationRequests()) {
                try {new BranchPublishService().runRequested(item.getId(),item.getPublishRequestedBy(),item.getRevision());}
                catch(Exception ex){Logger.error(BranchScheduler.class,ex);}
                finally{SetCharacterEncodingFilter.setCurrentRequestBean(previous);BranchView.invalidate();}
            }
            for(BranchEntity item:branches.due(new Timestamp(System.currentTimeMillis()))) {
                try {
                    String domain = BranchDomains.name(item.getDomainId());
                    var details=UsersDB.getUser(item.getScheduledBy());
                    if(details==null||!details.isAdmin()||!details.isAuthorized())throw new IllegalStateException(Prop.getInstance().getText("branches.schedule.user_unavailable"));
                    Identity user=new Identity(details);
                    if(!user.isEnabledItem(BranchAccess.PERMISSION))throw new IllegalStateException(Prop.getInstance().getText("branches.schedule.permission"));
                    RequestBean bean=new RequestBean();bean.setUser(user);bean.setDomain(domain);
                    SetCharacterEncodingFilter.setCurrentRequestBean(bean);
                    try(BranchContexts.Scope ignored=BranchContexts.live(null,domain,item.getScheduledBy())) {
                        BranchMergeService merge=new BranchMergeService();
                        var report=merge.prepare(item.getId(),domain,user,item.getRevision());
                        merge.claimScheduled(item.getId(),domain,user,item.getRevision(),report.preparationHash());
                        new BranchPublishService().run(item.getId(),item.getScheduledBy());
                    }
                }catch(Exception ex){attention(item,ex);}
                finally{SetCharacterEncodingFilter.setCurrentRequestBean(previous);BranchView.invalidate();}
            }
        }catch(Exception ex){Logger.error(BranchScheduler.class,ex);}
        finally {try {SetCharacterEncodingFilter.unRegisterDataContext();}finally {SetCharacterEncodingFilter.setCurrentRequestBean(previous);}}
    }

    private static void attention(BranchEntity item,Exception error) {
        String reason=error instanceof ResponseStatusException response?Objects.toString(response.getReason(),Prop.getInstance().getText("branches.schedule.permission")):Objects.toString(error.getMessage(),error.getClass().getSimpleName());
        try {BranchService.getInstance().stopScheduled(item.getId(),item.getRevision(),reason);}
        catch(Exception ex){Logger.error(BranchScheduler.class,ex);}
    }
}

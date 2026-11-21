package sk.iway.iwcm.branch;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.users.UsersDB;

/** Publishes queued requests and due packages on administrative nodes. */
public final class BranchScheduler {
    private static final AtomicBoolean CRON_RUNNING=new AtomicBoolean();
    private static final ReentrantLock PUBLICATION_LOCK=new ReentrantLock(true);
    private BranchScheduler() { }

    public static void main(String[] args) {
        if(!canPublish() || !CRON_RUNNING.compareAndSet(false,true))return;
        try {
            // Stagger dynamic admin nodes; the database claim remains the protection against duplicate execution.
            Thread.sleep(ThreadLocalRandom.current().nextInt(10000));
            publish(true);
        }catch(InterruptedException ex){Thread.currentThread().interrupt();}
        finally {CRON_RUNNING.set(false);}
    }

    static void publishRequested() {
        if(canPublish())publish(false);
    }

    private static boolean canPublish() {
        return BranchAccess.isEnabled() && !ClusterDB.isPublicNode();
    }

    private static void publish(boolean includeScheduled) {
        try {
            PUBLICATION_LOCK.lockInterruptibly();
        }catch(InterruptedException ex){Thread.currentThread().interrupt();return;}
        try {processQueue(includeScheduled);}
        finally {PUBLICATION_LOCK.unlock();}
    }

    private static void processQueue(boolean includeScheduled) {
        RequestBean previous=SetCharacterEncodingFilter.getCurrentRequestBean();
        try {
            BranchReadiness.verify();
            BranchService branches=BranchService.getInstance();
            for(BranchEntity item:branches.publicationRequests()) {
                try {new BranchPublishService().runRequested(item.getId(),item.getPublishRequestedBy(),item.getRevision());}
                catch(Exception ex){Logger.error(BranchScheduler.class,ex);}
                finally{SetCharacterEncodingFilter.setCurrentRequestBean(previous);BranchView.invalidate();}
            }
            if (includeScheduled) {
                publishScheduled(branches, previous);
                branches.retryDeletions();
            }
        }catch(Exception ex){Logger.error(BranchScheduler.class,ex);}
        finally {
            try {SetCharacterEncodingFilter.unRegisterDataContext();}
            finally {SetCharacterEncodingFilter.setCurrentRequestBean(previous);}
        }
    }

    private static void publishScheduled(BranchService branches,RequestBean previous) {
        for(BranchEntity item:branches.due(new Timestamp(System.currentTimeMillis()))) {
            try {
                String domain = BranchDomains.name(item.getDomainId());
                RequestBean bean = new RequestBean();
                bean.setDomain(domain);
                SetCharacterEncodingFilter.setCurrentRequestBean(bean);
                var details=UsersDB.getUser(item.getScheduledBy());
                if(details==null||!details.isAdmin()||!details.isAuthorized())throw new IllegalStateException(Prop.getInstance().getText("branches.schedule.user_unavailable"));
                Identity user=new Identity(details);
                if(!user.isEnabledItem(BranchAccess.PERMISSION))throw new IllegalStateException(Prop.getInstance().getText("branches.schedule.permission"));
                bean.setUser(user);
                try(BranchContexts.Scope ignored=BranchContexts.live(null,domain,item.getScheduledBy())) {
                    BranchMergeService merge=new BranchMergeService();
                    var report=merge.prepare(item.getId(),domain,user,item.getRevision());
                    merge.claimScheduled(item.getId(),domain,user,item.getRevision(),report.preparationHash());
                    new BranchPublishService().run(item.getId(),item.getScheduledBy());
                }
            }catch(Exception ex){attention(item,ex);}
            finally{SetCharacterEncodingFilter.setCurrentRequestBean(previous);BranchView.invalidate();}
        }
    }

    private static void attention(BranchEntity item,Exception error) {
        String reason=error instanceof ResponseStatusException response?Objects.toString(response.getReason(),Prop.getInstance().getText("branches.schedule.permission")):Objects.toString(error.getMessage(),error.getClass().getSimpleName());
        try {BranchService.getInstance().stopScheduled(item.getId(),item.getRevision(),reason);}
        catch(Exception ex){Logger.error(BranchScheduler.class,ex);}
    }
}

package sk.iway.iwcm.branch;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

/** Domain-scoped metadata persistence for working versions. */
@Service
public class BranchService {
    private final BranchRepository repository;

    public BranchService(BranchRepository repository) { this.repository = repository; }

    /** Returns the managed service for request filters and existing cron entry points. */
    public static BranchService getInstance() {
        return Tools.getSpringBean("branchService", BranchService.class);
    }

    public List<BranchEntity> list(String domainName, boolean includeClosed) {
        return list(BranchDomains.id(domainName), includeClosed);
    }

    public List<BranchEntity> list(int domainId, boolean includeClosed) {
        BranchReadiness.requireEnabled();
        return includeClosed ? repository.findAllByDomainIdOrderByIdDesc(domainId)
            : repository.findAllByDomainIdAndStateNotInOrderByIdDesc(domainId, List.of("MERGED", "CANCELLED", "DELETING"));
    }

    public Optional<BranchEntity> find(long id, String domainName) {
        return find(id, BranchDomains.id(domainName));
    }

    public Optional<BranchEntity> find(long id, int domainId) {
        BranchReadiness.requireEnabled();
        return repository.findFirstByIdAndDomainId(id, domainId);
    }

    static void validatePublishAt(Timestamp at) {
        if(at!=null && at.getTime()<=System.currentTimeMillis())
            throw new ResponseStatusException(HttpStatus.CONFLICT,sk.iway.iwcm.i18n.Prop.getInstance().getText("branches.schedule.future"));
    }

    /** Serializes DataTable metadata edits with the transition to DELETING. */
    @Transactional(transactionManager = "webjet2022TransactionManager")
    public BranchEntity editMetadata(long id, int domainId, Supplier<BranchEntity> edit) {
        BranchEntity branch = repository.lockMetadata(id, domainId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireNotDeleting(branch.getState());
        return edit.get();
    }

    static void requireNotDeleting(String state) {
        if ("DELETING".equals(state)) throw deletionPending();
    }

    private static ResponseStatusException deletionPending() {
        return new ResponseStatusException(HttpStatus.CONFLICT, Prop.getInstance().getText("branches.delete.pending.js"));
    }

    /** Commits deletion of the checked revision in two stages so a failed cleanup can be retried. */
    @Transactional(transactionManager = "webjet2022TransactionManager")
    public void delete(long id, int domainId, Identity user) {
        BranchReadiness.requireEnabled();
        BranchEntity branch=repository.findFirstByIdAndDomainId(id,domainId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Publishing package not found"));
        BranchMergeService.requireDiscardPermissions(branch, user);
        if (!"DELETING".equals(branch.getState())
                && repository.markDeleting(id, domainId, branch.getRevision(), user.getUserId(), Timestamp.from(Instant.now())) != 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (!finishDeletion(id)) throw deletionPending();
            }
        });
        BranchView.invalidate();
    }

    /** Retries committed deletions left by an interrupted request or a storage failure. */
    void retryDeletions() {
        for (long id : repository.pendingDeletions()) finishDeletion(id);
    }

    private boolean finishDeletion(long id) {
        try {
            BranchFiles.deleteBranch(id);
            repository.finishDeletion(id);
            return true;
        } catch (IOException | RuntimeException ex) {
            Logger.error(BranchService.class, "Deletion of publishing package " + id + " will be retried", ex);
            return false;
        }
    }

    /** Resolves server-owned routing metadata before the preview checks domain permissions. */
    public int previewDomainId(long id) {
        BranchReadiness.requireEnabled();
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getDomainId();
    }

    public List<BranchEntity> due(Timestamp now) {
        BranchReadiness.requireEnabled();
        return repository.findAllByStateAndPublishAtLessThanEqualOrderByPublishAtAscIdAsc("SCHEDULED", now);
    }

    public BranchEntity publication(long id) {
        BranchReadiness.requireEnabled();
        return repository.findById(id).filter(branch -> "MERGING".equals(branch.getState()))
            .orElseThrow(() -> new IllegalStateException("The branch is not owned by the publisher"));
    }

    public List<BranchEntity> publicationRequests() {
        BranchReadiness.requireEnabled();
        return repository.findAllByStateAndPublishRequestedByIsNotNullOrderByIdAsc("MERGING");
    }

    public boolean takePublicationRequest(long id,long revision,int actor) {
        BranchReadiness.requireEnabled();
        return repository.takePublicationRequest(id,revision,actor)==1;
    }

    public void completePublication(long id) {
        if (repository.markMerged(id, Timestamp.from(Instant.now())) != 1)
            throw new IllegalStateException("Publication ownership was lost");
    }

    public void failPublication(long id, boolean applied, String reason) {
        repository.markPublicationFailed(id, applied, reason, Timestamp.from(Instant.now()));
    }

    public void stopScheduled(long id, long revision, String reason) {
        repository.markNeedsAttention(id, revision, reason, Timestamp.from(Instant.now()));
    }

}

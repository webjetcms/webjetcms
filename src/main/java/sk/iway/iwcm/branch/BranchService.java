package sk.iway.iwcm.branch;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Tools;

/** Domain-scoped metadata persistence. Authorization belongs to the calling boundary. */
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
            : repository.findAllByDomainIdAndStateNotInOrderByIdDesc(domainId, List.of("MERGED", "CANCELLED"));
    }

    public Optional<BranchEntity> find(long id, String domainName) {
        return find(id, BranchDomains.id(domainName));
    }

    public Optional<BranchEntity> find(long id, int domainId) {
        BranchReadiness.requireEnabled();
        return repository.findFirstByIdAndDomainId(id, domainId);
    }

    /** Deletes a domain-scoped version in any state, including its private files and cascading WORK data. */
    public void delete(long id, int domainId) {
        BranchReadiness.requireEnabled();
        BranchEntity branch=repository.findFirstByIdAndDomainId(id,domainId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Publishing package not found"));
        try { BranchFiles.deleteBranch(id); }
        catch (IOException ex) { throw new IllegalStateException("Cannot delete working version files",ex); }
        repository.delete(branch);
        BranchView.invalidate();
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

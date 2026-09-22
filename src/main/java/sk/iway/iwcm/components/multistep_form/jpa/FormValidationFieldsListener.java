package sk.iway.iwcm.components.multistep_form.jpa;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.multistep_form.rest.MultistepFormsService;

/**
 * Invalidates cached validation fields when form steps or items change.
 * With active transaction synchronization, eviction runs once per form and domain
 * after commit so rolled-back changes do not clear the cache. Otherwise, eviction is immediate.
 */
public class FormValidationFieldsListener {

    /**
     * Invalidates cached validation fields after a form step or item is persisted, updated, or removed.
     *
     * <p>Reads the entity's current {@code formName} and {@code domainId} through
     * {@link BeanWrapperImpl}, allowing the same JPA lifecycle callback to handle both
     * {@link FormStepEntity} and {@link FormItemEntity}. No invalidation is scheduled
     * when the form name is empty or the domain identifier is {@code null}.</p>
     *
     * <p>When Spring transaction synchronization is active, registers an {@link Invalidation}
     * callback that clears the cache only after a successful commit. Deferring eviction
     * prevents rolled-back changes from clearing the cache and avoids other requests
     * rebuilding it from database values that have not yet been updated by the commit.
     * The record's value-based equality identifies callbacks with the same form name and
     * domain identifier, so multiple step or item changes within the same transaction
     * register only one invalidation for that pair.</p>
     *
     * <p>When transaction synchronization is inactive, clears the cache immediately by
     * invoking the callback directly. In both cases, cache eviction is delegated to
     * {@link MultistepFormsService#clearValidationFieldsCache(String, Integer)}.</p>
     *
     * @param entity changed form step or item exposing readable {@code formName} ({@link String})
     *               and {@code domainId} ({@link Integer}) properties
     */
    @PostPersist
    @PostUpdate
    @PostRemove
    public void onChange(Object entity) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(entity);
        String formName = (String) wrapper.getPropertyValue("formName");
        Integer domainId = (Integer) wrapper.getPropertyValue("domainId");
        if (Tools.isEmpty(formName) || domainId == null) return;

        Invalidation invalidation = new Invalidation(formName, domainId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            if (TransactionSynchronizationManager.getSynchronizations().contains(invalidation) == false) {
                TransactionSynchronizationManager.registerSynchronization(invalidation);
            }
        } else {
            invalidation.afterCommit();
        }
    }

    /**
     * Identifies an eviction by form and domain so repeated changes share one after-commit callback.
     *
     * @param formName logical form name
     * @param domainId domain owning the form
     */
    private record Invalidation(String formName, Integer domainId) implements TransactionSynchronization {
        @Override
        public void afterCommit() {
            MultistepFormsService.clearValidationFieldsCache(formName, domainId);
        }
    }
}

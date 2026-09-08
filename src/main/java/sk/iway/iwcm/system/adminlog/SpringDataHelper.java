package sk.iway.iwcm.system.adminlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Component
public class SpringDataHelper {

    @Autowired
    private Map<String, EntityManager> entityManagers;

    /**
     * Fetches the real entity from the database by iterating through all available EntityManagers.
     *
     * @param entity The entity class to fetch.
     * @param id     The ID of the entity.
     * @return The real entity from the database, or null if not found.
     */
    public Object getSpringDataEntity(Object entity, Long id) {
        for (Map.Entry<String, EntityManager> entry : entityManagers.entrySet()) {
            //Logger.debug(this.getClass(), "Searching for entity " + entity.getClass().getSimpleName() + " in persistence unit: " + entry.getKey());
            EntityManager freshEntityManager = null;
            try {
                EntityManager entityManager = entry.getValue();
                EntityManagerFactory factory = entityManager.getEntityManagerFactory();

                // Do NOT call entityManager.contains(entity) on the shared, transactional EntityManager.
                // This method is called from JPA lifecycle callbacks (@PreUpdate) that run during the
                // UnitOfWork commit. On EclipseLink 5.x touching the committing EntityManager corrupts the
                // change set and leads to "uowChangeSet is null" NullPointerException during commit.
                // Instead only check whether this persistence unit manages the entity class via the metamodel.
                boolean managesEntityClass = factory.getMetamodel().getEntities().stream()
                    .anyMatch(entityType -> entityType.getJavaType().equals(entity.getClass()));
                if (managesEntityClass == false) {
                    continue; // Skip to the next persistence unit
                }

                // Create a fresh EntityManager for the current persistence unit
                freshEntityManager = factory.createEntityManager();

                // Try to fetch the entity from the database
                Object dbEntity = freshEntityManager.find(entity.getClass(), id);

                if (dbEntity != null) {
                    //Logger.debug(this.getClass(), "Entity found in persistence unit: " + entityManager.toString());
                    return dbEntity; // Return the entity if found
                }
            } catch (IllegalArgumentException e) {
                //Logger.debug(this.getClass(), "Entity not found in persistence unit: " + entry.getValue(), e);
            } catch (Exception e) {
                //Logger.error(this.getClass(), "Error fetching entity from persistence unit: " + entry.getValue(), e);
            } finally {
                // Ensure the fresh EntityManager is closed
                if (freshEntityManager != null && freshEntityManager.isOpen()) {
                    freshEntityManager.close();
                }
                freshEntityManager = null; // Clear the reference to the EntityManager
            }
        }

        // Return null if the entity was not found in any persistence context
        return null;
    }

}
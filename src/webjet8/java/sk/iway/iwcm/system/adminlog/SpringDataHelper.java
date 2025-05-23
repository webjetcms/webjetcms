package sk.iway.iwcm.system.adminlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import javax.persistence.EntityManager;

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
                if (entityManager.contains(entity)==false) {
                    //this will throw IllegalArgumentException if the entity is not managed
                    continue; // Skip to the next persistence unit
                }

                // Create a fresh EntityManager for the current persistence unit
                freshEntityManager = entityManager.getEntityManagerFactory().createEntityManager();

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
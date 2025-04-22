package sk.iway.iwcm.system.adminlog;

import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component
public class SpringDataHelper {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Fetches the real entity from the database using a fresh EntityManager.
     *
     * @param entity The entity class to fetch.
     * @param id     The ID of the entity.
     * @return The real entity from the database.
     */
    public Object getSpringDataEntity(Object entity, Long id) {
        EntityManager freshEntityManager = null;
        try {
            // Create a fresh EntityManager
            freshEntityManager = entityManager.getEntityManagerFactory().createEntityManager();

            // Fetch the entity from the database
            Object dbEntity = freshEntityManager.find(entity.getClass(), id);

            return dbEntity;
        } finally {
            // Ensure the fresh EntityManager is closed
            if (freshEntityManager != null && freshEntityManager.isOpen()) {
                freshEntityManager.close();
            }
        }
    }

}
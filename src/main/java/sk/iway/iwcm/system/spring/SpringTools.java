package sk.iway.iwcm.system.spring;

import org.eclipse.persistence.jpa.JpaEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.stereotype.Component;

@Component
public class SpringTools {

    @Autowired
    private JpaContext jpaContext;

    /**
     * Return JpaEntityManager for given class.
     * @param clazz
     * @return
     */
    public JpaEntityManager getSpringEntityManager(Class<?> clazz) {
        return (JpaEntityManager) jpaContext.getEntityManagerByManagedType(clazz);
    }
}
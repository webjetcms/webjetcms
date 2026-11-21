package sk.iway.iwcm.branch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.components.media.MediaGroupRepository;
import sk.iway.iwcm.components.media.MediaRepository;
import sk.iway.iwcm.components.perex_groups.PerexGroupDocEntity;
import sk.iway.iwcm.components.perex_groups.PerexGroupDocRepository;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.spirit.model.Media;
import sk.iway.spirit.model.MediaGroupBean;
import sk.iway.webjet.v9.V9JpaDBConfig;

/** Real Spring-managed repositories and transactions on the disposable branch database. */
abstract class BranchJpaTest extends BaseWebjetTest {
    private static AnnotationConfigApplicationContext persistence;
    private static Object previousSpring, previousFactories;
    private static ServletContext servletContext;

    @BeforeAll static void initializeBranchPersistence() throws Exception {
        try (var connection=DBPool.getConnection()) {
            if (!"webjet_p01".equals(connection.getCatalog()) || !connection.getMetaData().getURL().contains(":33316/"))
                throw new IllegalStateException("Branch persistence tests require the isolated P01 database");
        }
        servletContext=Constants.getServletContext();
        previousSpring=servletContext.getAttribute("springContext");
        previousFactories=ReflectionTestUtils.getField(DBPool.class,"entityManagerFactories");
        persistence=new AnnotationConfigApplicationContext(Persistence.class);
        installBranchPersistence();
        ReflectionTestUtils.setField(DBPool.class,"entityManagerFactories",Map.of("iwcm",persistence.getBean(EntityManagerFactory.class)));
    }

    protected static void installBranchPersistence() {
        Constants.getServletContext().setAttribute("springContext",persistence);
    }

    protected static BranchService branchService() { return persistence.getBean(BranchService.class); }

    protected static BranchEntity createBranch(String domain, String name, String description, int userId) {
        return createBranch(domain, name, description, BranchEntity.DEFAULT_COLOR, userId);
    }

    protected static BranchEntity createBranch(int domainId, String name, String description, int userId) {
        return createBranch(domainId, name, description, BranchEntity.DEFAULT_COLOR, userId);
    }

    protected static BranchEntity createBranch(String domain, String name, String description, String color, int userId) {
        return createBranch(BranchDomains.id(domain), name, description, color, userId);
    }

    /** Creates fixture metadata through JPA without exposing a second application create path. */
    private static BranchEntity createBranch(int domainId, String name, String description, String color, int userId) {
        try { BranchReadiness.verifyActivation(); }
        catch (SQLException ex) { throw new IllegalStateException(ex); }
        Timestamp now=Timestamp.from(Instant.now());
        BranchEntity branch=new BranchEntity();
        branch.setDomainId(domainId);branch.setName(name);branch.setDescription(description);branch.setColor(color);
        branch.setState("DRAFT");branch.setRevision(0L);
        branch.setCreatedBy(userId);branch.setUpdatedBy(userId);
        branch.setCreatedAt(now);branch.setUpdatedAt(now);
        return persistence.getBean(BranchRepository.class).saveAndFlush(branch);
    }

    @BeforeEach void injectBranchRepositories() { persistence.getAutowireCapableBeanFactory().autowireBean(this); }

    @AfterAll static void closeBranchPersistence() {
        sk.iway.iwcm.SetCharacterEncodingFilter.unRegisterDataContext();
        servletContext.setAttribute("springContext",previousSpring);
        ReflectionTestUtils.setField(DBPool.class,"entityManagerFactories",previousFactories);
        if (persistence!=null) persistence.close();
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass=true)
    static class Persistence {
        @Bean LocalValidatorFactoryBean validator() { return new LocalValidatorFactoryBean(); }

        @Bean("webjet2022EntityManager") LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            var factory=new V9JpaDBConfig().entityManagerFactory();
            factory.setPersistenceUnitPostProcessors(unit->{
                unit.getManagedClassNames().removeIf(name->Files.exists(Path.of("build/classes/java/test",name.replace('.','/')+".class")));
                unit.getManagedClassNames().add(sk.iway.iwcm.io.FileHistoryBean.class.getName());
            });
            return factory;
        }

        @Bean({"webjet2022TransactionManager","transactionManager"})
        JpaTransactionManager transactionManager(@Qualifier("webjet2022EntityManager") EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean JpaRepositoryFactoryBean<BranchRepository,BranchEntity,Long> branchRepository(EntityManagerFactory factory) {
            var repository=new JpaRepositoryFactoryBean<BranchRepository,BranchEntity,Long>(BranchRepository.class);
            repository.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(factory));
            return repository;
        }

        @Bean BranchService branchService(BranchRepository repository) { return new BranchService(repository); }

        @Bean JpaRepositoryFactoryBean<BranchFileRepository,BranchFileEntity,Long> branchFileRepository(EntityManagerFactory factory) {
            var repository=new JpaRepositoryFactoryBean<BranchFileRepository,BranchFileEntity,Long>(BranchFileRepository.class);
            repository.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(factory));
            return repository;
        }

        @Bean JpaRepositoryFactoryBean<PerexGroupDocRepository,PerexGroupDocEntity,Long> perexGroupDocRepository(EntityManagerFactory factory) {
            var repository=new JpaRepositoryFactoryBean<PerexGroupDocRepository,PerexGroupDocEntity,Long>(PerexGroupDocRepository.class);
            repository.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(factory));
            return repository;
        }

        @Bean JpaRepositoryFactoryBean<MediaGroupRepository,MediaGroupBean,Long> mediaGroupRepository(EntityManagerFactory factory) {
            var repository=new JpaRepositoryFactoryBean<MediaGroupRepository,MediaGroupBean,Long>(MediaGroupRepository.class);
            repository.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(factory));
            return repository;
        }

        @Bean JpaRepositoryFactoryBean<MediaRepository,Media,Long> mediaRepository(EntityManagerFactory factory) {
            var repository=new JpaRepositoryFactoryBean<MediaRepository,Media,Long>(MediaRepository.class);
            repository.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(factory));
            return repository;
        }
    }
}

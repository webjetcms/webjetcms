# Extension of Spring DATA repositories of WebJET CMS

In some cases, you may need to extend the options in your project `Spring DATA` repositories of existing entities. For example, when displaying a special news item where you need to display a list of `DocDetails` entities.

You have several options for implementation. This is a sample `REST controller` that shows the use of all the examples below:

```java
package sk.iway.web.news;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.OriginalPackageRepository;

import java.util.List;

@RestController
@RequestMapping("/test/news-controller/")
public class NewsController {

    /**
     * Repository in original package
     */
    @Autowired
    private OriginalPackageRepository originalPackageRepository;

    /**
     * Unmodified DocDetailsRepository with Specification query
     */
    @Autowired
    private DocDetailsRepository docDetailsRepository;

    /**
     * Custom repository with custom entity
     */
    @Autowired
    private NewsRepository newsRepository;

    @GetMapping("/original-package")
    public List<DocDetails> getDocNews() {
        return originalPackageRepository.findByTitleLike("%test%");
    }

    private static Specification<DocDetails> getByTitleOrExternalLink(String text) {
        return (root, query, criteriaBuilder) -> {
           return criteriaBuilder.or(
               criteriaBuilder.like(root.get("title"), text),
               criteriaBuilder.like(root.get("externalLink"), text)
           );
        };
    }

    @GetMapping("/unmodified-repository")
    public List<DocDetails> getDocNewsOrig() {
        return docDetailsRepository.findAll(getByTitleOrExternalLink("%test%"));
    }

    @GetMapping("/unmodified-repository-direct")
    public List<DocDetails> getDocNewsOrigDirect() {
        return docDetailsRepository.findAll((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("title"), "%test%"));
    }

    @GetMapping("/custom-repository")
    public List<NewsDocDetails> getNews() {
        return newsRepository.findByTitleLike("%test%");
    }

}
```

V `SpringConfig` a `JpaDBConfig` don't forget to add package `sk.iway.web.news`, or create examples in the package you configured/use.

## New repository in the original package

The simplest solution without additional configuration is to create a new `Spring DATA` repository in the same package as the standard WebJET repository (e.g. `sk.iway.iwcm.doc`). This will ensure that it is automatically initialized when WebJET starts.

!>**Warning:** you must not add this package to your `JpaDBConfig` because one entity (class) cannot be in multiple JPA configurations at the same time.

In the example a method is created `findByTitleLike` to find a list of web pages by name. The original `DocDetails` entity.

```java
package sk.iway.iwcm.doc;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OriginalPackageRepository extends JpaRepository<DocDetails, Long>, JpaSpecificationExecutor<DocDetails> {

   List<DocDetails> findByTitleLike(String title);

}
```

## Use `Specification`

Most repositories extend `JpaSpecificationExecutor` and thus it is possible to dynamically create a condition for data retrieval. In the above `NewsController` the standard (without your change) repository is used `DocDetailsRepository`. Using `criteriaBuilder` it is possible to build specific conditions for selecting data from the database, an example is in the method `getByTitleOrExternalLink`.

```java
public class NewsController {
    /**
     * Unmodified DocDetailsRepository with Specification query
     */
    @Autowired
    private DocDetailsRepository docDetailsRepository;

    private static Specification<DocDetails> getByTitleOrExternalLink(String text) {
        return (root, query, criteriaBuilder) -> {
           return criteriaBuilder.or(
               criteriaBuilder.like(root.get("title"), text),
               criteriaBuilder.like(root.get("externalLink"), text)
           );
        };
    }

    @GetMapping("/unmodified-repository")
    public List<DocDetails> getDocNewsOrig() {
        return docDetailsRepository.findAll(getByTitleOrExternalLink("%test%"));
    }
}
```

In the case of a simple condition, you can also use direct notation `Specification` when calling `findAll`:

```java
    @GetMapping("/unmodified-repository-direct")
    public List<DocDetails> getDocNewsOrigDirect() {
        return docDetailsRepository.findAll((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("title"), "%test%"));
    }
```

## Creating your own repository and entity

For some cases it is advisable to create a custom repository and custom entity (it is not possible to use the original entity from WebJET CMS due to different `TransactionManager`). This is also suitable for the case when you do not need to transfer all the data and thus this solution partially replaces DTO objects.

How to create the entity first, you can use similar code as in the original entity, in our example we only transfer the column `title`. Technically there is no problem in that for the table `documents` another entity already exists:

```java
package sk.iway.web.news;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "documents")
public class NewsDocDetails {

	@Id
	@Column(name = "doc_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator="S_documents")
	protected Long id;

	private String title;
}
```

The repository itself is standard:

```java
package sk.iway.web.news;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<NewsDocDetails, Long>, JpaSpecificationExecutor<NewsDocDetails> {

    List<NewsDocDetails> findByTitleLike(String title);

}
```

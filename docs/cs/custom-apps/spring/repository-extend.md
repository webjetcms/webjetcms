# Rozšíření Spring DATA repozitářů WebJET CMS

V některých případech budete ve vašem projektu potřebovat rozšířit možnosti `Spring DATA` repozitářů stávajících entit. Například při zobrazení speciálních novinek, kde potřebujete zobrazit seznam `DocDetails` entit.

Máte několik možností realizace. Toto je ukázkový `REST controller`, který ukazuje použití všech níže uvedených příkladů:

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

V `SpringConfig` a `JpaDBConfig` nezapomeňte přidat package `sk.iway.web.news`, nebo příklady vytvořte ve vámi nakonfigurovaném/používaném package.

## Nový repozitář v originálním package

Nejjednodušší řešení bez dodatečné konfigurace je vytvořit nový `Spring DATA` repozitář v package stejném jako je standardní WebJET repozitář (např. `sk.iway.iwcm.doc`). To zajistí jeho automatickou inicializaci při startu WebJETu.

!>**Upozornění:** tento package nesmíte přidat do vašeho `JpaDBConfig`, protože jedna entita (třída) nemůže být ve více JPA konfiguracích najednou.

V příkladu je vytvořena metoda `findByTitleLike` pro vyhledání seznamu web stránek podle názvu. Používají se původní `DocDetails` entity.

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

## Použití `Specification`

Většina repozitářů rozšiřuje `JpaSpecificationExecutor` a lze tedy dynamicky vytvořit podmínku pro získání dat. V výše uvedeném `NewsController` je použit standardní (bez vaší změny) repozitář `DocDetailsRepository`. S využitím `criteriaBuilder` lze sestavit specifické podmínky pro výběr dat z databáze, příklad je v metodě `getByTitleOrExternalLink`.

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

V případě jednoduché podmínky můžete použít i přímý zápis `Specification` při volání `findAll`:

```java
    @GetMapping("/unmodified-repository-direct")
    public List<DocDetails> getDocNewsOrigDirect() {
        return docDetailsRepository.findAll((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("title"), "%test%"));
    }
```

## Vytvoření vlastního repozitáře a entity

Pro některé případy je vhodné vytvořit vlastní repozitář a vlastní entitu (nelze použít původní entitu z WebJET CMS z důvodu rozdílného `TransactionManager`). To je vhodné i pro případ, kdy nepotřebujete přenášet všechny údaje a tedy částečně toto řešení nahrazuje DTO objekty.

Jako první vytvořte entitu, můžete použít podobný kód jako v původní entitě, v našem příkladu přenášíme pouze sloupec `title`. Technicky není problém v tom, že pro tabulku `documents` již existuje jiná entita:

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

Samotný repozitář je standardní:

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

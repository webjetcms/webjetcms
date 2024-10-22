# Rozšíření úložišť Spring DATA systému WebJET CMS

V některých případech může být nutné rozšířit možnosti v projektu. `Spring DATA` úložiště existujících entit. Například při zobrazování speciální novinky, kde je třeba zobrazit seznam. `DocDetails` subjekty.

Máte několik možností implementace. Toto je ukázka `REST controller` který ukazuje použití všech níže uvedených příkladů:

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

V `SpringConfig` a `JpaDBConfig` nezapomeňte přidat balíček `sk.iway.web.news`, nebo vytvořit příklady v konfigurovaném/používaném balíčku.

## Nové úložiště v původním balíčku

Nejjednodušším řešením bez dodatečné konfigurace je vytvoření nové položky `Spring DATA` úložiště ve stejném balíčku jako standardní úložiště WebJET (např. `sk.iway.iwcm.doc`). Tím zajistíte, že se automaticky inicializuje při spuštění WebJETu.

**Varování:** nesmíte tento balíček přidávat do `JpaDBConfig` protože jedna entita (třída) nemůže být současně ve více konfiguracích JPA.

V příkladu je vytvořena metoda `findByTitleLike` vyhledat seznam webových stránek podle názvu. Původní `DocDetails` entita.

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

## Použijte `Specification`

Většina úložišť rozšiřuje `JpaSpecificationExecutor` a je tak možné dynamicky vytvořit podmínku pro načtení dat. Ve výše uvedeném `NewsController` je použito standardní úložiště (bez vaší změny). `DocDetailsRepository`. Použití `criteriaBuilder` je možné vytvořit specifické podmínky pro výběr dat z databáze, příkladem je metoda `getByTitleOrExternalLink`.

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

V případě jednoduché podmínky můžete také použít přímý zápis `Specification` při volání `findAll`:

```java
    @GetMapping("/unmodified-repository-direct")
    public List<DocDetails> getDocNewsOrigDirect() {
        return docDetailsRepository.findAll((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("title"), "%test%"));
    }
```

## Vytvoření vlastního úložiště a entity

Pro některé případy je vhodné vytvořit vlastní úložiště a vlastní entitu (není možné použít původní entitu ze systému WebJET CMS kvůli odlišným podmínkám). `TransactionManager`). To je vhodné i pro případ, kdy nepotřebujete přenášet všechna data a toto řešení tak částečně nahrazuje objekty DTO.

Jak nejprve vytvořit entitu, můžete použít podobný kód jako v původní entitě, v našem příkladu pouze přeneseme sloupec `title`. Z technického hlediska to pro tabulku není žádný problém. `documents` již existuje jiná entita:

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

Samotné úložiště je standardní:

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

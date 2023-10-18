# Rozšírenie Spring DATA repozitárov WebJET CMS

V niektorých prípadoch budete vo vašom projekte potrebovať rozšíriť možnosti Spring DATA repozitárov existujúcich entít. Napríklad pri zobrazení špeciálnych noviniek, kde potrebujete zobraziť zoznam ```DocDetails``` entít.

Máte niekoľko možností realizácie. Toto je ukážkový REST controller, ktorý ukazuje použitie všetkých nižšie uvedených príkladov:

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

V ```SpringConfig``` a ```JpaDBConfig``` nezabudnite pridať package ```sk.iway.web.news```, alebo príklady vytvorte vo vami nakonfigurovanom/používanom package.

## Nový repozitár v originálnom package

Najjednoduchšie riešenie bez dodatočnej konfigurácie je vytvoriť nový Spring DATA repozitár v package rovnakom ako je štandardný WebJET repozitár (napr. ```sk.iway.iwcm.doc```). To zabezpečí jeho automatickú inicializáciu pri štarte WebJETu.

UPOZORNENIE: tento package nesmiete pridať do vášho ```JpaDBConfig```, pretože jedna entita (trieda) nemôže byť vo viacerých JPA konfiguráciách naraz.

V príklade je vytvorená metóda ```findByTitleLike``` pre vyhľadanie zoznamu web stránok podľa názvu. Používajú sa pôvodné ```DocDetails``` entity.

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

## Použitie ```Specification```

Väčšina repozitárov rozširuje ```JpaSpecificationExecutor``` a je teda možné dynamicky vytvoriť podmienku pre získanie dát. V hore uvedenom ```NewsController``` je použitý štandardný (bez vašej zmeny) repozitár ```DocDetailsRepository```. S využitím ```criteriaBuilder``` je možné zostaviť špecifické podmienky pre výber dát z databázy, príklad je v metóde ```getByTitleOrExternalLink```.

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

V prípade jednoduchej podmienky môžete použiť aj priamy zápis ```Specification``` pri volaní ```findAll```:

```java
    @GetMapping("/unmodified-repository-direct")
    public List<DocDetails> getDocNewsOrigDirect() {
        return docDetailsRepository.findAll((root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("title"), "%test%"));
    }
```

## Vytvorenie vlastného repozitára a entity

Pre niektoré prípady je vhodné vytvoriť vlastný repozitár a vlastnú entitu (nie je možné použiť pôvodnú entitu z WebJET CMS z dôvodu rozdielneho ```TransactionManager```). To je vhodné aj pre prípad, kedy nepotrebujete prenášať všetky údaje a teda čiastočne toto riešenie nahrádza DTO objekty.

Ako prve vytvorte entitu, môžete použiť podobný kód ako v pôvodnej entite, v našom príklade prenášame len stĺpec ```title```. Technicky nie je problém v tom, že pre tabuľku ```documents``` už existuje iná entita:

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

Samotný repozitár je štandardný:

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

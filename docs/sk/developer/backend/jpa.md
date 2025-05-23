# Práca s JPA

Naše postrehy a návody k JPA.

## Dedenie v JPA entitách

V prípade ak dve alebo viaceré tabuľky obsahujú väčšie množstvo rovnakých stĺpcov, ktoré nechceme deklarovať viacnásobne, môžeme použiť dedenie (Inheritance). Príklad je tabuľka ```documents``` a ```documents_history```, ktoré majú väčšinu stĺpcov zhodných, ale chceme s nimi pracovať ako s dvoma nezávislými entitami.

Vytvoríme rodičovskú triedu, v ktorej budeme uchovávať spoločné stĺpce (môžeme ju pomenovať napríklad basic). Táto trieda musí obsahovať anotáciu ```@MappedSuperclass``` (dedenie pomocu anotácie ```@Inheritance``` nie je pre tento typ dedenia vhodná).

```java
@MappedSuperclass
public class DocBasic implements DocGroupInterface, Serializable
{
```

Dôležité je aby sme v rodičovskej triede nepoužili žiadne Lombok anotácie ako ```@Getter / @Setter``` takže všetky metódy je potrebné napísať ručne. JPA nie je na takéto dedenie metód prispôsobený čo by spôsobilo pád kódu na chybe ```NoSuchMethodError (Ljava/lang/String;)V```. Triedy ktoré dedia od takejto triedy s anotáciou ```@MappedSuperclass``` nepotrebujú okrem rozšírenia žiadne iné úpravy alebo anotácie. Metódy rodičovskej triedy je možné prepísať.


```java
@Entity
@Table(name = "documents")
@Getter
@Setter
public class DocDetails extends DocBasic {
```
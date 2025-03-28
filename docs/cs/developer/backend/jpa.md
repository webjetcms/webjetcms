# Práce s JPA

Naše postřehy a návody k JPA.

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

## Dědění v JPA entitách

V případě, že dvě nebo více tabulek obsahují větší množství stejných sloupců, které nechceme deklarovat vícenásobně, můžeme použít dědění (Inheritance). Příklad je tabulka `documents` a `documents_history`, které mají většinu sloupců shodných, ale chceme s nimi pracovat jako se dvěma nezávislými entitami.

Vytvoříme rodičovskou třídu, ve které budeme uchovávat společné sloupce (můžeme ji pojmenovat například basic). Tato třída musí obsahovat anotaci `@MappedSuperclass` (dědění pomoci anotace `@Inheritance` není pro tento typ dědění vhodná).

```java
@MappedSuperclass
public class DocBasic implements DocGroupInterface, Serializable
{
```

Důležité je abychom v rodičovské třídě nepoužili žádné Lombok anotace jako `@Getter / @Setter` takže všechny metody je třeba napsat ručně. JPA není na takové dědění metod přizpůsoben, což by způsobilo pád kódu na chybě. `NoSuchMethodError (Ljava/lang/String;)V`. Třídy které dědí od takové třídy s anotací `@MappedSuperclass` nepotřebují kromě rozšíření žádné jiné úpravy nebo anotace. Metody rodičovské třídy lze přepsat.

```java
@Entity
@Table(name = "documents")
@Getter
@Setter
public class DocDetails extends DocBasic {
```

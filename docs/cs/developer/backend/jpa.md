# Práce s JPA

Naše postřehy a pokyny týkající se společného parlamentního shromáždění.

<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

## Dědění v entitách JPA

Pokud dvě nebo více tabulek obsahují větší počet stejných sloupců, které nechceme deklarovat vícekrát, můžeme použít dědičnost. Příkladem je tabulka `documents` a `documents_history` které mají většinu sloupců shodných, ale chceme s nimi pracovat jako se dvěma nezávislými entitami.

Vytvoříme nadřazenou třídu, do které budeme ukládat společné sloupce (můžeme ji pojmenovat například basic). Tato třída musí obsahovat anotaci `@MappedSuperclass` (dědičnost pomocí anotace `@Inheritance` není pro tento typ dědictví vhodný).

```java
@MappedSuperclass
public class DocBasic implements DocGroupInterface, Serializable
{
```

Je důležité, abychom v rodičovské třídě nepoužívali žádné anotace Lombok, protože `@Getter / @Setter` takže všechny metody je třeba napsat ručně. JPA není uzpůsoben pro takovou dědičnost metod, která by při chybě způsobila pád kódu. `NoSuchMethodError (Ljava/lang/String;)V`. Třídy, které dědí od takové třídy s anotací `@MappedSuperclass` nepotřebují žádné úpravy ani poznámky kromě rozšíření. Metody nadřazené třídy lze přepsat.

```java
@Entity
@Table(name = "documents")
@Getter
@Setter
public class DocDetails extends DocBasic {
```

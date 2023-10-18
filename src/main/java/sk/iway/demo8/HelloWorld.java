package sk.iway.demo8;

public interface HelloWorld {
    default String hello() {
        return "world";
    }
}
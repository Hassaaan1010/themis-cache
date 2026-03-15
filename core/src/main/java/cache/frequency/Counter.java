package cache.frequency;

public interface  Counter {

    void increment(String key);

    void decay();

    short getCount(String key);
}

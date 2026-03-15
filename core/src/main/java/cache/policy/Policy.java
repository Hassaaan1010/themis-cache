package cache.policy;


public interface Policy {

    // String evict(Map<String, ByteString> cache);
    // String set(Map<String, ByteString> cache, );
    // Set should not be part of Policy's responsiblity as set will only interact with intra tenant keys and metrics



    void Rebalance();
} 
package cache.policy;

import java.util.ArrayList;
import java.util.Map;

public interface Policy {

    // String evict(Map<String, ByteString> cache);
    // String set(Map<String, ByteString> cache, );
    // Set should not be part of Policy's responsiblity as set will only interact with intra tenant keys and metrics
    

    void redistribute();

    Map<String,ArrayList<String>> getTenantEvictablesMap();
} 
package cache;

import java.util.HashMap;
import java.util.Map;
import com.google.protobuf.ByteString;

import cache.policy.EvictionDecider;

public class Cache {

    private final Map<String, ByteString> cache;
    private final EvictionDecider evictionPolicy; 

    // private final metricTracker;

    public Cache(EvictionDecider policy) {
        this.cache = new HashMap<String, ByteString>();
        this.evictionPolicy = policy;
    }

    public void put(String key, ByteString value) {
        evictionPolicy.evict(this.cache);
        cache.put(key, value);
    }

    public ByteString get(String key) {
        return cache.get(key);
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public int size() {
        return cache.size();
    }
}

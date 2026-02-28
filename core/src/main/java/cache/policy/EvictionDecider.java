package cache.policy;

import java.util.Map;

import com.google.protobuf.ByteString;

public interface EvictionDecider {

    String evict(Map<String, ByteString> cache);
} 
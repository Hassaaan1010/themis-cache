package cache.policy;

import java.util.Map;

import com.google.protobuf.ByteString;


public class FairCachePolicy implements EvictionDecider {

    @Override
    public String evict(Map<String, ByteString> cache){
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'evict'");
    }
    
}

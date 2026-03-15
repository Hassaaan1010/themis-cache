package cache.demand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ByteString;

public class DemandTracker {
    
    private final Map<String, ArrayList<Integer>> demandMap;

    public DemandTracker() {
        this.demandMap = new HashMap<>();
    }


    public void getFail(String key) {

    }

    public void setFail(String key, ByteString value) {

    }
    
    public Map<String, ArrayList<Integer>> getDemandMap() {
        return demandMap;
    }

    


}

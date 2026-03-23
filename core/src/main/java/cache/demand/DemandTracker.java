package cache.demand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ByteString;

public class DemandTracker {

    /**
     * This map use to calculate demand of each tenant at end of window Reset to
     * new map after rebalancing
     *
     * <T> can not be Short because size of 32767 is not out cap.
     */
    private Map<String, ArrayList<Integer>> demandMap;

    public DemandTracker() {
        this.demandMap = new HashMap<>();
    }

    public void getFail(String key) {
        ArrayList<Integer> metrics = demandMap.get(key);

        if (metrics == null) {
            // Initialize metrics as [1, 0]
            /**
             * Frequency set to 1 and size to 0. If tenant actually needs it,
             * they will try to set value. Without value, area can not be
             * assumed
             */
            metrics = new ArrayList<>();
            metrics.add(1); // frequency
            metrics.add(0);

            demandMap.put(key, metrics);
        } else {
            metrics.set(0, metrics.get(0) + 1);
        }
    }

    public void setFail(String key, ByteString value) {

        ArrayList<Integer> metrics = demandMap.get(key);

        if (metrics == null) {

            metrics = new ArrayList<>();
            metrics.add(1); // Frequency
            metrics.add(value.size()); // Size

            demandMap.put(key, metrics);

        } else {
            metrics.set(0, metrics.get(0) + 1);
            metrics.set(1, Math.max(metrics.get(1), value.size())); // Max size for accomodating largest demand possible. This can not be exploited due to fairness guarantee
        }

    }

    public void stopTracking(String key) {
        demandMap.remove(key);
    }

    public Short getFrequency(String key) {
        Integer freq = this.demandMap.get(key).get(0);
        Short result = null;

        if (freq >= Short.MAX_VALUE) {
            result = Short.MAX_VALUE;
        } else if (freq != null) {
            result = freq.shortValue();
        }

        return result;
    }

    public ArrayList<Integer> getMetrics(String key) {
        ArrayList<Integer> result = demandMap.get(key);

        return result;
    }

    public Map<String, ArrayList<Integer>> getDemandMap() {
        return Collections.unmodifiableMap(this.demandMap);
    }

    public void resetDemandMap() {
        this.demandMap = new HashMap<>();
    }

}

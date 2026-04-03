package cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.google.protobuf.ByteString;

import cache.demand.DemandTracker;
import cache.frequency.CountMinSketch;
import cache.frequency.Counter;
import cache.utils.Entry;
import commonCore.CoreConstants;
import tenants.Tenant;

public class Cache {

    private final Map<String, Entry> cache;
    // private final metricTracker;

    private final Counter frequencyCounter;
    private final DemandTracker demandTracker;

    private final int sampleSize;
    private final int maxBoundedEvictions;
    private final ArrayList<Entry> entries;

    private final Tenant tenant;

    public Cache(Tenant tenant) {
        this.tenant = tenant;
        this.cache = new HashMap<>();
        this.frequencyCounter = new CountMinSketch(CoreConstants.GLOBAL_EPSILON, CoreConstants.GLOBAL_DELTA);
        this.demandTracker = new DemandTracker();
        this.sampleSize = CoreConstants.EVICTION_SAMPLE_SIZE;
        this.maxBoundedEvictions = CoreConstants.MAX_BOUNDED_EVICTIONS;
        this.entries = new ArrayList<>();
    }

    /**
     * Try bounded random sample eviction for key based on frequency.
     */
    private boolean tryMakingSpace(String candidateKey, long spaceNeeded) throws Exception {

        /**
         * If key was in Cache, then it will not have freq in demand tracker, and cached
         * keys will have cms frequency.
         */
        // Get candidateKey's count
        Short candidateKeyFrequency;

        candidateKeyFrequency = demandTracker.getFrequency(candidateKey);
        if (candidateKeyFrequency == null) {
            candidateKeyFrequency = this.frequencyCounter.getCount(candidateKey);
        }

        int failures = 0;

        for (int attempt_i = 0; attempt_i < maxBoundedEvictions; attempt_i++) {

            String victim = candidateKey;
            short victimFrequency = candidateKeyFrequency;

            for (int sample_j = 0; sample_j < sampleSize; sample_j++) {
                int randomIndex = ThreadLocalRandom.current().nextInt(this.entries.size());
                String existingKey = entries.get(randomIndex).key();
                short existingKeyFrequency = this.frequencyCounter.getCount(existingKey);

                if (existingKeyFrequency < victimFrequency) {
                    victim = existingKey;
                    victimFrequency = existingKeyFrequency;
                }
            }

            if (!victim.equals(candidateKey)) {
                remove(victim);
                if (tenant.getAvailable() >= spaceNeeded)
                    return true;
                failures = 0; // reset
            } else {
                failures++;
                if (failures >= 2)
                    return false; // tolerate some bad luck
            }
        }

        // Sorry, maybe next time. (Unless someone else has already eaten up your
        // efforts lol)
        return false;

    }

    public boolean set(String key, ByteString newValue) throws Exception {

        // If already exists, update.
        Entry existingEntry = this.cache.get(key);
        long available = tenant.getAvailable();
        boolean success = true;

        // if (EchoServer.DEBUG_SERVER) LogUtil.log("//////// Size of payload: ", newValue.size());

        // If already exists
        if (existingEntry != null) {

            long sizeDiff = existingEntry.value().size() - newValue.size();

            // New value is smaller or equal
            if (sizeDiff >= 0) {
                // Add diff to quota
                tenant.returnAvailable(sizeDiff);

                // Replace value
                existingEntry.setValue(newValue);

            } else {

                // Invert subtraction
                sizeDiff = -1 * sizeDiff;

                // If available can still fit has enough to replace directly
                if (sizeDiff < available) {
                    // use available
                    tenant.useAvailable(sizeDiff);

                    // Replace value
                    existingEntry.setValue(newValue);

                } else {
                    // Try bounded eviction
                    boolean evictionSuccess = tryMakingSpace(key, sizeDiff);

                    if (evictionSuccess) {
                        // Update available
                        tenant.useAvailable(sizeDiff);

                        // Replace value
                        existingEntry.setValue(newValue);

                    } else {
                        // Mark fail
                        demandTracker.setFail(key, newValue);

                        // Remove to prevent stale reads
                        remove(key);

                        success = false;
                    }
                }
            }
        }
        // New entry
        else {
            // If available is enough
            if (available >= newValue.size()) {
                // Use available
                tenant.useAvailable(newValue.size());

                Entry newEntry = new Entry(key, newValue);

                // Add to entries
                entries.add(newEntry);

                // Cache entry
                this.cache.put(key, newEntry);

            }
            // If available not enough
            else {
                // Try bounded eviction
                boolean evictionSuccess = tryMakingSpace(key, newValue.size());

                if (evictionSuccess) {
                    // Use available
                    tenant.useAvailable(newValue.size());

                    Entry newEntry = new Entry(key, newValue);

                    // Add to entries
                    entries.add(newEntry);

                    // Cache entry
                    this.cache.put(key, newEntry);

                } else {
                    // Mark fail
                    demandTracker.setFail(key, newValue);

                    success = false;
                }
            }
        }

        if (success) {
            // Incase this was being tracked as demand
            demandTracker.stopTracking(key);

            // Always increment on success
            frequencyCounter.increment(key);
        }

        return success;
    }

    // @Deprecated
    // private void successfulSet(String key, ByteString val, Entry existingEntry,
    // int sizeDiff) throws Exception {
    // if (existingEntry == null) {
    // // Use available
    // tenant.useAvailable(val.size());

    // Entry newEntry = new Entry(key, val);

    // // Add to entries
    // entries.add(newEntry);

    // // Cache entry
    // this.cache.put(key, newEntry);

    // } else {
    // // Add diff to quota
    // tenant.useAvailable(sizeDiff);

    // // Replace value
    // existingEntry.setValue(val);

    // }
    // }

    public ByteString get(String key) {
        Entry entry = cache.get(key);

        if (entry == null) {
            
            demandTracker.getFail(key);
            return null;
        }

        ByteString val = entry.value();

        if (val == null) {
            demandTracker.getFail(key);
            return null;
        } else {
            // Increment counter
            this.frequencyCounter.increment(key);
            return val;
        }
    }

    // Removes key from cache. Removes from entries and replaces it with last entry.
    public void remove(String key) throws Exception {

        Entry removedEnt = cache.remove(key);
        if (removedEnt == null) // Key not found
            return;

        // Return space
        tenant.returnAvailable(removedEnt.value().size());

        int removeIndex = removedEnt.index;
        int lastIndex = entries.size() - 1;

        if (lastIndex < 0)
            return;

        Entry lastEnt = entries.remove(lastIndex);

        // If removed last was from last, then dont have to fill gap
        if (removeIndex != lastIndex) {
            lastEnt.index = removeIndex;
            entries.set(removeIndex, lastEnt);
        }

        this.demandTracker.stopTracking(key);
    }

    public int cacheSize() {
        return cache.size();
    }

    public int getKeySize(String key) {
        return this.cache.get(key).size();
    }

    public Set<String> getKeySet() {
        return this.cache.keySet();
    }

    public DemandTracker getDemandTracker() {
        return demandTracker;
    }

    public Counter getFrequencyCounter() {
        return frequencyCounter;
    }

}

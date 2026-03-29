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
         * If key was in Cache, then it will not have freq in demand tracker, and cached keys will have cms frequency.
        */
        // Get candidateKey's count
        Short candidateKeyFrequency;

        candidateKeyFrequency = demandTracker.getFrequency(candidateKey);
        if (candidateKeyFrequency == null) {
            candidateKeyFrequency = this.frequencyCounter.getCount(candidateKey);
        }

        for (int attempt_i = 0; attempt_i < maxBoundedEvictions; attempt_i++) {

            // Start with skepticism against candidate
            String victim = candidateKey;
            short victimFrequency = candidateKeyFrequency;

            for (int sample_j = 0; sample_j < sampleSize; sample_j++) {
                int randomIndex = ThreadLocalRandom.current().nextInt(this.entries.size());

                // Get key from entries
                String existingKey = entries.get(randomIndex).key();

                // Get count from frequency coutner
                short existingKeyFrequency = this.frequencyCounter.getCount(existingKey);

                /**
                 * For per-byte-hotness comparision instead of raw frequency, use byteHotness = frequency / size
                 * For effeciency comparision use effeciency = frequency * size. ~ as per FairCache, where effeciency is throughput
                */
                // Compare frequency. 
                if (existingKeyFrequency < victimFrequency) {
                    victim = existingKey;
                    victimFrequency = existingKeyFrequency;
                }
            }

            // If candidate wasn't worst
            if (!victim.equals(candidateKey)) {
                // Evict worst;
                remove(victim);

                // Is there enough available yet? 
                if (tenant.getAvailable() >= spaceNeeded) {
                    return true;
                }
            } // Candidate is worst in first set of N random samples. Don't cache, fail
            else if (attempt_i == 1) {
                return false;
            }

        }

        // Sorry, maybe next time. (Unless someone else has already eaten up your efforts lol)
        return false;

    }

    public boolean set(String key, ByteString newValue) throws Exception {

        // If already exists, update.
        Entry existingEntry = this.cache.get(key);
        long available = tenant.getAvailable(); 
        boolean success = true;       

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
            if (available > newValue.size()) {
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
    // private void successfulSet(String key, ByteString val, Entry existingEntry, int sizeDiff) throws Exception {
    //     if (existingEntry  == null) {
    //         // Use available
    //         tenant.useAvailable(val.size());

    //         Entry newEntry = new Entry(key, val);

    //         // Add to entries
    //         entries.add(newEntry);

    //         // Cache entry
    //         this.cache.put(key, newEntry);

    //     } else {
    //          // Add diff to quota
    //         tenant.useAvailable(sizeDiff);

    //         // Replace value
    //         existingEntry.setValue(val);

    //     }
    // }
    

    public ByteString get(String key) {
        Entry entry = cache.get(key);
        
        if (entry == null) {
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

        // Remove candidate from cache
        Entry removedEnt = cache.remove(key);

        // Free up availability
        tenant.returnAvailable(removedEnt.value().size());

        // Remove last element from entries as replacement
        Entry lastEnt = entries.remove(-1);

        // Update replacement's index as candidate's index
        lastEnt.index = removedEnt.index;

        // Set replacement in place of candidate's index
        entries.set(removedEnt.index, lastEnt);

        // Remove from demand tracking
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

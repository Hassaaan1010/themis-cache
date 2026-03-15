package cache;

import commonCore.CoreConstants;

/** This class is to be accessed only for allocation and deallocation by a Policy during re-balancing
 * All Executables only interact with their own available memory.   
 */ 
public class MemoryManager {
    private final long maxBytes;
    private long usedBytes;

    public MemoryManager() {
        this.maxBytes = CoreConstants.TOTAL_CACHE_SIZE;
        this.usedBytes = 0;
    }

    public boolean tryAllocate(int objectSize) {
        if (usedBytes + objectSize > maxBytes) {
            return false;
        }

        this.usedBytes += objectSize;
        return true;
    }

    public void free(int objectSize) {
        this.usedBytes -= objectSize;
    }

    public long remaining() {
        return maxBytes - usedBytes;
    }

    public long used() {
        return usedBytes;
    }
    
}

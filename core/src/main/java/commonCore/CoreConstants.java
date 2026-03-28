package commonCore;


public class CoreConstants {
    private CoreConstants() {}  // prevent constructions

    // public static final 
    public static final int MAX_FRAME_LENGTH = 1024*1024; //1MB
    public static final int MAX_CONNECTIONS_PER_TENANT = 100;
    public static final int TOTAL_CACHE_SIZE = 1024*1024 * 500; // 500MB 

    public static final int QUEUE_CAPACITY = 1000;

    public static final float GLOBAL_EPSILON = (float) 0.01;
    public static final float GLOBAL_DELTA = (float) 0.01;


    public static final int MAX_BOUNDED_EVICTIONS = 4; // TODO: Could potentially be attached to a coeffeceint of size. Larger vals may want more trials of bounded eviction....
    public static final int EVICTION_SAMPLE_SIZE = 10; // 

    public static final int AMORTIZATION_WINDOW = 500;
    public static final int REBALANCING_WINDOW = 1 * 60 * 1000; // in milli seconds
    // public static final int DECAYING_WINDOW; // Decaying be part of rebalancing (as sep thread?)

    

    /**
    For a tenant k:v pair to be considered in part of demanded size, this threshold value is held against access frequency of the key. 

    We may instead use efficiency of file ( freq * size ). 

    higher threshold frequency = more cycles to acheive fairness.
    **/
    public static final short THRESHOLD_FREQUENCY = 100; 
    
    public static final double UNPREMPTABLE_PERCENTAGE = 0.2;
    
}


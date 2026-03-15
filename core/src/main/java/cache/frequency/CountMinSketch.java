package cache.frequency;

import java.util.concurrent.ThreadLocalRandom;

public class CountMinSketch implements Counter {

    /**
     * ε controls how wrong the counter is allowed to be. Count-Min Sketch
     * always overestimates counts because of hash collisions. ε sets the
     * maximum overestimation relative to the total number of updates.
     * estimated_count ≤ true_count + ε × total_updates width w = ceil(e / ε) 
     * ε        width 
     * 0.1	    ~27
     * 0.01	    ~272
     * 0.001	~2718
     */
    private final float epsilon_maxWrongness;

    /**
     * δ controls how often the guarantee might fail. probability(error > ε ×
     * total_updates) ≤ δ depth d = ceil(ln(1 / δ)) 
     * δ	    depth 
     * 0.1	    3 
     * 0.01	    5 
     * 0.001	7
     */
    private final float delta_violationChance;

    private final int width;
    private final int depth;
    private final short[][] table;
    
    private final int[] seeds;

    public CountMinSketch(float epsilon, float delta) {

        this.epsilon_maxWrongness = epsilon;
        this.delta_violationChance = delta;

        this.width = (int) Math.ceil(Math.E / this.epsilon_maxWrongness);
        this.depth = (int) Math.ceil(Math.log(1.0 / this.delta_violationChance));

        this.table = new short[depth][width];

        this.seeds = new int[depth];
        for (int i = 0; i < depth; i++) {
            seeds[i] = ThreadLocalRandom.current().nextInt();
        }
    }

    @Override
    public short getCount(String key) {
        //start with infinite min, for  ith seed, get hash , w = hash % width, get short[i][w], update min return min

        // Local final references for better inlining
        final short[][] tbl = this.table;
        final int[] seedsList = this.seeds;
        final int w = this.width;
        final int d = this.depth;

        final int baseHash = key.hashCode();

        short min = Short.MAX_VALUE;

        for (int i = 0; i < d; i++) {

            int h = Hasher.murmurHash32(baseHash, seedsList[i]);
            int idx = (h & Integer.MAX_VALUE) % w;

            short val = tbl[i][idx];
            if (val < min) {
                min = val;
            }
        }

        return min;
    }


    @Override
    public void increment(String key) {
        // for  ith seed, get hash , w = hash % width, increment short[i][w] by 1

        final short[][] tbl = this.table;
        final int[] seedsList = this.seeds;
        final int w = this.width;
        final int d = this.depth;

        final int baseHash = key.hashCode();

        for (int i = 0; i < d; i++) {

            int h = Hasher.murmurHash32(baseHash, seedsList[i]);
            int idx = (h & Integer.MAX_VALUE) % w;

            short val = tbl[i][idx];
            if (val != Short.MAX_VALUE) {
                tbl[i][idx] = (short) (val + 1);
            }
        }
    }

    // 10_000 width 10 depth, time taken < 1ms 
    @Override
    public void decay() {

        final short[][] tbl = this.table;
        final int d = this.depth;
        final int w = this.width;

        for (int i = 0; i < d; i++) {
            short[] row = tbl[i];

            for (int j = 0; j < w; j++) {
                row[j] = (short) (row[j] >>> 1);
            }
        }
    }

}

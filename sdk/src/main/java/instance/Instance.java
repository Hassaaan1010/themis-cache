package instance;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import com.google.protobuf.ByteString;

import client.EchoClient;
import common.LogUtil;
import common.parsing.protos.ResponseProtos.Response;

/**
 * FairCache 2-Tenant Workload Simulator — 5 windows × 30 seconds.
 *
 * Run:
 *   java -Denv.file=.env.tenant1 -Dtenant.id=1 -jar target/sdk-0.1-SNAPSHOT-jar-with-dependencies.jar
 *   java -Denv.file=.env.tenant2 -Dtenant.id=2 -jar target/sdk-0.1-SNAPSHOT-jar-with-dependencies.jar
 *
 * Setup:
 *   Total cache  = 100 MB
 *   Fair shares  = 50 MB : 50 MB
 *   File size    = 5 MB  (5_000_000 bytes, files file_0001.bin to file_0040.bin)
 *   Threshold    = 7     (must match CoreConstants.THRESHOLD_FREQUENCY)
 *   Frequency decay = halving each redistribution event
 *
 * Scenario summary:
 *
 *   W1  T1: fill 50MB hot (f01-f10),  demand 40MB (f11-f18, SET+GET×9)
 *       T2: fill 10MB hot (f01-f02),  leave 40MB cold (unused)
 *       → redistribution: T1 alloc=90MB, T2 alloc=10MB
 *
 *   W2  T1: recover f01-f10 (5 GETs), fill f11-f18 into new 40MB (SET+GET×9)
 *       T2: recover f01-f02 (5 GETs)
 *       → no demand → no redistribution. Debt: T1=+40, T2=-40
 *
 *   W3  T1: maintain all 18 files (5 GETs each)
 *       T2: recover f01-f02 (5 GETs), demand 80MB (f03-f18, SET+GET×9)
 *       → redistribution: T2 lender gets 80MB. T1=10MB, T2=90MB
 *
 *   W4  T1: re-establish 10MB (f01-f02, SET+GET×9)
 *       T2: recover f01-f02, fill f03-f18 into 80MB (SET+GET×9)
 *       → no demand → no redistribution. Debt: T1=+40, T2=-40
 *
 *   W5  T1: maintain f01-f02 (5 GETs)
 *       T2: maintain all 18 files (5 GETs each)
 *       → both debts cancel. System back to fair.
 *
 * Timing (worst case window):
 *   T1-W1: 10 SET + 90 GET + 8 SET + 72 GET = 180 ops × 100ms = 18s < 30s  OK
 *   T2-W3: 10 GET + 16 SET + 144 GET        = 170 ops × 100ms = 17s < 30s  OK
 */
public class Instance {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String PAYLOADS_DIR = "/home/hassaan/Project/new_5MB_payloads";
    private static final int    WINDOW_MS    = 30_000;   // 30 s per window
    private static final int    OP_SLEEP_MS  = 100;      // between every network op
    private static final int    NUM_WINDOWS  = 5;

    // File index ranges (0-based into sorted payloadFiles list).
    // Per-tenant caches → T1 and T2 share the same filename pool without collision.
    private static final int F01 = 0;   // file_0001 (0-indexed)
    private static final int F03 = 2;   // file_0003 — T2 demand start
    private static final int F11 = 10;  // file_0011 — T1 demand start
    private static final int F11_END = 2;   // file_0001–file_0002 (T2 hot, T1 small)
    private static final int F10_END = 10;  // file_0001–file_0010 (T1 full hot)
    private static final int F18_END = 18;  // file_0003–file_0018 (demand pool)

    // ── Runtime state ─────────────────────────────────────────────────────────
    private static EchoClient        cacheClient;
    private static List<PayloadFile> payloadFiles;
    private static final List<Double> setLatencies = new ArrayList<>();
    private static final List<Double> getLatencies = new ArrayList<>();

    static class PayloadFile {
        final String key;
        final Path   path;

        PayloadFile(String filename, Path path) {
            this.key  = filename.replace(".bin", "");
            this.path = path;
        }

        ByteString load() throws IOException {
            return EchoClient.readFiletoByteString(path);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) {

        int tenantId = Integer.parseInt(System.getProperty("tenant.id", "1"));

        System.out.printf("==================================================%n");
        System.out.printf("  FairCache Workload — Tenant %d%n", tenantId);
        System.out.printf("  %d windows x %ds | 50MB fair share | 100MB total%n",
                NUM_WINDOWS, WINDOW_MS / 1000);
        System.out.printf("==================================================%n%n");

        try {
            cacheClient = new EchoClient();
            cacheClient.start();

            Response auth = cacheClient.authenticate();
            if (auth.getStatus() != 200)
                throw new RuntimeException("Authentication failed. Status: " + auth.getStatus());
            LogUtil.log("Authenticated. Tenant:", tenantId);

            payloadFiles = loadPayloadMetadata(PAYLOADS_DIR);
            System.out.printf("Loaded %d payload files from %s%n%n",
                    payloadFiles.size(), PAYLOADS_DIR);

            if      (tenantId == 1) runTenant1();
            else if (tenantId == 2) runTenant2();
            else throw new IllegalArgumentException("Unknown tenant.id: " + tenantId);

        } catch (Exception e) {
            LogUtil.log("Fatal error.", "Message", e.getMessage());
            e.printStackTrace();
        } finally {
            try { printResults(); } catch (IOException ignored) {}
            if (cacheClient != null) cacheClient.shutdown();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tenant 1 — 5-window sequence
    // ─────────────────────────────────────────────────────────────────────────

    private static void runTenant1() throws Exception {

        // ── Window 1 ──────────────────────────────────────────────────────────
        // Fill full 50MB allocation hot (f01-f10): SET + 9 GETs → freq = 10 > 7
        // Signal 40MB demand (f11-f18): SET (may fail, alloc full) + 9 GETs → freq = 10
        // DemandTracker records f11-f18 with fail_count > threshold.
        //
        // Expected after redistribution:
        //   T1: cold=0, hot=50MB, demand=40MB, net_demand=+40MB → alloc 50→90MB
        //   T2: cold=40MB (unused alloc), hot=10MB, net_demand=-40MB → alloc 50→10MB
        long wStart = System.currentTimeMillis();
        System.out.println("[T1-W1] Fill 50MB allocation (f01-f10 hot) + signal 40MB demand (f11-f18)");
        setAndHeat(F01, F10_END, 9);   // f01-f10: 1 SET + 9 GET = freq 10  [50MB hot]
        setAndHeat(F11, F18_END, 9);   // f11-f18: 1 SET + 9 GET = freq 10  [40MB demand]
        waitWindow(wStart, 1);

        // ── Window 2 ──────────────────────────────────────────────────────────
        // Alloc = 90MB. Decay halved all: freq 5.
        // Recover f01-f10: +5 GETs → freq 10 (hot).
        // Fill new 40MB with f11-f18: fresh SET + 9 GETs → freq 10 (hot).
        //
        // Expected after redistribution:
        //   T1: cold=0, hot=90MB, demand=0, net_demand=0 → no change
        //   T2: cold=0, hot=10MB, demand=0, net_demand=0 → no change
        //   Debt: T1 A=50+90=140, R=100 → debt=+40 (borrower)
        wStart = System.currentTimeMillis();
        System.out.println("[T1-W2] Recover f01-f10 + fill f11-f18 into new 40MB space");
        getFiles(F01, F10_END, 5);     // recover: freq 5+5 = 10
        setAndHeat(F11, F18_END, 9);   // f11-f18 fresh: freq 10
        waitWindow(wStart, 2);

        // ── Window 3 ──────────────────────────────────────────────────────────
        // Decay halved all: freq 5. Recover all 18 files: +5 GETs → freq 10.
        // T2 will signal 80MB demand this window.
        //
        // Expected after redistribution:
        //   T1: cold=0, hot=90MB, demand=0, net_demand=0, debt=+80 (borrower)
        //   T2: cold=0, hot=10MB, demand=80MB, debt=-80 (lender) → preemption
        //   fairCachePremption takes 80MB from T1 → T1=10MB, T2=90MB
        //   Server performs amortized eviction: T1 keys above 10MB are removed.
        wStart = System.currentTimeMillis();
        System.out.println("[T1-W3] Maintain all 18 files (90MB) hot — recovery pass");
        getFiles(F01, F18_END, 5);     // all 18 files: freq 5+5 = 10
        waitWindow(wStart, 3);

        // ── Window 4 ──────────────────────────────────────────────────────────
        // T1 alloc dropped to 10MB. Server evicted keys > 10MB.
        // Re-establish 10MB: SET f01-f02 fresh + 9 GETs → freq 10 (hot).
        //   NOTE: scenario specified 4 GETs, giving freq=5 < threshold=7 → cold.
        //   Using 9 GETs so freq=10>7 → hot_region=10MB as the scenario intends.
        //
        // Expected after redistribution:
        //   T1: cold=0, hot=10MB, demand=0, debt=+40
        //   T2: cold=0, hot=90MB, demand=0, debt=-40 → lenderFairDemand=0 → no redistrib
        wStart = System.currentTimeMillis();
        System.out.println("[T1-W4] Re-establish 10MB (f01-f02) after eviction");
        // evict(F01, F18_END);
        // Note: 4. not 9 get count because on delete the counter doesnt reset to 0. it remains in decayed state
        setAndHeat(F01, F03, 4);   // f01-f02: 1 SET + 9 GET = freq 10  [10MB hot] 
        waitWindow(wStart, 4);

        // ── Window 5 ──────────────────────────────────────────────────────────
        // Decay: freq 5. Recover f01-f02: +5 GETs → freq 10.
        // T2 also just maintaining → no demand anywhere → no redistribution.
        //
        // Final: T1 cumul A=250+10=260*, R=250 → debt≈0. Fair state reached.
        //   (*cumul A: 50+90+90+10+10=250 exactly)
        wStart = System.currentTimeMillis();
        System.out.println("[T1-W5] Maintain f01-f02 (10MB)");
        getFiles(F01, F11_END, 5);
        waitWindow(wStart, 5);

        System.out.println("\n[T1] All 5 windows complete.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tenant 2 — 5-window sequence
    // ─────────────────────────────────────────────────────────────────────────

    private static void runTenant2() throws Exception {

        // ── Window 1 ──────────────────────────────────────────────────────────
        // Use only 10MB of the 50MB allocation.
        // SET f01-f02 + 9 GETs → freq 10 (hot). Remaining 40MB left empty.
        // Empty 40MB = available space = cold region in redistribution.
        //
        // Expected after redistribution:
        //   T2: cold=40MB, hot=10MB, net_demand=-40MB → alloc 50→10MB
        //   T1: gets T2's 40MB cold surplus → alloc 50→90MB
        long wStart = System.currentTimeMillis();
        System.out.println("[T2-W1] Fill 10MB (f01-f02 hot), leave 40MB cold/unused");
        setAndHeat(F01, F11_END, 9);   // f01-f02: 1 SET + 9 GET = freq 10  [10MB hot]
        // 40MB intentionally unused → cold via available space
        waitWindow(wStart, 1);

        // ── Window 2 ──────────────────────────────────────────────────────────
        // Alloc = 10MB. Decay: freq 5. Recover: +5 GETs → freq 10.
        // No demand → no redistribution.
        //   Debt: T2 A=50+10=60, R=100 → debt=-40 (lender)
        wStart = System.currentTimeMillis();
        System.out.println("[T2-W2] Recover f01-f02");
        getFiles(F01, F11_END, 5);
        waitWindow(wStart, 2);

        // ── Window 3 ──────────────────────────────────────────────────────────
        // Recover f01-f02 (5 GETs → freq 10).
        // Signal 80MB demand: SET f03-f18 (will fail, only 10MB alloc) + 9 GETs each.
        // DemandTracker records f03-f18 with fail_count > threshold.
        //
        // Expected after redistribution:
        //   T2: lender, debt=-80, demand=80MB → lenderFairDemand=min(80,80)=80MB
        //   Preempts 80MB from T1 (borrower, debt=+80) → T1=10MB, T2=90MB
        wStart = System.currentTimeMillis();
        System.out.println("[T2-W3] Recover f01-f02 + signal 80MB demand (f03-f18)");
        getFiles(F01, F11_END, 5);     // recover: freq 5+5 = 10
        setAndHeat(F03, F18_END, 9);   // f03-f18: SET (expected failures) + 9 GETs
        waitWindow(wStart, 3);

        // ── Window 4 ──────────────────────────────────────────────────────────
        // T2 alloc now 90MB. Decay: freq 5.
        // Recover f01-f02 (5 GETs → freq 10).
        // Fill new 80MB space: SET f03-f18 fresh + 9 GETs → freq 10 (hot).
        //
        // Expected after redistribution:
        //   T2: cold=0, hot=90MB, demand=0, debt=-40 (lender)
        //   T1: debt=+40 (borrower), demand=0 → lenderFairDemand=0 → no redistrib
        wStart = System.currentTimeMillis();
        System.out.println("[T2-W4] Recover f01-f02 + fill 80MB space (f03-f18)");
        getFiles(F01, F11_END, 5);     // recover: freq 5+5 = 10
        setAndHeat(F03, F18_END, 9);   // f03-f18 fresh: freq 10  [80MB hot]
        waitWindow(wStart, 4);

        // ── Window 5 ──────────────────────────────────────────────────────────
        // Maintain all 18 files hot. No demand anywhere.
        //   T2 cumul A = 50+10+10+90+90 = 250, R = 250 → debt = 0. Fair.
        wStart = System.currentTimeMillis();
        System.out.println("[T2-W5] Maintain all 18 files (90MB)");
        getFiles(F01, F18_END, 5);     // all 18: freq 5+5 = 10
        waitWindow(wStart, 5);

        System.out.println("\n[T2] All 5 windows complete.");

        // Get a key to trigger rebalancing from sleeping cache thread
        getFiles(F01, F11_END, 1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Operation helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * For each file in [fromIdx, toIdx):
     *   SET it once  (freq +1)
     *   GET it getCount times (freq +getCount)
     * Total increments = 1 + getCount.
     * With getCount=9: freq = 10 > threshold(7) → hot.
     */
    private static void setAndHeat(int fromIdx, int toIdx, int getCount) throws Exception {
        for (int i = fromIdx; i < toIdx; i++) {
            doSet(payloadFiles.get(i));
            for (int g = 0; g < getCount; g++) {
                doGet(payloadFiles.get(i).key);
            }
        }
    }

    /**
     * GET each file in [fromIdx, toIdx) exactly getCount times.
     * Used for frequency recovery after halving decay.
     * With getCount=5: freq 5 (post-decay) + 5 = 10 > threshold → stays hot.
     */
    private static void getFiles(int fromIdx, int toIdx, int getCount) throws Exception {
        for (int i = fromIdx; i < toIdx; i++) {
            for (int g = 0; g < getCount; g++) {
                doGet(payloadFiles.get(i).key);
            }
        }
    }

    private static void evict(int fromIdx, int toIdx) throws Exception {
        for (int i = fromIdx; i < toIdx; i++) {
            doDel(payloadFiles.get(i).key);
        }
    }

    private static Response doDel(String key) throws Exception {
        Response res = cacheClient.deleteKey(key);
         if (res.getStatus() == 429) {
            LogUtil.log("Rate-limited on SET — sleeping 10s");
            Thread.sleep(10_000);
        } else if (res.getStatus() != 200) {
            LogUtil.log("DEL non-200", res.getStatus(), "key:",key );
        }
        Thread.sleep(OP_SLEEP_MS);
        return res;
    }

    private static Response doSet(PayloadFile pf) throws Exception {
        ByteString data = pf.load();
        long t0 = System.nanoTime();
        Response rsp = cacheClient.setKey(pf.key, data, false, false);
        setLatencies.add((System.nanoTime() - t0) / 1_000_000.0);

        if (rsp.getStatus() == 429) {
            LogUtil.log("Rate-limited on SET — sleeping 10s");
            Thread.sleep(10_000);
        } else if (rsp.getStatus() != 201) {
            LogUtil.log("SET non-201:", rsp.getStatus(), "key:", pf.key);
        }
        Thread.sleep(OP_SLEEP_MS);
        return rsp;
    }

    private static Response doGet(String key) throws Exception {
        long t0 = System.nanoTime();
        Response rsp = cacheClient.getKey(key, false, true);
        getLatencies.add((System.nanoTime() - t0) / 1_000_000.0);

        if (rsp.getStatus() == 429) {
            LogUtil.log("Rate-limited on GET — sleeping 10s");
            Thread.sleep(10_000);
        }
        Thread.sleep(OP_SLEEP_MS);
        return rsp;
    }

    /** Sleep out the remainder of the 30-second window. */
    private static void waitWindow(long wStartMs, int windowNum) throws Exception {
        long elapsed   = System.currentTimeMillis() - wStartMs;
        long remaining = WINDOW_MS - elapsed;
        if (remaining > 500) {
            System.out.printf("[W%d] Ops done in %ds. Sleeping %ds for redistribution...%n",
                    windowNum, elapsed / 1000, remaining / 1000);
            Thread.sleep(remaining);
        } else if (remaining > 0) {
            Thread.sleep(remaining);
        } else {
            System.out.printf("[W%d] WARNING: overran window by %dms!%n", windowNum, -remaining);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private static List<PayloadFile> loadPayloadMetadata(String dir) throws IOException {
        File d = new File(dir);
        if (!d.exists() || !d.isDirectory())
            throw new IOException("Payloads directory not found: " + dir);

        File[] bins = d.listFiles((ignored, name) -> name.endsWith(".bin"));
        if (bins == null || bins.length == 0)
            throw new IOException("No .bin files found in: " + dir);

        Arrays.sort(bins, Comparator.comparing(File::getName));

        List<PayloadFile> list = new ArrayList<>();
        for (File f : bins)
            list.add(new PayloadFile(f.getName(), f.toPath()));
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Results
    // ─────────────────────────────────────────────────────────────────────────

    private static void printResults() throws IOException {
        String tid = System.getProperty("tenant.id", "1");
        System.out.println("\n========== RESULTS ==========");
        if (!setLatencies.isEmpty()) {
            Map<String, Double> s = percentiles(setLatencies);
            System.out.println("SET Operations:");
            System.out.printf("  P50: %.3f ms%n",     s.get("p50"));
            System.out.printf("  P95: %.3f ms%n",     s.get("p95"));
            System.out.printf("  P99: %.3f ms%n",     s.get("p99"));
            System.out.printf("  Average: %.3f ms%n", s.get("avg"));
        }
        if (!getLatencies.isEmpty()) {
            Map<String, Double> g = percentiles(getLatencies);
            System.out.println("GET Operations:");
            System.out.printf("  P50: %.3f ms%n",     g.get("p50"));
            System.out.printf("  P95: %.3f ms%n",     g.get("p95"));
            System.out.printf("  P99: %.3f ms%n",     g.get("p99"));
            System.out.printf("  Average: %.3f ms%n", g.get("avg"));
        }
        String outFile = "latencies_tenant" + tid + ".csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
            pw.println("op_type,index,latency_ms");
            for (int i = 0; i < setLatencies.size(); i++)
                pw.printf("SET,%d,%.3f%n", i, setLatencies.get(i));
            for (int i = 0; i < getLatencies.size(); i++)
                pw.printf("GET,%d,%.3f%n", i, getLatencies.get(i));
        }
        System.out.println("Latencies exported to: " + outFile);
    }

    private static Map<String, Double> percentiles(List<Double> vals) {
        List<Double> s = new ArrayList<>(vals);
        Collections.sort(s);
        int n = s.size();
        double sum = 0;
        for (double v : s) sum += v;
        Map<String, Double> m = new HashMap<>();
        m.put("p50", s.get((int) (n * 0.50)));
        m.put("p95", s.get((int) (n * 0.95)));
        m.put("p99", s.get((int) (n * 0.99)));
        m.put("avg", sum / n);
        return m;
    }
}

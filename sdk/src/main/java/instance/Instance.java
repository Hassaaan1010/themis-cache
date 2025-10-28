package instance;

import com.google.protobuf.ByteString;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import client.EchoClient;
import common.LogUtil;
import common.parsing.protos.ResponseProtos.Response;

public class Instance {
    private static EchoClient cacheClient;
    private static final String PAYLOADS_DIR = "/home/hassaan/Project/testGen/payloads";
    private static final int NUM_OPERATIONS = 2_000;

    // Store only file metadata, not the actual file data
    static class PayloadFile {
        String filename;
        String key;
        Path filePath;
        double probability;

        PayloadFile(String filename, Path filePath, double probability) {
            this.filename = filename;
            this.key = filename.replace(".bin", "");
            this.filePath = filePath;
            this.probability = probability;
        }

        // Lazy load file data only when needed
        ByteString loadData() throws IOException {
            return EchoClient.readFiletoByteString(filePath);
        }
    }

    public static void main(String[] args) {
        List<Double> setLatencies = new ArrayList<>();
        List<Double> getLatencies = new ArrayList<>();
        List<Integer> fileSizes = new ArrayList<>();

        try {
            // Initialize client
            cacheClient = new EchoClient();
            cacheClient.start();

            // Authenticate
            System.out.println("Starting Auth operation_____________________________________________");
            LogUtil.log("Sending Auth");
            Response authResponse = cacheClient.authenticate();

            if (authResponse.getStatus() == 200) {
                LogUtil.log("Auth was successful.", "Status:", authResponse.getStatus());
            } else {
                LogUtil.log("Response without 200 status.", "Response", authResponse);
                throw new Error("Authentication failed");
            }

            // Load only file metadata (paths and probabilities)
            System.out.println("Loading payload file metadata_____________________________________________");
            List<PayloadFile> payloadFiles = loadPayloadFileMetadata();
            System.out.println("Loaded metadata for " + payloadFiles.size() + " payload files");

            // Build cumulative distribution for sampling
            double[] cumulativeProbs = buildCumulativeDistribution(payloadFiles);
            Random random = new Random();

            // Perform SET operations with latency measurement
            System.out.println("Starting SET operations_____________________________________________");
            for (int i = 0; i < NUM_OPERATIONS; i++) {
                PayloadFile file = selectFileByZipf(payloadFiles, cumulativeProbs, random);

                // Load file data on-demand
                ByteString data = file.loadData();

                // Add size to fileSize array
                fileSizes.add(data.size());

                long startTime = System.nanoTime();
                Response setResponse = cacheClient.setKey(file.key, data, false, false);
                long endTime = System.nanoTime();

                double latencyMs = (endTime - startTime) / 1_000_000.0;
                setLatencies.add(latencyMs);

                if (setResponse.getStatus() != 201) {
                    LogUtil.log("SET failed for key:", file.key, "Status:", setResponse.getStatus());
                }

                if ((i + 1) % 10000 == 0) {
                    System.out.println("Completed " + (i + 1) + " SET operations");
                }
            }

            // Perform GET operations with latency measurement
            System.out.println("Starting GET operations_____________________________________________");
            for (int i = 0; i < NUM_OPERATIONS; i++) {
                PayloadFile file = selectFileByZipf(payloadFiles, cumulativeProbs, random);

                long startTime = System.nanoTime();
                Response getResponse = cacheClient.getKey(file.key, false, true);
                long endTime = System.nanoTime();

                double latencyMs = (endTime - startTime) / 1_000_000.0;
                getLatencies.add(latencyMs);

                fileSizes.add(getResponse.getValue().size());

                if (getResponse.getStatus() != 200) {
                    LogUtil.log("GET failed for key:", file.key, "Status:", getResponse.getStatus());
                }

                if ((i + 1) % 10000 == 0) {
                    System.out.println("Completed " + (i + 1) + " GET operations");
                }
            }

            // Calculate percentiles
            System.out.println("Calculating percentiles_____________________________________________");
            Map<String, Double> setMetrics = calculatePercentiles(setLatencies);
            Map<String, Double> getMetrics = calculatePercentiles(getLatencies);

            // Print results
            System.out.println("\n========== RESULTS ==========");
            System.out.println("SET Operations:");
            System.out.println("  P50: " + String.format("%.3f", setMetrics.get("p50")) + " ms");
            System.out.println("  P95: " + String.format("%.3f", setMetrics.get("p95")) + " ms");
            System.out.println("  P99: " + String.format("%.3f", setMetrics.get("p99")) + " ms");
            System.out.println("  Average: " + String.format("%.3f", setMetrics.get("avg")) + " ms");

            System.out.println("\nGET Operations:");
            System.out.println("  P50: " + String.format("%.3f", getMetrics.get("p50")) + " ms");
            System.out.println("  P95: " + String.format("%.3f", getMetrics.get("p95")) + " ms");
            System.out.println("  P99: " + String.format("%.3f", getMetrics.get("p99")) + " ms");
            System.out.println("  Average: " + String.format("%.3f", getMetrics.get("avg")) + " ms");

            // Export to CSV
            exportToCSV(setLatencies, getLatencies, fileSizes, setMetrics, getMetrics);

            LogUtil.log("Completed successfully");

        } catch (Exception e) {
            LogUtil.log("Error in Instance main method.", "Error message", e.getMessage(), "Error", e);
            e.printStackTrace();
        } finally {
            if (cacheClient != null) {
                cacheClient.shutdown();
            }
        }
    }

    private static List<PayloadFile> loadPayloadFileMetadata() throws IOException {
        List<PayloadFile> files = new ArrayList<>();
        File payloadsDir = new File(PAYLOADS_DIR);

        if (!payloadsDir.exists() || !payloadsDir.isDirectory()) {
            throw new IOException("Payloads directory not found: " + PAYLOADS_DIR);
        }

        // Get all .bin files and sort them
        File[] binFiles = payloadsDir.listFiles((_, name) -> name.endsWith(".bin"));
        if (binFiles == null || binFiles.length == 0) {
            throw new IOException("No payload files found in: " + PAYLOADS_DIR);
        }

        Arrays.sort(binFiles, Comparator.comparing(File::getName));

        // Calculate Zipf probabilities (matching main.py logic)
        double zipfExponent = 1.0;
        double[] weights = new double[binFiles.length];
        double sumWeights = 0.0;

        for (int i = 0; i < binFiles.length; i++) {
            weights[i] = 1.0 / Math.pow(i + 1, zipfExponent);
            sumWeights += weights[i];
        }

        // Normalize probabilities
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= sumWeights;
        }

        // Store only metadata (paths and probabilities), not actual file content
        for (int i = 0; i < binFiles.length; i++) {
            files.add(new PayloadFile(binFiles[i].getName(), binFiles[i].toPath(), weights[i]));
        }

        return files;
    }

    private static double[] buildCumulativeDistribution(List<PayloadFile> files) {
        double[] cumulative = new double[files.size()];
        cumulative[0] = files.get(0).probability;

        for (int i = 1; i < files.size(); i++) {
            cumulative[i] = cumulative[i - 1] + files.get(i).probability;
        }

        return cumulative;
    }

    private static PayloadFile selectFileByZipf(List<PayloadFile> files,
            double[] cumulativeProbs,
            Random random) {
        double rand = random.nextDouble();

        for (int i = 0; i < cumulativeProbs.length; i++) {
            if (rand <= cumulativeProbs[i]) {
                return files.get(i);
            }
        }

        return files.get(files.size() - 1);
    }

    private static Map<String, Double> calculatePercentiles(List<Double> latencies) {
        Collections.sort(latencies);
        Map<String, Double> metrics = new HashMap<>();

        int size = latencies.size();
        metrics.put("p50", latencies.get((int) (size * 0.50)));
        metrics.put("p95", latencies.get((int) (size * 0.95)));
        metrics.put("p99", latencies.get((int) (size * 0.99)));

        double sum = 0.0;
        for (double lat : latencies) {
            sum += lat;
        }
        metrics.put("avg", sum / size);

        return metrics;
    }

    private static void exportToCSV(List<Double> setLatencies,
            List<Double> getLatencies,
            List<Integer> fileSizes,
            Map<String, Double> setMetrics,
            Map<String, Double> getMetrics) throws IOException {

        // Export detailed latencies
        try (PrintWriter writer = new PrintWriter(new FileWriter("latencies_detailed.csv"))) {
            writer.println("index,operation_type,operation_index,latency_ms,file_size");

            for (int i = 0; i < setLatencies.size(); i++) {
                writer.println(i + ",SET," + i + "," + setLatencies.get(i) + "," + fileSizes.get(i));
            }

            for (int i = 0; i < getLatencies.size(); i++) {
                writer.println(i + ",GET," + i + "," + getLatencies.get(i) + "," + fileSizes.get(i));
            }
        }

        // Export summary metrics
        try (PrintWriter writer = new PrintWriter(new FileWriter("latencies_summary.csv"))) {
            writer.println("operation_type,metric,value_ms");

            writer.println("SET,P50," + setMetrics.get("p50"));
            writer.println("SET,P95," + setMetrics.get("p95"));
            writer.println("SET,P99," + setMetrics.get("p99"));
            writer.println("SET,Average," + setMetrics.get("avg"));

            writer.println("GET,P50," + getMetrics.get("p50"));
            writer.println("GET,P95," + getMetrics.get("p95"));
            writer.println("GET,P99," + getMetrics.get("p99"));
            writer.println("GET,Average," + getMetrics.get("avg"));
        }

        System.out.println("\nExported results to:");
        System.out.println("  - latencies_detailed.csv");
        System.out.println("  - latencies_summary.csv");
    }
}

// package instance;

// import com.google.protobuf.ByteString;

// import java.nio.file.Path;

// import client.EchoClient;
// import common.LogUtil;
// import common.parsing.protos.ResponseProtos.Response;

// public class Instance {

// private static EchoClient cacheClient;

// public static void main(String[] args) {
// ;
// try {
// cacheClient = new EchoClient();

// cacheClient.start();

// System.out.println("Starting Auth
// operation_____________________________________________");
// LogUtil.log("Sending Auth");
// Response authResponse = cacheClient.authenticate();

// // LogUtil.log("Receieved Auth", "Auth response", authResponse);

// if (authResponse.getStatus() == 200) {
// LogUtil.log("Auth was successful.", "Status:", authResponse.getStatus());
// } else {
// LogUtil.log("Response without 200 status.", "Response", authResponse);
// throw new Error("Authentication failed");

// }

// System.out.println("Starting Set operation
// _____________________________________________");
// /*
// * SET sample
// */
// // String value = "PONG";
// // ByteString payloadValue = ByteString.copyFromUtf8(value);

// Path filePath =
// Path.of("/home/hassaan/Project/loadTestFiles/25_MB_FILE.bin");
// ByteString payloadValue = EchoClient.readFiletoByteString(filePath);

// System.out.println("File read to memory.");
// Response setResponse = cacheClient.setKey("PING", payloadValue, false,
// false);

// // Validate response
// if (setResponse.getStatus() == 201) {
// // String message = setResponse.getMessage();
// // LogUtil.log(message, "Response status: ", setResponse.getStatus(), "Set
// // message:", message);
// LogUtil.log("Response for SET recieved with success.");

// } else {
// LogUtil.log("Response without 201 status.", "Response", setResponse);
// throw new Exception("Set request failed.");
// }

// for (int i = 0; i < 100_000; i++) {
// // Thread.sleep(0, 500000);
// Thread.sleep(500);
// callGet("PING");
// }

// LogUtil.log("Completed successfully");

// // for (int i = 0; i < 100_000; i++){
// // }
// // LogUtil.log("Looped successfully");

// } catch (Exception e) {
// cacheClient.shutdown();
// LogUtil.log("Error in Instance main method.", "Error message",
// e.getMessage(), "Error", e);
// }

// }

// private static void callGet(String key) {
// System.out.println("Starting Get operation
// _____________________________________________");
// /*
// * GET sample
// */
// cacheClient.getKey(key, false, true); // TODO: payload size should be ENUM.

// // Validate response
// /*
// * if (getResponse.getStatus() == 200) {
// * // String returnedMessage = getResponse.getMessage();
// * // LogUtil.log("Response received successfully", "Value", returnedMessage);
// * LogUtil.log("Response for GET recieved with success.");
// *
// * } else {
// * LogUtil.log("Get request failed. Response without 200 status.", "Response",
// * getResponse);
// *
// * // throw new Exception("Get request failed.");
// * }
// */

// }

// }

import pandas as pd
from scipy.stats import pearsonr, spearmanr
import numpy as np

# Load data
folder_name = "zgb_10k_2gC_4gS/"
csv_file_name = folder_name + "latencies_detailed.csv"
df = pd.read_csv(csv_file_name)

# Group by unique file sizes and calculate mean latency
grouped = df.groupby('file_size').agg({
    'latency_ms': ['mean', 'std', 'count']
}).reset_index()
grouped.columns = ['file_size', 'mean_latency', 'std_latency', 'count']

print("Correlation on UNIQUE file sizes (not individual requests):")
print("="*60)

# Now calculate correlation on unique file sizes
file_sizes = grouped['file_size'].values
mean_latencies = grouped['mean_latency'].values

pearson_r, pearson_p = pearsonr(file_sizes, mean_latencies)
spearman_r, spearman_p = spearmanr(file_sizes, mean_latencies)

# Log-log for power law
log_sizes = np.log10(file_sizes)
log_latencies = np.log10(mean_latencies)
log_pearson_r, log_pearson_p = pearsonr(log_sizes, log_latencies)

print(f"Pearson Correlation (r):      {pearson_r:.4f}  (p-value: {pearson_p:.2e})")
print(f"Spearman Correlation (ρ):     {spearman_r:.4f}  (p-value: {spearman_p:.2e})")
print(f"Log-Log Pearson (power law):  {log_pearson_r:.4f}  (p-value: {log_pearson_p:.2e})")

print(f"\nNumber of unique file sizes: {len(grouped)}")
print(f"File size range: {file_sizes.min()} to {file_sizes.max()} bytes")
print(f"Latency range: {mean_latencies.min():.3f} to {mean_latencies.max():.3f} ms")

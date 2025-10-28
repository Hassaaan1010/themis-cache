import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

# load data
folder_name="small_heap_tuned_zgc/" #example folderName/
csv_file_name = folder_name + "latencies_detailed.csv"
df = pd.read_csv(csv_file_name)

# split by operation type
set_df = df[df["operation_type"] == "SET"]
get_df = df[df["operation_type"] == "GET"]

# configure figure
fig, axes = plt.subplots(2, 4, figsize=(16, 8))
axes = axes.flatten()

# SET - linear
axes[0].plot(set_df["operation_index"], set_df["latency_ms"], color="tab:blue")
axes[0].set_title("SET Latency (Linear Scale)")
axes[0].set_xlabel("Request Index (0–9999)")
axes[0].set_ylabel("Latency (ms)")
axes[0].set_ylim(0, 130)

# SET - logarithmic
axes[1].plot(set_df["operation_index"], set_df["latency_ms"], color="tab:blue")
axes[1].set_title("SET Latency (Logarithmic Y Scale)")
axes[1].set_xlabel("Request Index (0–9999)")
axes[1].set_ylabel("Latency (ms)")
axes[1].set_yscale("log")
axes[1].set_ylim(0.05, 130)

# GET - linear
axes[2].plot(get_df["operation_index"], get_df["latency_ms"], color="tab:green")
axes[2].set_title("GET Latency (Linear Scale)")
axes[2].set_xlabel("Request Index (0–9999)")
axes[2].set_ylabel("Latency (ms)")
axes[2].set_ylim(0, 130)

# GET - logarithmic
axes[3].plot(get_df["operation_index"], get_df["latency_ms"], color="tab:green")
axes[3].set_title("GET Latency (Logarithmic Y Scale)")
axes[3].set_xlabel("Request Index (0–9999)")
axes[3].set_ylabel("Latency (ms)")
axes[3].set_yscale("log")
axes[3].set_ylim(0.05, 130)
# [4] File Size Distribution Histogram (Bell Curve)
axes[4].hist(df["file_size"], bins=50, color="tab:orange", edgecolor="black", alpha=0.7)
axes[4].set_title("File Size Distribution (Frequency)")
axes[4].set_xlabel("File Size (bytes)")
axes[4].set_ylabel("Frequency")
axes[4].set_xscale("log")
axes[4].grid(True, linestyle="--", alpha=0.3)

# [5] Empty or additional metric
axes[5].axis('off')

# [6] SET: Latency vs File Size (Binned Line Plot)
# Group data into bins and calculate mean latency per bin
set_sorted = set_df.sort_values("file_size")
bins = np.logspace(np.log10(set_sorted["file_size"].min()), 
                   np.log10(set_sorted["file_size"].max()), 30)
set_sorted['bin'] = pd.cut(set_sorted['file_size'], bins=bins)
set_binned = set_sorted.groupby('bin', observed=True).agg({
    'file_size': 'mean',
    'latency_ms': 'mean'
}).reset_index(drop=True)

axes[6].plot(set_binned["file_size"], set_binned["latency_ms"], 
             color="tab:red", marker='o', linewidth=2, markersize=4)
axes[6].set_title("SET: Latency vs File Size")
axes[6].set_xlabel("File Size (bytes)")
axes[6].set_ylabel("Average Latency (ms)")
axes[6].set_xscale("log")
axes[6].set_yscale("log")
axes[6].grid(True, linestyle="--", alpha=0.3)

# [7] GET: Latency vs File Size (Binned Line Plot)
get_sorted = get_df.sort_values("file_size")
get_sorted['bin'] = pd.cut(get_sorted['file_size'], bins=bins)
get_binned = get_sorted.groupby('bin', observed=True).agg({
    'file_size': 'mean',
    'latency_ms': 'mean'
}).reset_index(drop=True)

axes[7].plot(get_binned["file_size"], get_binned["latency_ms"], 
             color="tab:purple", marker='o', linewidth=2, markersize=4)
axes[7].set_title("GET: Latency vs File Size")
axes[7].set_xlabel("File Size (bytes)")
axes[7].set_ylabel("Average Latency (ms)")
axes[7].set_xscale("log")
axes[7].set_yscale("log")
axes[7].grid(True, linestyle="--", alpha=0.3)

plt.tight_layout()
plt.savefig(folder_name + "latency_analysis.png", dpi=300)
plt.show()
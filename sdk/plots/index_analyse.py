import pandas as pd
from scipy.stats import pearsonr, spearmanr
from sklearn.metrics import r2_score
from sklearn.linear_model import LinearRegression
import numpy as np
import matplotlib.pyplot as plt

# Load data
folder_name = "zgb_10k_2gC_4gS/"
csv_file_name = folder_name + "latencies_detailed.csv"
df = pd.read_csv(csv_file_name)

# Add execution order index if not present
if 'index' not in df.columns:
    df['index'] = df.index

# Split by operation type
set_df = df[df["operation_type"] == "SET"].copy()
get_df = df[df["operation_type"] == "GET"].copy()

print("=" * 60)
print("CORRELATION ANALYSIS: Execution Order vs Latency")
print("(Testing if performance degrades over time)")
print("=" * 60)

# Function to calculate correlations
def calculate_correlations(data, operation_name):
    execution_order = data["index"].values
    latencies = data["latency_ms"].values
    
    # Pearson correlation (linear relationship)
    pearson_r, pearson_p = pearsonr(execution_order, latencies)
    
    # Spearman correlation (monotonic relationship)
    spearman_r, spearman_p = spearmanr(execution_order, latencies)
    
    # R-squared using linear regression
    X = execution_order.reshape(-1, 1)
    y = latencies
    model = LinearRegression()
    model.fit(X, y)
    r2 = r2_score(y, model.predict(X))
    
    print(f"\n{operation_name} Operations:")
    print("-" * 60)
    print(f"Pearson Correlation (r):      {pearson_r:.4f}  (p-value: {pearson_p:.2e})")
    print(f"Spearman Correlation (ρ):     {spearman_r:.4f}  (p-value: {spearman_p:.2e})")
    print(f"R² Score (Linear):            {r2:.4f}")
    
    # Find outliers
    print(f"\nLatency Statistics:")
    print(f"  Min:     {latencies.min():.3f} ms")
    print(f"  Median:  {np.median(latencies):.3f} ms")
    print(f"  P95:     {np.percentile(latencies, 95):.3f} ms")
    print(f"  P99:     {np.percentile(latencies, 99):.3f} ms")
    print(f"  Max:     {latencies.max():.3f} ms")
    
    # Find the worst latency spike
    max_idx = latencies.argmax()
    max_lat_row = data.iloc[max_idx]
    print(f"\nWORST LATENCY SPIKE:")
    print(f"  Latency: {max_lat_row['latency_ms']:.3f} ms")
    print(f"  Index:   {max_lat_row['index']}")
    print(f"  File:    {max_lat_row.get('file_size', 'N/A')} bytes")
    
    # Interpretation
    print(f"\nInterpretation:")
    if pearson_r > 0.3:
        print(f"  • POSITIVE correlation - latency INCREASES over time")
        print(f"  • This suggests PERFORMANCE DEGRADATION (likely GC pressure)")
    elif pearson_r < -0.3:
        print(f"  • NEGATIVE correlation - latency DECREASES over time")
        print(f"  • This suggests warm-up effects or caching benefits")
    else:
        print(f"  • NO significant time-based correlation")
        print(f"  • Latency spikes are likely random GC pauses, not gradual degradation")
    
    if pearson_p < 0.01:
        print(f"  • Correlation is statistically SIGNIFICANT (p < 0.01)")
    
    return pearson_r, spearman_r, r2, model

# Calculate for SET operations
set_pearson, set_spearman, set_r2, set_model = calculate_correlations(set_df, "SET")

# Calculate for GET operations
get_pearson, get_spearman, get_r2, get_model = calculate_correlations(get_df, "GET")

# Summary
print("\n" + "=" * 60)
print("SUMMARY")
print("=" * 60)
print(f"SET Operations: r={set_pearson:.3f}, ρ={set_spearman:.3f}, R²={set_r2:.3f}")
print(f"GET Operations: r={get_pearson:.3f}, ρ={get_spearman:.3f}, R²={get_r2:.3f}")

if set_pearson > 0.3 or get_pearson > 0.3:
    print("\n⚠️  WARNING: Performance degradation detected!")
    print("   Likely causes:")
    print("   1. Java GC pauses increasing over time")
    print("   2. Memory pressure building up")
    print("   3. Cache eviction thrashing")
    print("\n   Recommendations:")
    print("   - Check JVM GC logs for long pauses")
    print("   - Increase heap size or use low-latency GC (ZGC/Shenandoah)")
    print("   - Monitor memory usage trends")
else:
    print("\n✓ No systematic performance degradation over time")
    print("  High latency spikes are likely isolated GC events")

print("=" * 60)

# Create visualization
fig, axes = plt.subplots(1, 2, figsize=(14, 5))

# SET plot
axes[0].scatter(set_df['index'], set_df['latency_ms'], alpha=0.3, s=1, color='blue')
axes[0].plot(set_df['index'], set_model.predict(set_df[['index']]), 
             'r-', linewidth=2, label=f'Trend (r={set_pearson:.3f})')
axes[0].set_xlabel('Execution Order (Index)')
axes[0].set_ylabel('Latency (ms)')
axes[0].set_title('SET: Latency Over Time')
axes[0].legend()
axes[0].grid(True, alpha=0.3)

# GET plot
axes[1].scatter(get_df['index'], get_df['latency_ms'], alpha=0.3, s=1, color='green')
axes[1].plot(get_df['index'], get_model.predict(get_df[['index']]), 
             'r-', linewidth=2, label=f'Trend (r={get_pearson:.3f})')
axes[1].set_xlabel('Execution Order (Index)')
axes[1].set_ylabel('Latency (ms)')
axes[1].set_title('GET: Latency Over Time')
axes[1].legend()
axes[1].grid(True, alpha=0.3)

plt.tight_layout()
plt.savefig(folder_name + 'latency_over_time.png', dpi=300)
print(f"\nPlot saved to: {folder_name}latency_over_time.png")

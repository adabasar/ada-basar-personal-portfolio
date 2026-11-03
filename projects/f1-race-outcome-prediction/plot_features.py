"""Feature importance plots for the CSCI 467 F1 tree models.
 
Reads the importance CSVs written by train_tree_models.py and saves one bar
chart per model.
 
Usage:
    python3 plot_feature_importance.py
"""
 
from pathlib import Path
 
import matplotlib.pyplot as plt
import pandas as pd
 
 
DATA_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = DATA_DIR / "tree_models_results"
MODELS = {"random_forest": "Random Forest", "extra_trees": "Extra Trees"}
TOP_N = 15
 
 
def main() -> None:
    for model_name, title in MODELS.items():
        table = pd.read_csv(OUTPUT_DIR / f"{model_name}_feature_importance.csv").head(TOP_N)
        # Drop the "numeric__" and "categorical__" prefixes from the pipeline.
        names = table["feature"].str.split("__").str[-1]
 
        plt.figure(figsize=(7, 5))
        plt.barh(names[::-1], table["importance"][::-1])
        plt.xlabel("Importance")
        plt.title(f"{title}: top {TOP_N} features")
        plt.tight_layout()
        plt.savefig(OUTPUT_DIR / f"{model_name}_feature_importance.png", dpi=200)
        plt.close()
 
    print(f"Output: {OUTPUT_DIR}")
 
 
if __name__ == "__main__":
    main()
 
Claude finished the response

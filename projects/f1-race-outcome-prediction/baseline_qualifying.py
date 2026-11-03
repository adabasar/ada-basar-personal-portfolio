"""Qualifying-position baseline for the CSCI 467 F1 project.

Predicts that each driver finishes in the class matching their qualifying
position of the following 3 classes: P1-P3 podium, P4-P10 top ten, P11+ outside the top ten. There is
nothing to fit, so the training split is scored only for reference.

Usage:
    python3 baseline_qualifying.py                 # train and dev
    python3 baseline_qualifying.py --include-test  # final evaluation only
"""

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

from evaluate import CLASSES, evaluate, format_report


DATA_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = DATA_DIR / "baseline_results"
SPLIT_FILES = {
    "train": "f1_features_train_2006_2017.csv",
    "dev": "f1_features_dev_2018.csv",
    "test": "f1_features_test_2019_2020.csv",
}


def predict_from_qualifying(qualifying_position: pd.Series) -> np.ndarray:
    if qualifying_position.isna().any():
        raise ValueError("Baseline requires a qualifying position for every row.")
    return np.select(
        [qualifying_position <= 3, qualifying_position <= 10],
        ["podium", "top_ten"],
        default="outside_top_ten",
    )


def metrics_row(split: str, results: dict) -> dict:
    row = {
        "model": "qualifying_baseline",
        "split": split,
        "rows": results["rows"],
        "accuracy": results["accuracy"],
        "macro_f1": results["macro_f1"],
    }
    for class_name, scores in results["per_class"].items():
        for metric in ["precision", "recall", "f1"]:
            row[f"{class_name}_{metric}"] = scores[metric]
    row["adjacent_errors"] = results["adjacent_errors"]
    row["distant_errors"] = results["distant_errors"]
    return row


def main(include_test: bool) -> None:
    # The test split stays untouched until the final evaluation.
    splits = ["train", "dev"] + (["test"] if include_test else [])
    OUTPUT_DIR.mkdir(exist_ok=True)

    rows = []
    for split in splits:
        data = pd.read_csv(DATA_DIR / SPLIT_FILES[split])
        predictions = predict_from_qualifying(data["qualifying_position"])
        results = evaluate(data["target_class"], predictions)
        print(format_report(split, results), end="\n\n")

        rows.append(metrics_row(split, results))
        pd.DataFrame(results["confusion_matrix"], index=CLASSES, columns=CLASSES).to_csv(
            OUTPUT_DIR / f"confusion_matrix_{split}.csv", index_label="true_class"
        )

    pd.DataFrame(rows).to_csv(OUTPUT_DIR / "metrics.csv", index=False)
    print(f"Output: {OUTPUT_DIR}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument(
        "--include-test",
        action="store_true",
        help="also score the 2019-2020 test split (final evaluation only)",
    )
    main(parser.parse_args().include_test)

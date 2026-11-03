"""Multinomial logistic regression for the CSCI 467 F1 project.

Fits on the training seasons only and selects C and class_weight by development
macro-F1. Imputation, scaling, and one-hot encoding are fit on the training
split inside the pipeline.

Usage:
    python3 train_logistic_regression.py                 # train and dev
    python3 train_logistic_regression.py --include-test  # final evaluation only
"""

import argparse
from pathlib import Path

import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import f1_score
from sklearn.pipeline import Pipeline, make_pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from evaluate import CLASSES, evaluate, format_report, metrics_row


DATA_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = DATA_DIR / "logistic_regression_results"
SPLIT_FILES = {
    "train": "f1_features_train_2006_2017.csv",
    "dev": "f1_features_dev_2018.csv",
    "test": "f1_features_test_2019_2020.csv",
}
RACE = ["season", "round"]

NUMERIC_FEATURES = [
    "qualifying_position",
    "grid_position",
    "q1_gap_pct",
    "q2_gap_pct",
    "q3_gap_pct",
    "driver_points_share",
    "constructor_points_share",
    "driver_starts_before",
    "driver_recent_avg_finish",
    "driver_recent_top10_rate",
    "driver_recent_podium_rate",
    "driver_recent_dnf_rate",
    "constructor_recent_avg_finish",
    "constructor_recent_top10_rate",
    "constructor_recent_podium_rate",
    "constructor_recent_dnf_rate",
    "driver_circuit_starts_before",
    "driver_circuit_avg_finish_before",
    "constructor_circuit_avg_finish_before",
    "driver_age",
]
CATEGORICAL_FEATURES = ["circuitId"]

C_VALUES = [0.001, 0.01, 0.1, 1, 10, 100]
CLASS_WEIGHTS = [None, "balanced"]


def add_model_features(data: pd.DataFrame) -> pd.DataFrame:
    """Add features that compare drivers within a race, using pre-race data only."""
    data = data.copy()
    races = data.groupby(RACE)

    # Grid 0 means a pit-lane start, which is behind every other car.
    data["grid_position"] = data["grid"].where(data["grid"] > 0, races["grid"].transform("size"))

    # Raw lap times mostly reflect circuit length, so compare within the session.
    for stage in ["q1", "q2", "q3"]:
        seconds = data[f"{stage}_seconds"]
        fastest = races[f"{stage}_seconds"].transform("min")
        data[f"{stage}_gap_pct"] = 100 * (seconds - fastest) / fastest

    # A win was worth 10 points before 2010 and 25 after, so use share of the leader.
    for team in ["driver", "constructor"]:
        points = data[f"{team}_points_before_race"]
        leader = races[f"{team}_points_before_race"].transform("max")
        data[f"{team}_points_share"] = (points / leader).fillna(0)
    return data


def load_split(split: str) -> pd.DataFrame:
    return add_model_features(pd.read_csv(DATA_DIR / SPLIT_FILES[split]))


def build_model(c: float, class_weight) -> Pipeline:
    numeric = make_pipeline(SimpleImputer(strategy="median", add_indicator=True), StandardScaler())
    features = ColumnTransformer([
        ("numeric", numeric, NUMERIC_FEATURES),
        ("categorical", OneHotEncoder(handle_unknown="ignore"), CATEGORICAL_FEATURES),
    ])
    classifier = LogisticRegression(
        solver="lbfgs", C=c, class_weight=class_weight, max_iter=5000
    )
    return Pipeline([("features", features), ("classifier", classifier)])


def tune(train: pd.DataFrame, dev: pd.DataFrame) -> tuple[Pipeline, pd.DataFrame]:
    inputs = NUMERIC_FEATURES + CATEGORICAL_FEATURES
    best_model, best_score = None, -1.0
    rows = []
    for class_weight in CLASS_WEIGHTS:
        for c in C_VALUES:
            model = build_model(c, class_weight).fit(train[inputs], train["target_class"])
            scores = {
                split: f1_score(
                    data["target_class"], model.predict(data[inputs]),
                    labels=CLASSES, average="macro",
                )
                for split, data in [("train", train), ("dev", dev)]
            }
            rows.append({
                "class_weight": class_weight or "none",
                "C": c,
                "train_macro_f1": scores["train"],
                "dev_macro_f1": scores["dev"],
            })
            if scores["dev"] > best_score:
                best_model, best_score = model, scores["dev"]
    return best_model, pd.DataFrame(rows)


def coefficients(model: Pipeline) -> pd.DataFrame:
    classifier = model.named_steps["classifier"]
    table = pd.DataFrame(
        classifier.coef_.T,
        index=model.named_steps["features"].get_feature_names_out(),
        columns=classifier.classes_,
    )
    return table[CLASSES].rename_axis("feature")


def main(include_test: bool) -> None:
    # The test split stays untouched until the final evaluation.
    splits = {"train": load_split("train"), "dev": load_split("dev")}
    if include_test:
        splits["test"] = load_split("test")
    OUTPUT_DIR.mkdir(exist_ok=True)

    model, tuning = tune(splits["train"], splits["dev"])
    classifier = model.named_steps["classifier"]
    print(f"Selected C={classifier.C}, class_weight={classifier.class_weight}\n")
    print(tuning.round(4).to_string(index=False), end="\n\n")
    tuning.to_csv(OUTPUT_DIR / "tuning_results.csv", index=False)
    coefficients(model).to_csv(OUTPUT_DIR / "coefficients.csv")

    rows = []
    inputs = NUMERIC_FEATURES + CATEGORICAL_FEATURES
    for split, data in splits.items():
        results = evaluate(data["target_class"], model.predict(data[inputs]))
        print(format_report(split, results), end="\n\n")

        rows.append(metrics_row("logistic_regression", split, results))
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

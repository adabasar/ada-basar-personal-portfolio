"""Random Forest and Extra Trees for the CSCI 467 F1 project.
 
Uses the same features and preprocessing as train_logistic_regression.py, fits
on the training seasons only, and selects hyperparameters by development
macro-F1.
 
Usage:
    python3 train_tree_models.py                 # train and dev
    python3 train_tree_models.py --include-test  # final evaluation only
"""
 
import argparse
from pathlib import Path
 
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import ExtraTreesClassifier, RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.metrics import f1_score
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder
 
from evaluate import CLASSES, evaluate, format_report, metrics_row
from train_logistic_regression import CATEGORICAL_FEATURES, NUMERIC_FEATURES, load_split
 
 
DATA_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = DATA_DIR / "tree_models_results"
INPUTS = NUMERIC_FEATURES + CATEGORICAL_FEATURES
MODELS = {"random_forest": RandomForestClassifier, "extra_trees": ExtraTreesClassifier}
 
MAX_DEPTHS = [None, 6, 10, 16]
MIN_SAMPLES_LEAF = [1, 5, 10, 20]
MAX_FEATURES = ["sqrt", 0.5]
CLASS_WEIGHTS = [None, "balanced"]
CONTEXT_COLUMNS = ["season", "round", "raceName", "driverName", "constructorName", "grid", "status"]
 
def build_model(model_name: str, max_depth, min_samples_leaf: int, max_features, class_weight) -> Pipeline:
    features = ColumnTransformer([
        ("numeric", SimpleImputer(strategy="median", add_indicator=True), NUMERIC_FEATURES),
        ("categorical", OneHotEncoder(handle_unknown="ignore"), CATEGORICAL_FEATURES),
    ])
    classifier = MODELS[model_name](
        n_estimators=300,
        max_depth=max_depth,
        min_samples_leaf=min_samples_leaf,
        max_features=max_features,
        class_weight=class_weight,
        random_state=467,
        n_jobs=-1,
    )
    return Pipeline([("features", features), ("classifier", classifier)]) 
 
def tune(model_name: str, train: pd.DataFrame, dev: pd.DataFrame) -> tuple[Pipeline, pd.DataFrame]:
    best_model, best_score = None, -1.0
    rows = []
    for class_weight in CLASS_WEIGHTS:
        for max_depth in MAX_DEPTHS:
            for min_samples_leaf in MIN_SAMPLES_LEAF:
                for max_features in MAX_FEATURES:
                    model = build_model(
                        model_name, max_depth, min_samples_leaf, max_features, class_weight
                    ).fit(train[INPUTS], train["target_class"])
                    scores = {
                        split: f1_score(
                            data["target_class"], model.predict(data[INPUTS]),
                            labels=CLASSES, average="macro",
                        )
                        for split, data in [("train", train), ("dev", dev)]
                    }
                    rows.append({
                        "class_weight": class_weight or "none",
                        "max_depth": max_depth or "none",
                        "min_samples_leaf": min_samples_leaf,
                        "max_features": max_features,
                        "train_macro_f1": scores["train"],
                        "dev_macro_f1": scores["dev"],
                    })
                    if scores["dev"] > best_score:
                        best_model, best_score = model, scores["dev"]
    return best_model, pd.DataFrame(rows)
 
 
def feature_importances(model: Pipeline) -> pd.DataFrame:
    table = pd.DataFrame({
        "feature": model.named_steps["features"].get_feature_names_out(),
        "importance": model.named_steps["classifier"].feature_importances_,
    })
    return table.sort_values("importance", ascending=False)
 
 
def main(include_test: bool) -> None:
    splits = {"train": load_split("train"), "dev": load_split("dev")}
    if include_test:
        splits["test"] = load_split("test")
    OUTPUT_DIR.mkdir(exist_ok=True)
 
    rows = []
    for model_name in MODELS:
        model, tuning = tune(model_name, splits["train"], splits["dev"])
        classifier = model.named_steps["classifier"]
        print(
            f"[{model_name}] Selected max_depth={classifier.max_depth}, "
            f"min_samples_leaf={classifier.min_samples_leaf}, "
            f"max_features={classifier.max_features}, class_weight={classifier.class_weight}\n"
        )
        print(tuning.round(4).to_string(index=False), end="\n\n")
        tuning.to_csv(OUTPUT_DIR / f"{model_name}_tuning_results.csv", index=False)
        feature_importances(model).to_csv(
            OUTPUT_DIR / f"{model_name}_feature_importance.csv", index=False
        )
 
        for split, data in splits.items():
            predicted = model.predict(data[INPUTS])
            results = evaluate(data["target_class"], predicted)
            print(format_report(f"{model_name} {split}", results), end="\n\n")
 
            rows.append(metrics_row(model_name, split, results))
            pd.DataFrame(results["confusion_matrix"], index=CLASSES, columns=CLASSES).to_csv(
                OUTPUT_DIR / f"{model_name}_confusion_matrix_{split}.csv", index_label="true_class"
            )
            if split == "dev":
                predictions = data[CONTEXT_COLUMNS].assign(
                    true_class=data["target_class"], predicted_class=predicted
                )
                predictions.to_csv(OUTPUT_DIR / f"{model_name}_predictions_dev.csv", index=False)
 
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

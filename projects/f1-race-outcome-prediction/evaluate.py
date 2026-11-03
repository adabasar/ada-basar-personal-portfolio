"""Shared evaluation metrics for the CSCI 467 F1 project.

Every model should be scored with evaluate() so results are directly comparable:

    from evaluate import evaluate, format_report
    print(format_report("dev", evaluate(y_true, y_pred)))
"""

import numpy as np
from sklearn.metrics import accuracy_score, confusion_matrix, precision_recall_fscore_support


# Ordered from best to worst finish; error distance depends on this order.
CLASSES = ["podium", "top_ten", "outside_top_ten"]


def evaluate(y_true, y_pred) -> dict:
    """Return per-class P/R/F1, macro-F1, accuracy, confusion matrix, error distances."""
    y_true = np.asarray(y_true)
    y_pred = np.asarray(y_pred)
    # sklearn ignores labels outside CLASSES, which would silently inflate scores.
    unknown = (set(y_true) | set(y_pred)) - set(CLASSES)
    if unknown:
        raise ValueError(f"Unknown class labels: {sorted(map(str, unknown))}")

    precision, recall, f1, support = precision_recall_fscore_support(
        y_true, y_pred, labels=CLASSES, zero_division=0
    )
    confusion = confusion_matrix(y_true, y_pred, labels=CLASSES)
    distance = np.abs(np.subtract.outer(np.arange(3), np.arange(3)))
    return {
        "rows": len(y_true),
        "accuracy": accuracy_score(y_true, y_pred),
        "macro_f1": f1.mean(),
        "per_class": {
            name: {
                "precision": precision[i],
                "recall": recall[i],
                "f1": f1[i],
                "support": int(support[i]),
            }
            for i, name in enumerate(CLASSES)
        },
        # Rows are true classes and columns are predicted classes, in CLASSES order.
        "confusion_matrix": confusion,
        # Adjacent: podium/top_ten or top_ten/outside. Distant: podium/outside.
        "adjacent_errors": int(confusion[distance == 1].sum()),
        "distant_errors": int(confusion[distance == 2].sum()),
    }


def metrics_row(model: str, split: str, results: dict) -> dict:
    """Flatten evaluate() output into one row of a results table."""
    row = {
        "model": model,
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


def format_report(name: str, results: dict) -> str:
    lines = [
        f"=== {name} ({results['rows']} rows) ===",
        f"accuracy: {results['accuracy']:.4f}   macro-F1: {results['macro_f1']:.4f}",
        "",
        f"{'class':<18}{'precision':>10}{'recall':>10}{'f1':>10}{'support':>10}",
    ]
    for class_name, scores in results["per_class"].items():
        lines.append(
            f"{class_name:<18}{scores['precision']:>10.4f}{scores['recall']:>10.4f}"
            f"{scores['f1']:>10.4f}{scores['support']:>10d}"
        )
    lines += ["", "confusion matrix (rows = true, columns = predicted):"]
    lines.append(f"{'':<18}" + "".join(f"{c:>17}" for c in CLASSES))
    for class_name, row in zip(CLASSES, results["confusion_matrix"]):
        lines.append(f"{class_name:<18}" + "".join(f"{count:>17d}" for count in row))
    lines += [
        "",
        f"adjacent errors: {results['adjacent_errors']}   "
        f"distant errors (podium vs. outside): {results['distant_errors']}",
    ]
    return "\n".join(lines)

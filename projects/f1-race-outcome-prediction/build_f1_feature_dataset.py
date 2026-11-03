"""Build leakage-safe F1 pre-race features for the CSCI 467 project.

Usage:
    python3 build_f1_feature_dataset.py PATH_TO_DATASET_FOLDER

The folder must contain race_results.csv, qualifying_results.csv, races.csv,
circuits.csv, and drivers.csv from muharsyad/formula-one-datasets.
"""

from pathlib import Path
import sys

import numpy as np
import pandas as pd


START_YEAR = 2006
END_YEAR = 2020
TRAIN_END_YEAR = 2017
DEV_YEAR = 2018
TEST_START_YEAR = 2019
FORM_WINDOW = 5
OUTPUT_DIR = Path("f1_feature_output")


def target_label(position_text: object) -> str:
    """Use classified finish position; retirements/DQs are outside top ten."""
    position = pd.to_numeric(pd.Series([position_text]), errors="coerce").iloc[0]
    if pd.isna(position) or position > 10:
        return "outside_top_ten"
    if position <= 3:
        return "podium"
    return "top_ten"


def time_to_seconds(value: object) -> float:
    if pd.isna(value):
        return np.nan
    parts = str(value).split(":")
    try:
        if len(parts) == 2:
            return 60 * float(parts[0]) + float(parts[1])
        return float(parts[0])
    except ValueError:
        return np.nan


def add_driver_history(results: pd.DataFrame) -> pd.DataFrame:
    d = results.sort_values(["season", "round", "driverId"]).copy()
    d["driver_points_before_race"] = d.groupby(
        ["season", "driverId"], sort=False
    )["points"].transform(lambda x: x.cumsum().shift(fill_value=0))

    driver_group = d.groupby("driverId", sort=False)
    d["driver_starts_before"] = driver_group.cumcount()
    d["driver_recent_avg_finish"] = driver_group["position"].transform(
        lambda x: x.shift().rolling(FORM_WINDOW, min_periods=1).mean()
    )
    d["driver_recent_avg_points"] = driver_group["points"].transform(
        lambda x: x.shift().rolling(FORM_WINDOW, min_periods=1).mean()
    )
    d["driver_recent_top10_rate"] = driver_group["is_top10"].transform(
        lambda x: x.shift().rolling(FORM_WINDOW, min_periods=1).mean()
    )
    d["driver_recent_podium_rate"] = driver_group["is_podium"].transform(
        lambda x: x.shift().rolling(FORM_WINDOW, min_periods=1).mean()
    )
    d["driver_recent_dnf_rate"] = driver_group["is_dnf"].transform(
        lambda x: x.shift().rolling(FORM_WINDOW, min_periods=1).mean()
    )
    return d


def add_constructor_history(results: pd.DataFrame) -> pd.DataFrame:
    event_team = (
        results.groupby(["season", "round", "constructorId"], as_index=False)
        .agg(
            constructor_race_points=("points", "sum"),
            constructor_race_avg_finish=("position", "mean"),
            constructor_race_top10_rate=("is_top10", "mean"),
            constructor_race_podium_rate=("is_podium", "mean"),
            constructor_race_dnf_rate=("is_dnf", "mean"),
        )
        .sort_values(["season", "round", "constructorId"])
    )
    event_team["constructor_points_before_race"] = event_team.groupby(
        ["season", "constructorId"], sort=False
    )["constructor_race_points"].transform(lambda x: x.cumsum().shift(fill_value=0))

    team_group = event_team.groupby("constructorId", sort=False)
    for source, destination in [
        ("constructor_race_avg_finish", "constructor_recent_avg_finish"),
        ("constructor_race_points", "constructor_recent_avg_points"),
        ("constructor_race_top10_rate", "constructor_recent_top10_rate"),
        ("constructor_race_podium_rate", "constructor_recent_podium_rate"),
        ("constructor_race_dnf_rate", "constructor_recent_dnf_rate"),
    ]:
        event_team[destination] = team_group[source].transform(
            lambda x: x.shift().rolling(FORM_WINDOW, min_periods=1).mean()
        )
    return event_team


def add_circuit_history(results: pd.DataFrame) -> pd.DataFrame:
    d = results.sort_values(["season", "round", "driverId"]).copy()
    driver_circuit = d.groupby(["driverId", "circuitId"], sort=False)
    d["driver_circuit_starts_before"] = driver_circuit.cumcount()
    d["driver_circuit_avg_finish_before"] = driver_circuit["position"].transform(
        lambda x: x.shift().expanding().mean()
    )
    d["driver_circuit_avg_points_before"] = driver_circuit["points"].transform(
        lambda x: x.shift().expanding().mean()
    )

    # Aggregate to one row per constructor/event before shifting. Computing
    # this directly on driver rows would let the second teammate's row see the
    # first teammate's result from the current race.
    team_circuit_event = (
        d.groupby(
            ["season", "round", "constructorId", "circuitId"], as_index=False
        )
        .agg(constructor_circuit_event_avg_finish=("position", "mean"))
        .sort_values(["season", "round", "constructorId"])
    )
    team_circuit_event["constructor_circuit_avg_finish_before"] = (
        team_circuit_event.groupby(["constructorId", "circuitId"], sort=False)[
            "constructor_circuit_event_avg_finish"
        ].transform(lambda x: x.shift().expanding().mean())
    )
    d = d.merge(
        team_circuit_event[
            [
                "season",
                "round",
                "constructorId",
                "circuitId",
                "constructor_circuit_avg_finish_before",
            ]
        ],
        on=["season", "round", "constructorId", "circuitId"],
        how="left",
        validate="many_to_one",
    )
    return d


def main(data_dir: Path) -> None:
    results = pd.read_csv(data_dir / "race_results.csv")
    qualifying = pd.read_csv(data_dir / "qualifying_results.csv")
    races = pd.read_csv(data_dir / "races.csv")
    circuits = pd.read_csv(data_dir / "circuits.csv")
    drivers = pd.read_csv(data_dir / "drivers.csv")

    # Keep one pre-project season to initialize recent-form features. Much older
    # races include historical shared-drive records with multiple result rows
    # per driver, which are irrelevant to the 2006-2020 study period.
    results = results[results["season"] >= START_YEAR - 1].copy()
    qualifying = qualifying[qualifying["season"] >= START_YEAR].copy()
    races = races[races["season"] >= START_YEAR - 1].copy()

    results = results.merge(
        races[["season", "round", "raceName", "circuitId", "date"]],
        on=["season", "round"],
        how="left",
        validate="many_to_one",
    )
    results["is_classified"] = pd.to_numeric(
        results["positionText"], errors="coerce"
    ).notna()
    results["is_top10"] = (
        results["is_classified"] & results["positionText"].astype(str).str.fullmatch(r"[1-9]|10")
    ).astype(int)
    results["is_podium"] = (
        results["is_classified"] & results["positionText"].astype(str).isin(["1", "2", "3"])
    ).astype(int)
    results["is_dnf"] = (~results["is_classified"]).astype(int)

    results = add_driver_history(results)
    team_history = add_constructor_history(results)
    results = add_circuit_history(results)

    team_feature_columns = [
        "season", "round", "constructorId", "constructor_points_before_race",
        "constructor_recent_avg_finish", "constructor_recent_avg_points",
        "constructor_recent_top10_rate", "constructor_recent_podium_rate",
        "constructor_recent_dnf_rate",
    ]
    results = results.merge(
        team_history[team_feature_columns],
        on=["season", "round", "constructorId"],
        how="left",
        validate="many_to_one",
    )

    qualifying = qualifying.rename(
        columns={"position": "qualifying_position", "Q1": "q1", "Q2": "q2", "Q3": "q3"}
    )
    for stage in ["q1", "q2", "q3"]:
        qualifying[f"{stage}_seconds"] = qualifying[stage].map(time_to_seconds)

    result_feature_columns = [
        "season", "round", "driverId", "constructorId", "position",
        "positionText", "grid", "status", "points", "raceName", "circuitId",
        "date", "driver_points_before_race", "driver_starts_before",
        "driver_recent_avg_finish", "driver_recent_avg_points",
        "driver_recent_top10_rate", "driver_recent_podium_rate",
        "driver_recent_dnf_rate", "constructor_points_before_race",
        "constructor_recent_avg_finish", "constructor_recent_avg_points",
        "constructor_recent_top10_rate", "constructor_recent_podium_rate",
        "constructor_recent_dnf_rate", "driver_circuit_starts_before",
        "driver_circuit_avg_finish_before", "driver_circuit_avg_points_before",
        "constructor_circuit_avg_finish_before",
    ]
    data = qualifying.merge(
        results[result_feature_columns],
        on=["season", "round", "driverId", "constructorId"],
        how="inner",
        validate="one_to_one",
    )
    data = data.merge(
        circuits[["circuitId", "circuitName", "lat", "long", "locality", "country"]],
        on="circuitId", how="left", validate="many_to_one"
    )
    data = data.merge(
        drivers[["driverId", "dateOfBirth", "nationality"]],
        on="driverId", how="left", validate="many_to_one"
    )
    data["race_date"] = pd.to_datetime(data["date"])
    data["dateOfBirth"] = pd.to_datetime(data["dateOfBirth"])
    data["driver_age"] = (
        (data["race_date"] - data["dateOfBirth"]).dt.days / 365.25
    )
    data["target_class"] = data["positionText"].map(target_label)
    data = data[data["season"].between(START_YEAR, END_YEAR)].copy()
    data = data.sort_values(["season", "round", "qualifying_position"])

    train = data[data["season"] <= TRAIN_END_YEAR].copy()
    dev = data[data["season"] == DEV_YEAR].copy()
    test = data[data["season"] >= TEST_START_YEAR].copy()

    OUTPUT_DIR.mkdir(exist_ok=True)
    data.to_csv(OUTPUT_DIR / "f1_features_all_2006_2020.csv", index=False)
    train.to_csv(OUTPUT_DIR / "f1_features_train_2006_2017.csv", index=False)
    dev.to_csv(OUTPUT_DIR / "f1_features_dev_2018.csv", index=False)
    test.to_csv(OUTPUT_DIR / "f1_features_test_2019_2020.csv", index=False)

    summaries = []
    for name, split in [("train", train), ("dev", dev), ("test", test)]:
        counts = split["target_class"].value_counts()
        summaries.append({
            "split": name,
            "years": f"{split.season.min()}-{split.season.max()}",
            "rows": len(split),
            "races": split[["season", "round"]].drop_duplicates().shape[0],
            "podium": counts.get("podium", 0),
            "top_ten": counts.get("top_ten", 0),
            "outside_top_ten": counts.get("outside_top_ten", 0),
        })
    summary = pd.DataFrame(summaries)
    summary.to_csv(OUTPUT_DIR / "split_class_counts.csv", index=False)
    print(summary.to_string(index=False))
    print(f"\nRows: {len(data)}; columns: {len(data.columns)}")
    print(f"Duplicate driver-race rows: {data.duplicated(['season', 'round', 'driverId']).sum()}")
    print(f"Output: {OUTPUT_DIR.resolve()}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python3 build_f1_feature_dataset.py PATH_TO_DATASET_FOLDER")
    main(Path(sys.argv[1]))

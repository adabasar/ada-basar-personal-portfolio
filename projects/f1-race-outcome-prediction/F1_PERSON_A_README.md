# CSCI 467 F1 Midterm: Person A Data Package

## Run the pipeline

Keep `build_f1_feature_dataset.py` beside the downloaded dataset folder, then run:

```bash
python3 build_f1_feature_dataset.py formula-one-datasets-main
```

This creates `f1_feature_output/` containing the full dataset, chronological
train/dev/test files, and the report-ready split/class counts.

## Current chronological split

- Train: 2006-2017
- Development: 2018
- Test: 2019-2020

Confirm these boundaries with the group before finalizing experiments. If the
group changes them, edit the constants at the top of the script and rerun it.

## Target

- `podium`: classified P1-P3
- `top_ten`: classified P4-P10
- `outside_top_ten`: P11+, retirement, disqualification, withdrawal, or other
  unclassified result

## Important pre-race features

- `qualifying_position`, `q1_seconds`, `q2_seconds`, `q3_seconds`
- `grid`: official starting-grid position after penalties
- `driver_points_before_race`
- `constructor_points_before_race`
- Driver rolling five-race finish, points, top-ten, podium, and DNF statistics
- Constructor rolling five-race finish, points, top-ten, podium, and DNF statistics
- Driver and constructor circuit-history statistics
- Driver experience and age
- Circuit ID, location, country, latitude, and longitude

## Leakage protection

Every rolling or cumulative feature is shifted by one race. Current-race
`position`, `positionText`, `points`, and `status` are retained only for the
target and error analysis; teammates must not include them in model inputs.

## Missing values

Historical features are naturally missing for rookie drivers, new teams, and
drivers or teams visiting a circuit for the first time. Modeling pipelines
should impute numeric missing values using training-set medians and optionally
add missing-value indicators. Do not calculate imputation values using the
development or test sets.

# Predicting Formula 1 Race Outcomes

A machine learning project that predicts, before a race starts, whether a driver will finish on the **podium (P1–P3)**, in the **top ten (P4–P10)**, or **outside the top ten** (including retirements).

**Team project, USC CSCI 467 (Introduction to Machine Learning), Fall 2026.**
Original repository: [Childishbambi/CSCI-467-Midterm-Project](https://github.com/Childishbambi/CSCI-467-Midterm-Project)

## My role
I built the data pipeline (`build_f1_feature_dataset.py`): it turns raw Formula 1 data from 2006–2020 into a clean, leak-free feature dataset with chronological train / dev / test splits.

## Approach
- **Features:** qualifying position and lap times, grid position, championship points before the race, rolling five-race form for drivers and teams, circuit history, driver experience and age, and circuit location
- **No data leakage:** every rolling or cumulative feature is shifted by one race, so a model only sees what was known before the start
- **Splits by time:** train 2006–2017, dev 2018, test 2019–2020
- **Models:** qualifying-position baseline, logistic regression, random forest and extra trees
- **Shared evaluation:** `evaluate.py` reports accuracy, macro-F1, per-class precision/recall, and how far off wrong predictions are

## Results so far (dev set, 2018)
| Model | Accuracy | Macro-F1 |
|---|---|---|
| Qualifying baseline | 0.671 | 0.666 |
| Logistic regression | 0.662 | 0.660 |

## Tech stack
Python · pandas · NumPy · scikit-learn · matplotlib

## Run it
```bash
python3 build_f1_feature_dataset.py formula-one-datasets-main
python3 baseline_qualifying.py
python3 train_logistic_regression.py
```
More detail on the dataset is in `F1_PERSON_A_README.md`.

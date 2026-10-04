import csv

PATH = "evaluation/results/results_v1.csv"
YES = {1, 2, 4, 5, 11, 13, 33, 35, 7, 6, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
       24, 25, 26, 27, 28, 29, 30, 31, 37, 38, 39, 40, 41}
NO = {3, 8, 9, 10, 12, 34, 36, 32}

with open(PATH, newline="", encoding="utf-8-sig") as f:
    reader = csv.DictReader(f)
    fields = reader.fieldnames
    rows = list(reader)

for r in rows:
    i = int(r["id"])
    if i in YES:
        r["manual_correct"] = "Y"
    elif i in NO:
        r["manual_correct"] = "N"
    else:
        raise SystemExit("No grade for id %d" % i)

with open(PATH, "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=fields)
    w.writeheader()
    w.writerows(rows)
print("graded", len(rows), "rows; Y =", sum(r["manual_correct"] == "Y" for r in rows))
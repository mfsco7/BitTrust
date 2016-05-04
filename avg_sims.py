import csv
import pandas as pd

net_size = 30
algo = "ORIGINAL"
# algo = "TRUST"
# algo = "TRUST2"
pc_free = [10, 20, 30]


def avg(array: list):
    return sum(array) / len(array)


file_name = "log/%d/%s/st.csv" % (net_size, algo)
with open(file_name, 'w', newline='') as csv_file2:
    writer = csv.writer(csv_file2, delimiter=';')
    writer.writerow(["n freerider", "FreeRider AVG", "Normal avg"])

    for i in pc_free:
        file_name2 = "log/%d/%s/%d/st.csv" % (net_size, algo, net_size * i /100)
        df = pd.read_csv(file_name2, sep=';')
        free = sum(df["FreeRiderTime"]) / len(df["FreeRiderTime"])
        normal = avg(df["NormalTime"])
        writer.writerow([i, free, normal])


import matplotlib.pyplot as plt
import pandas as pd

node_type = "normal"
# node_type = "freerider"
trust_algorithm = 1
# trust_algorithm = 2

csv_folder = "/home/aferreira/Documentos/Hyrax/"

df = pd.read_csv(csv_folder + "st20160407Original.csv", sep=";")

if trust_algorithm == 1:
    df2 = pd.read_csv(csv_folder + "st20160407Trust.csv", sep=";")
else:
    '''if talgorithm == 2'''
    df2 = pd.read_csv(csv_folder + "st20160407Trust2.csv", sep=";")

x = df['n freerider']
x2 = df2['n freerider']
if node_type == "normal":
    y = df['normal avg']
    y2 = df2['Normal avg']
elif node_type == "freerider":
    y = df['FreeRider AVG']
    y2 = df2['FreeRider AVG']

# y_2 = []
# for i in y:
#     print(i)
#     y_2 += [float(i)]
#     print(type(float(i)))

# y2_2 = []
# for i in y2:
#     y2_2 += [float(i)]
#     print(type(float(i)))

plt.plot(x, y, 'r', label='Original')
plt.plot(x2, y2, 'b', label='Trust')
plt.legend(loc=0)
plt.grid(True)
plt.xlabel("Number of FreeRiders")
plt.ylabel("Avg Download Time [ms]")
plt.title("Unchoking Algorithms: Original vs Trust")
plt.show()

# time = datetime.now().strftime('%Y%m%d_%H%M%S')
# plt.savefig("sim_times_" + time + "_" + node_type + "_" + ".png")

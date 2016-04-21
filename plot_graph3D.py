from mpl_toolkits.mplot3d import Axes3D
import matplotlib.pyplot as plt
import pandas as pd

node_type = "normal"
# node_type = "freerider"
trust_algorithm = 1
# trust_algorithm = 2

csv_folder = "/home/aferreira/Documentos/Hyrax/"

df = pd.read_csv(csv_folder + "st20160407Original.csv", sep=";")

# if trust_algorithm == 1:
df2 = pd.read_csv(csv_folder + "st20160407Trust.csv", sep=";")
# else:
'''if talgorithm == 2'''
df3 = pd.read_csv(csv_folder + "st20160407Trust2.csv", sep=";")

x = df['n freerider']
x2 = df2['n freerider']
x3 = df3['n freerider']
if node_type == "normal":
    y = df['normal avg']
    y2 = df2['Normal avg']
    y3 = df3['Normal avg']
elif node_type == "freerider":
    y = df['FreeRider AVG']
    y2 = df2['FreeRider AVG']
    y3 = df3['FreeRider AVG']

# y_2 = []
# for i in y:
#     print(i)
#     y_2 += [float(i)]
#     print(type(float(i)))

# y2_2 = []
# for i in y2:
#     y2_2 += [float(i)]
#     print(type(float(i)))

# plt.plot(x, y, 'r', label='Original')
# plt.plot(x2, y2, 'b', label='Trust')

fig = plt.figure()
ax = fig.gca(projection='3d')

z = [30] * len(x)
z2 = [30] * len(x)
z3 = [40] * len(x)

xlen = len(x)
ylen = len(y)

# colortuple = ('y', 'b')
# colors = np.empty(x.shape, dtype=str)
# for y0 in range(ylen):
#     for x0 in range(xlen):
#         colors[x0, y0] = colortuple[(x0 + y0) % len(colortuple)]

# surf = ax.plot(x, z, y, color='g', antialiased=False)
# line2 = ax.plot(x2, z2, y2, color='b', antialiased=False)

# ax.plot(x, z3, y, color='g', antialiased=False)
# ax.plot(x3, z3, y3, color='b', antialiased=False)


surf = ax.plot_surface([x, x3], [z, z3], [y, y3], rstride=1, cstride=1, color='y',
                       linewidth=1, antialiased=False)

# surf2 = ax.plot_surface([x2, x3], [z2, z3], [y2, y3], rstride=1, cstride=1,
#                         linewidth=1, antialiased=False)

# surf = ax.plot_wireframe([x, x3], [z, z3], [y, y3], rstride=19, cstride=19, color='y',
#                          linewidth=1, antialiased=False)

# surf = ax.plot_trisurf(x, z, y, rstride=19, cstride=19, color='y',
#                        linewidth=1, antialiased=False)

# ax.set_zlim3d(-1, 1)
# ax.w_zaxis.set_major_locator(LinearLocator(6))




# gca.plot_surface(x, 30, y)
# gca.plot_surface(x2, 40, y2)
# gca.plot_surface(x, 40, y)
# gca.legend(loc=0)
# .grid(True)
ax.view_init(10, -135)
plt.xlabel("Number of FreeRiders")
plt.ylabel("Network Size")
# plt.title("Unchoking Algorithms: Original vs Trust")

plt.show()

# time = datetime.now().strftime('%Y%m%d_%H%M%S')
# plt.savefig("sim_times_" + time + "_" + node_type + "_" + ".png")

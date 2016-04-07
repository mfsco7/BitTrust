from datetime import datetime
from math import floor, ceil
from matplotlib.pyplot import plot, savefig
from numpy import arange
from os import mkdir
from os.path import exists
from psutil import Popen
from shutil import copyfile
from statistics import stdev, mean
from subprocess import PIPE

import csv

# from threading import Lock

peersimLibraries = "../peersim-1.0.5/*"
commonsmath35Library = "../commons-math3-3.5/commons-math3-3.5.jar"
bitTrustOutDir = "out/production/BitTrust"

libraries = peersimLibraries + ":" + commonsmath35Library + ":" + bitTrustOutDir

cfgFile = "conf/Time.conf"

simulation = 0
confidence = 0.95
zvalue = 1.96
min_interval_range = 0.05
mis = min_interval_range / 2  # Min interval spread
interval_spread = {}

cfgfiles = ["Time30t1f.conf", "Time30t3f.conf", "Time30t5f.conf", "Time30t10f.conf",
            "Time30t15f.conf", "Time30t20f.conf", "Time30t25f.conf", "Time30T1f.conf",
            "Time30T3f.conf", "Time30T5f.conf", "Time30T10f.conf", "Time30T15f.conf",
            "Time30T20f.conf", "Time30T25f.conf"]

nnodesToRun = [30]
percemfreeRidersToRUn = [0, 1, 2, 3, 5, 10, 15, 20, 25]
algorithmsToRun = ["original", "trust", "trust2"]


def simulate(lock):
    """ Function that each thread will run. It will run one simulation with our trust algorithm
    and one with the original algorithm for at least 30 times and continues simulating while the
    interval spread is greater than the minimum interval spread.

    :param lock: Locker to ensure mutual exclusion
    :return: Nothing
    """

    global simulation, downtime, interval_spread
    interval_small = False
    #    while (simulation < 30) or (not interval_small):
    #    for i in range(1):
    try:
        lock.acquire(timeout=3)
        sim = simulation
        print("simulation " + str(sim) + " starting")

        # rand_seed = randint(1, 2 ** 63 - 1)
        # cfg_file2 = generate_conf_file(rand_seed, sim)
        simulation += 1
        lock.release()

        # last_time = 0
        print(cfgfiles[sim])
        last_time = run_process("conf/" + cfgfiles[sim])
        # last_time = {'FREE_RIDER': randint(0, 7 ** 7), 'NORMAL': randint(0, 7 ** 7)}

        lock.acquire(timeout=3)

        """ see and change confidence here """
        # for key in last_time.keys():
        #    downtime[key] += [last_time[key]]
        #    if len(downtime[key]) > 1:
        #        interval_spread[key] = calc_interval(downtime[key])
        #        if interval_spread[key] < mis:
        #            interval_small = True

        # with open('csv/simulationTimes' + algorithm + '.csv', 'a+', newline='') as \
        #                    csv_file2:
        #    writer2 = csv.writer(csv_file2, delimiter=';')

        #    writer2.writerow([rand_seed] + [time for time in last_time.values()] +
        #                     [amplitude for amplitude in interval_spread.values()])
        print("simulation " + str(sim) + " finishes")
        lock.release()

    except TimeoutError:
        lock.release()


def simulate2(num_nodes, nfreerider2, algorithm2):
    global simulation
    sim = simulation
    print("simulation " + str(sim) + " starting")

    # rand_seed = randint(1, 2 ** 63 - 1)
    # cfg_file2 = generate_conf_file(rand_seed, sim)
    cfg_file2 = generate_conf_file2(1234567890, algorithm2, num_nodes, nfreerider2)
    simulation += 1

    # last_time = 0
    print(cfg_file2)
    last_time = run_process(cfg_file2)
    # last_time = {'FREE_RIDER':  7 ** 7, 'NORMAL': 7 ** 7}

    """ see and change confidence here """
    # for key in last_time.keys():
    #    downtime[key] += [last_time[key]]
    #    if len(downtime[key]) > 1:
    #        interval_spread[key] = calc_interval(downtime[key])
    #        if interval_spread[key] < mis:
    #            interval_small = True

    #
    # writer.writerow([time for time in last_time.values()])
    tmp = [cfg_file2] + [str(time) for time in last_time.values()]
    print(tmp)

    with open('csv/simulationTimes20160407.csv', 'a+', newline='') as csv_file2:
        writer2 = csv.writer(csv_file2, delimiter=';')
        writer2.writerow(tmp)
    #
    # writer2.writerow([cfg_file2] + [time for time in last_time.values()])
    print("simulation " + str(sim) + " finishes")


def is_interval_small():
    """
    Check, for all types of nodes, if the interval amplitude is smaller than it is desired
    :return:  True if interval amplitude is smaller, False otherwise
    """
    for interval in interval_spread.values():
        if interval > mis:
            return False
    return True


def generate_conf_file(rand_seed=1234567890, sim=0):
    """
    Make new configuration file based on template cfgFile. It will take a integer to
    be a random seed parameter and produce a file for passing to peersim simulator.

    :param rand_seed: Integer to be used as random seed
    :param sim: Simulation Number
    :return: Names of Configuration files
    """
    cfg_file2 = "conf/Time" + algorithm + str(sim) + ".conf"
    copyfile(cfgFile, cfg_file2)
    with open(cfg_file2, mode="a") as file:
        file.write("# This part was generated by the script\n")
        file.write("random.seed " + str(rand_seed) + "\n")
        file.write("protocol.simulation.unchoking" + algorithm + "\n")

    return cfg_file2


def generate_conf_file2(rand_seed=1234567890, algorithmm='original', net_size=100,
                        freerider=0):
    """
    Make new configuration file based on template cfgFile. It will take a integer to
    be a random seed parameter and produce a file for passing to peersim simulator.

    :param freerider:
    :param net_size:
    :param algorithmm:
    :param rand_seed: Integer to be used as random seed
    :param sim: Simulation Number
    :return: Names of Configuration files
    """
    cfg_file2 = "conf/Time" + algorithmm + "_" + str(nfreerider) + "f.conf"
    copyfile(cfgFile, cfg_file2)
    with open(cfg_file2, mode="a") as file:
        file.write("# This part was generated by the script\n")
        file.write("random.seed " + str(rand_seed) + "\n")
        file.write("network.size " + str(net_size) + "\n")
        file.write("protocol.simulation.unchoking " + algorithmm + "\n")
        file.write("init.net.nFreeRider " + str(freerider) + "\n")

    return cfg_file2


def run_process(cfg_file="conf/Time-1.conf", seed=1234567890):
    """
    Creates nodeID new process and waits for it to end. After process termination it will fetch the
    last node download time.

    :param cfg_file: Name of configuration file
    :param seed:
    :return: Time that last node took to fetch the file
    """
    p = Popen(["java", "-cp", libraries, "peersim.Simulator", cfg_file], stdout=PIPE, stderr=PIPE,
              universal_newlines=True)
    # p = Popen(["ls", cfg_file], stdout=PIPE, stderr=PIPE)

    stdout = p.communicate()[0]
    # stdout = '\n\nNode 2 is NORMAL\nNode 3 is NORMAL'
    # last_peer = stdout.split(" at time ")[-1]
    # last_time = int(last_peer.split('\n')[0])
    #
    # return last_time
    print(type(stdout))
    # p.wait()
    # stdout = stdout.decode("utf-8")

    down_times = {}

    nodes_completed = {}

    algo = ""
    if algorithm == "original":
        algo = "ORIGINAL"
    elif algorithm == 'trust':
        algo = "TRUST"
    elif algorithm == "trust2":
        algo = "TRUST2"

    with open('csv/s' + str(30) + algo + '_' + str(nfreerider) + '.csv', newline='') as csv_file_p:
        spam_reader = csv.reader(csv_file_p, delimiter=';')
        for row in spam_reader:
            [node, node_type, time] = row
            if node_type in down_times:
                down_times[node_type] += [int(time)]
            else:
                down_times[node_type] = [int(time)]
            nodes_completed[int(node)] = [True]
    #
    for n in range(2, 30):
        if n not in nodes_completed:
            string = "Node " + str(n) + " is "
            temp0 = stdout.split(string)
            temp = temp0[1]
            n_type = temp.split("\n")[0]
            if n_type not in down_times:
                down_times[n_type] = [10 ** 7]
            else:
                down_times[n_type] += [10 ** 7]

    for key in down_times.keys():
        # print(key, down_times[key])
        down_times[key] = sum(down_times[key]) / len(down_times[key])
        # print(key, down_times[key])
    # down_times = {'FREE_RIDER': 2000000, 'NORMAL': 1500000}
    return down_times


def calc_interval(down_time: list):
    """
    Computes the Interval Spread from the values contained on down_time. The interval spread is
    calculated by this formula: error / mean(values), with error as z-value * (stdev * sqrt(
    |values|)), z-value as 1.96 and stdev is standard deviation.

    :param down_time: All last downtime of all finished simulations
    :return: The Interval Spread
    """

    mean0 = mean(down_time)
    std0 = stdev(down_time)
    sqrt = len(down_time) ** (1 / 2)
    error = zvalue * (std0 / sqrt)
    # print(mean0, error, (mean0-error, mean0+error))
    return error / mean0


def calc_avg(num_group=100):
    """
    Calculates the average of downtime arrays. This method returns a smaller representation of
    points from two downtime arrays. It first splits each array, independently, in num_groups
    groups. Then it calculates for each group the average between the values. After this,
    this method joins and returns the values in the form of two arrays.

    :type num_group: int
    :param num_group: Maximum number of groups that will be created
    :return: Two arrays with the Averages of the two given downtime arrays
    """
    size = len(downtime)

    min_points = floor(size / num_group)
    max_points = ceil(size / num_group)

    n_cluster_max = size % num_group

    sim = 0
    x2, y2 = [], []
    cluster = n_cluster_max * [max_points] + (num_group - n_cluster_max) * [min_points]
    for nPoints in cluster:
        x2 += [sum(downtime[sim:sim + nPoints]) / nPoints]
        # y2 += [sum(downtime2[sim:sim + nPoints]) / nPoints]
        sim += nPoints
    return x2, y2


def plot_graph(x: list, y: list):
    """
    Draw the graph from the downtimes and save it to a file

    :param x: Downtime of trust approach
    :param y: Downtime of original approach
    """
    plot(arange(len(x)), x, '.', arange(len(y)), y)
    time = datetime.now().strftime('%Y%m%d_%H%M%S')
    savefig("simulation_times_avg" + time + ".png")


if __name__ == '__main__':
    #    nProcessors = cpu_count()
    nProcessors = 1

    #    locker = Lock()
    # algorithms = ["original"]

    if not exists("csv"):
        mkdir("csv", 0o744)

        # for cfgfile in cfgfiles:
        # for j in range(1):
    t = []
    simulation = 0
    downtime = {'FREE_RIDER': [], 'NORMAL': []}

    folder_name = "csv"
    if not exists(folder_name):
        mkdir(folder_name, 0o744)

    # print("Simulations with " + algorithm + " started")

    # with open('csv/simulationTimes20160.csv', 'w+', newline='\n') as csv_file0:
    #     csv_file0.write("bla\n")

    """ Creates new csv file or overwrites the old """
    # with open('csv/simulationTimes20160406.csv', 'w') as csv_file:
    #     writer = csv.writer(csv_file, delimiter=';')
    #     writer.writerow(['Random Seed', 'NormalTime', 'FreeRiderTime', 'NormalSpread',
    #                      'FreeRiderSpread'])

    for nnodes in nnodesToRun:
        for algorithm in algorithmsToRun:
            for nfreerider in percemfreeRidersToRUn:
                simulate2(nnodes, nfreerider, algorithm)

                # writer.writerow([])
                # writer.writerow([])

                # """ Create threads to control simulation """
                # for i in range(nProcessors):
                # print(cfgfile)
                # t.append(Thread(target=simulate, args=(locker,)))
                # t[i].start()
#
# """ Waits for threads to finish """
# for i in range(nProcessors):
#     t[i].join()

# avg_downtime, avg_downtime2 = calc_avg()
# plot_graph(avg_downtime, avg_downtime2)

print("done")

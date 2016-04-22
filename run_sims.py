from os import mkdir
from os.path import exists
from random import randint
from shutil import copyfile
from statistics import stdev, mean
from subprocess import PIPE
from threading import Lock, Thread, current_thread

from psutil import Popen, cpu_count

import csv

peersimLibraries = "../peersim-1.0.5/*"
commonsmath35Library = "../commons-math3-3.5/commons-math3-3.5.jar"
bitTrustOutDir = "out/production/BitTrust"

libraries = peersimLibraries + ":" + commonsmath35Library + ":" + bitTrustOutDir

cfgFile = "conf/Time.conf"

simulation = 0

'''Confidence Level'''
confidence = 0.95
zvalue = 1.96
min_interval_range = 0.05
mis = min_interval_range / 2  # Min interval spread
interval_spread = {}

'''Configuration parameters'''
# network_sizes = [20, 30, 40, 50, 60, 70, 80, 90, 100]
network_sizes = [30]
# percentages_freeriders = [0, 10, 20, 30, 40, 50]
percentages_freeriders = [0]
# algorithms = ["original", "trust", "trust2"]
algorithms = ["original"]


def simulate(lock, tasklist):
    global simulation

    sim_group = 0
    n_groups = len(tasklist)

    while sim_group < n_groups:
        while simulation < 30 or not is_interval_small():
            # TODO convert tasklist to a dict for better mapping


            lock.acquire()
            sim = simulation
            print("simulation " + str(sim) + " starting in " + current_thread().name +
                  " " + str(tasklist[sim_group]) + " nodes " + str(algorithm2) + " " + str(nfreerider2) +
                  " free riders")

            rand_seed = randint(1, 2 ** 63 - 1)
            cfg_file2 = generate_conf_file(rand_seed, tasklist[sim_group], )
            simulation += 1
            lock.release()

        sim_group += 1

    while True:
        try:
            lock.acquire(timeout=3)
            sim = simulation
            simulation += 1
            lock.release()

            if sim >= n_groups:
                # print(threading.current_thread().name + " finishes")
                break
            else:

                [num_nodes, algorithm2, nfreerider2] = tasklist[sim]
                cfg_file2 = generate_conf_file(1234567890, num_nodes, algorithm2, nfreerider2)
                print("simulation " + str(sim) + " starting in " + current_thread().name +
                      " " + str(num_nodes) + " nodes " + str(algorithm2) + " " + str(nfreerider2) +
                      " free riders")
                # print(threading.current_thread().name + " " + cfg_file2)

                last_time = run_process(cfg_file2, task=[num_nodes, algorithm2, nfreerider2])
                # print(num_nodes, algorithm2, nfreerider2)

                lock.acquire(timeout=3)

                with open('csv/simulationTimes' + algorithm2 + '20160421.csv', 'a+', newline='') \
                        as \
                        csv_file2:
                    writer2 = csv.writer(csv_file2, delimiter=';')
                    row = [cfg_file2, nfreerider2] + [str(time) for time in last_time.values()]
                    # print(row)
                    writer2.writerow(row)
                #
                # writer2.writerow([cfg_file2] + [time for time in last_time.values()])
                print("simulation " + str(sim) + " finishes")
                lock.release()

        except TimeoutError:
            lock.release()

    return


def is_interval_small():
    """
    Check, for all types of nodes, if the interval amplitude is smaller than it is desired
    :return:  True if interval amplitude is smaller, False otherwise
    """
    for interval in interval_spread.values():
        if interval > mis:
            return False
    return True


def generate_conf_file(rand_seed=1234567890, net_size=100, algorithmm='original',
                       freerider=0, task: list = None):
    """
    Make new configuration file based on template cfgFile. It will take a integer to
    be a random seed parameter and produce a file for passing to peersim simulator.

    :param rand_seed: Integer to be used as random seed
    :param net_size:
    :param algorithmm:
    :param freerider:
    :param task:
    :return: Names of Configuration files
    """
    if task is not None:
        net_size = task[0]
        algorithmm = task[1]
        freerider = task[2]
    cfg_file2 = "conf/Time" + str(net_size) + algorithmm + "_" + str(freerider) + "f.conf"
    copyfile(cfgFile, cfg_file2)
    with open(cfg_file2, mode="a") as file:
        file.write("# This part was generated by the script\n")
        file.write("random.seed " + str(rand_seed) + "\n")
        file.write("network.size " + str(net_size) + "\n")
        file.write("protocol.simulation.unchoking " + algorithmm + "\n")
        file.write("init.net.nFreeRider " + str(freerider) + "\n")

    return cfg_file2


def run_process(cfg_file="conf/Time-1.conf", seed=1234567890, task: list = None):
    """
    Creates nodeID new process and waits for it to end. After process termination it will fetch the
    last node download time.

    :param task:
    :param cfg_file: Name of configuration file
    :param seed:
    :return: Time that last node took to fetch the file
    """
    if task is None:
        task = [30, "original", 0]
    p = Popen(["java", "-cp", libraries, "peersim.Simulator", cfg_file], stdout=PIPE, stderr=PIPE,
              universal_newlines=True)
    # p = Popen(["ls", cfg_file], stdout=PIPE, stderr=PIPE)

    stdout = p.communicate()[0]
    # stdout = '\n\nNode 2 is NORMAL\nNode 3 is NORMAL'
    # last_peer = stdout.split(" at time ")[-1]
    # last_time = int(last_peer.split('\n')[0])
    #
    # print(type(stdout))
    # p.wait()
    # stdout = stdout.decode("utf-8")

    down_times = {}

    nodes_completed = {}

    algo = ""
    if task[1] == "original":
        algo = "ORIGINAL"
    elif task[1] == 'trust':
        algo = "TRUST"
    elif task[1] == "trust2":
        algo = "TRUST2"

    csv_name = 'csv/' + str(task[0]) + algo + '_' + str(task[2]) + '.csv'
    with open(csv_name, newline='') as \
            csv_file_p:
        spam_reader = csv.reader(csv_file_p, delimiter=';')
        for row in spam_reader:
            [node, node_type, time] = row
            if node_type in down_times:
                down_times[node_type] += [int(time)]
            else:
                down_times[node_type] = [int(time)]
            nodes_completed[int(node)] = [True]
    #
    for n in range(2, task[0], 1):
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


if __name__ == '__main__':
    locker = Lock()

    nProcessors = cpu_count()
    # nProcessors = 16

    t = []
    simulation = 0
    downtime = {'FREE_RIDER': [], 'NORMAL': []}

    folder_name = "csv"
    if not exists(folder_name):
        mkdir(folder_name, 0o744)

    """ Creates new csv file or overwrites the old """
    # with open('csv/simulationTimes20160411.csv', 'w') as csv_file:
    #     writer = csv.writer(csv_file, delimiter=';')
    #     writer.writerow(['Random Seed', 'NormalTime', 'FreeRiderTime', 'NormalSpread',
    #                      'FreeRiderSpread'])

    tasks = []

    for nnodes in network_sizes:
        for algorithm in algorithms:
            for percentage in percentages_freeriders:
                num_freeriders = (percentage / 100) * nnodes
                tasks += [[nnodes, algorithm, num_freeriders]]

    for i in range(nProcessors):
        t.append(Thread(target=simulate, args=(locker, tasks)))
        t[i].start()

    """ Waits for threads to finish """
    for i in range(nProcessors):
        t[i].join()

    print("done")

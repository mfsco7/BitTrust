import csv
from datetime import datetime
from os import makedirs
from os.path import exists
from random import randint
from shutil import copyfile
from statistics import stdev, mean
from subprocess import PIPE
from threading import Lock, Thread, current_thread

import pandas
from psutil import Popen, cpu_count

peersimLibraries = "../peersim-1.0.5/*"
commonsmath35Library = "../commons-math3-3.5/commons-math3-3.5.jar"
bitTrustOutDir = "out/production/BitTrust"

libraries = peersimLibraries + ":" + commonsmath35Library + ":" + bitTrustOutDir

cfgFile = "conf/Time.conf"

simulation = []
down_times = []

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
percentages_freeriders = [10]
# algorithms = ["ORIGINAL", "TRUST", "TRUST2"]
algorithms = ["ORIGINAL"]


def create_log_files(rand_seed, task):
    folder_name = "log/%d/%s/%d/%d/" % (task["net_size"], task["algo"], task["num_free"], rand_seed)

    if not exists(folder_name):
        makedirs(folder_name)
        # mkdir(folder_name, 0o744)

    file_name = folder_name + "NodeTypes.csv"
    with open(file_name, 'w+', newline='') as \
            csv_file2:
        writer2 = csv.writer(csv_file2, delimiter=';')

        num_normal = int(task["net_size"] - task["num_free"])

        writer2.writerow(["NodeID", "NodeType"])
        for i in range(1, num_normal):
            writer2.writerow([str(i), "NORMAL"])

        for j in range(num_normal, num_normal + int(task["num_free"])):
            writer2.writerow([str(j), "FREE_RIDER"])

    file_name2 = folder_name + "DownTimes.csv"

    with open(file_name2, 'w+', newline='') as \
            csv_file2:
        writer2 = csv.writer(csv_file2, delimiter=';')

        writer2.writerow(["NodeID", "NodeType", "DownloadTime"])
        for i in range(1, num_normal):
            writer2.writerow([str(i), "NORMAL", randint(10 ** 6, 10 ** 7)])

        for j in range(num_normal, num_normal + int(task["num_free"])):
            writer2.writerow([str(j), "FREE_RIDER", randint(10 ** 6, 10 ** 7)])


def simulate(lock, task_list: list) -> None:
    global simulation, down_times

    sim_group = 0
    n_groups = len(task_list)

    time = datetime.now().strftime('%Y%m%d')
    while sim_group < n_groups:
        down_times += [{"NORMAL": [], "FREE_RIDER": []}]
        task = task_list[sim_group]

        file_name = "log/%d/%s/%d/st.csv" % (task["net_size"], task["algo"], task["num_free"])
        with open(file_name, 'a', newline='') as csv_file2:
            writer2 = csv.writer(csv_file2, delimiter=';')

            while simulation[sim_group] < 30 or not is_interval_small():
                lock.acquire()
                sim = simulation[sim_group]
                rand_seed = randint(2 ** 30, 2 ** 34 - 1)
                print(rand_seed)
                cfg_file2 = generate_conf_file(rand_seed=rand_seed, task=task)

                simulation[sim_group] += 1
                print("sim %d starting in %s with %d nodes %s and %d" %
                      (
                          sim, current_thread().name, task["net_size"], task["algo"],
                          task["num_free"]))
                lock.release()

                # run_process(cfg_file2)
                create_log_files(rand_seed, task)

                lock.acquire()
                # remove(cfg_file2)
                avg = parse_log_files(task=task, seed=rand_seed)
                check_conf_interval(avg, down_times[sim_group])
                # TODO put the avg to a file
                print([rand_seed] + [time for time in avg.values])
                writer2.writerow([rand_seed] + [time for time in avg.values] + \
                                 [amplitude for amplitude in interval_spread.values()])
                csv_file2.flush()
                lock.release()

            sim_group += 1


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
                       freerider=0, task: dict = None):
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
        net_size = task["net_size"]
        algorithmm = task["algo"]
        freerider = task["num_free"]
    cfg_file2 = "conf/Time" + str(net_size) + "_" + algorithmm + "_" + str(freerider) + "f" + \
                str(rand_seed) + ".conf"
    copyfile(cfgFile, cfg_file2)
    with open(cfg_file2, mode="a") as file:
        file.write("# This part was generated by the script\n")
        file.write("random.seed " + str(rand_seed) + "\n")
        file.write("network.size " + str(net_size) + "\n")
        file.write("protocol.simulation.unchoking " + algorithmm + "\n")
        file.write("network.node.nFreeRider " + str(freerider) + "\n")

    return cfg_file2


def run_process(cfg_file="conf/Time-1.conf") -> None:
    """
    Creates a new process and waits for it to end. The stdout and stderr are redirect to a pipe.
    Since it waits for process completion, its a blocking function.
    :param cfg_file: Name of configuration file
    """
    p = Popen(["java", "-cp", libraries, "peersim.Simulator", cfg_file], stdout=PIPE, stderr=PIPE,
              universal_newlines=True)
    # p = Popen(["ls", cfg_file], stdout=PIPE, stderr=PIPE)
    p.wait()


def parse_log_files(task: dict, seed: int) -> dict:
    """
    Parse NodeTypes.csv and DownTimes.csv. Obtain type of nodes. Obtain download time of nodes
    that finished. Obtain list of nodes that didn't completed, and for each add a big value as
    download time. Group Download Time by NodeType and average it.
    :param task: net_size, algo, num freeriders
    :param seed: random seed used on simulation
    :return: Avg of download time grouped by type
    """
    folder_name = "log/%d/%s/%d/%d/" % (task["net_size"], task["algo"], task["num_free"], seed)

    """ Obtain type of nodes """
    file_name = folder_name + "NodeTypes.csv"
    df = pandas.read_csv(file_name, sep=';')

    """ Obtain download time of nodes that finished"""
    file_name2 = folder_name + "DownTimes.csv"
    df2 = pandas.read_csv(file_name2, sep=';')

    """ Obtain list of nodes that didn't completed, and for each add a big value """
    nodes_left = diff(df['NodeID'].values, df2['NodeID'].values)
    for node in nodes_left:
        node_type = df.query("NodeID == " + str(node))["NodeType"].values[0]
        df2 = df2.append({"NodeID": node,
                          "NodeType": node_type,
                          "DownloadTime": 10 ** 7}, ignore_index=True)

    """ Group Download Time by NodeType and average it """
    # TODO if no nodes with a specific type then dont divide
    avg = df2.groupby('NodeType').mean()['DownloadTime']
    return avg


def check_conf_interval(avg: pandas.Series, down_time: dict) -> dict:
    """

    :param avg:
    :param down_time:
    """  # TODO find a way to get node types dynamically
    for node_type in ['NORMAL', "FREE_RIDER"]:
        if node_type not in down_time:
            down_time[node_type] = []

        average = avg[node_type]
        down_time[node_type] += [average]

        if len(down_time[node_type]) > 30:
            interval_spread[node_type] = calc_interval(down_time[node_type])
            print(interval_spread[node_type])


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


def diff(a: list, b: list) -> list:
    """
    This is a temporary function and it will serve its purpose until a better implemented
    function is found. Given two lists of any type, it will iterate through the biggest
    and check if each element is on the other list. If its not, it's added to a third list. When
    it finishes, returns the third list contain all elements belonging to the biggest list but
    not to other.
    :param a: First list (recommend to be biggest)
    :param b: Second list (recommend to be smallest)
    :return: List with elements belonging to biggest but not the smallest list
    """
    if len(a) < len(b):
        temp = a
        a = b
        b = temp
    s = set(b)
    c = [x for x in a if x not in s]
    return c


if __name__ == '__main__':
    locker = Lock()

    nProcessors = cpu_count()
    # nProcessors = 2

    t = []
    downtime = {'FREE_RIDER': [], 'NORMAL': []}

    log_folder_name = "log"
    # if not exists(log_folder_name):
    #     mkdir(log_folder_name, 0o744)

    """ Creates new csv file or overwrites the old """
    # with open('csv/simulationTimes20160411.csv', 'w') as csv_file:
    #     writer = csv.writer(csv_file, delimiter=';')
    #     writer.writerow(['Random Seed', 'NormalTime', 'FreeRiderTime', 'NormalSpread',
    #                      'FreeRiderSpread'])

    tasks = []

    for nnodes in network_sizes:
        for algorithm in algorithms:
            for percentage in percentages_freeriders:
                """ convert freerider percentage to number of freeriders """
                num_freeriders = (percentage / 100) * nnodes

                tasks += [{"net_size": nnodes,
                           "algo": algorithm,
                           "num_free": num_freeriders}]
                simulation += [0]

                folder_name = "log/%d/%s/%d/" % (nnodes, algorithm, num_freeriders)
                if not exists(folder_name):
                    makedirs(folder_name)

                file_name = folder_name + "st.csv"
                with open(file_name, 'w', newline='') as csv_file2:
                    writer = csv.writer(csv_file2, delimiter=';')
                    writer.writerow(['Random Seed', "FreeRiderTime", 'NormalTime',
                                     "FreeRiderSpread", "NormalSpread"])

    for i in range(nProcessors):
        t.append(Thread(target=simulate, args=(locker, tasks)))
        t[i].start()

    """ Waits for threads to finish """
    for i in range(nProcessors):
        t[i].join()

    print("done")

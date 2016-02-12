from psutil import Popen, cpu_count
from threading import Thread, Lock

running = True

peersimLibraries = "../peersim-1.0.5/peersim-1.0.5.jar:../peersim-1.0.5/djep-1.0.0.jar:../peersim-1.0.5/jep-2.3.0.jar:../peersim-1.0.5/peersim-doclet.jar"
commonsmath35Library = "../commons-math3-3.5/commons-math3-3.5.jar"
bittrustOutDir = "out/production/BitTrust"

libraries = peersimLibraries + ":" + commonsmath35Library + ":" + bittrustOutDir

cfgFile = "conf/Time.conf"


simulation = 0
confidence = 0

#Function to be run at each thread
def simulate(lock=Lock()):
    global simulation
    while (confidence < 0.95):
        p = Popen(["java", "-cp", libraries, "peersim.Simulator", cfgFile])
        p.wait()
        lock.acquire(timeout=1)# TODO if a thread locks here and confidence already at objective see a workaround
        simulation += 1
        #see and change confidence here
        lock.release()
        

if __name__ == '__main__':
    nProcessors = cpu_count()

    t = []
    lock = Lock()

    #Create threads to control simulation
    for i in range(nProcessors):
        t.append(Thread(target=simulate, args=(lock,)))
        t[i].start()

    #Waits for threads to finish
    for i in range(nProcessors):
        t[i].join()


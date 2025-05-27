import json
import matplotlib.pyplot as plt
from matplotlib import rc
import numpy as np
from os.path import exists
import plot_setting

seeds = 10
f = 0
shape = "grid"
p = "outer"
r = 20
exp_lengths = {1: 150, 3: 250, 5: 300, 10: 400, 15: 500, 20: 600, 50: 600, 100: 600, 200: 1000, 500: 1500}

dev_nos = []
div = 1000

def getNumDevices(rtnNum):
    rtns_fn = open(f'workloads/routines/rtns_r{rtnNum}.txt', 'r')
    rtns = rtns_fn.readlines()
    rtns = [rtn[:-1] for rtn in rtns]
    d = {}
    for rtn in rtns:
        fn = open(f'workloads/routines/{rtn}.json', 'r')
        obj = json.load(fn)
        d[rtn] = len(obj['cmds'])
    return d

def setBoxColors(bp, color):
    for item in ['boxes', 'whiskers', 'fliers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)

def keep_min_max_fliers(bp):
    for i, fly in enumerate(bp['fliers']):
        fdata = fly.get_data()
        if len(fdata[0]) > 0:
            median = bp['medians'][i].get_data()[1][0]
            fliers = [[], []]
            # min value
            if fdata[1][0] < median:
                fliers[0].append(fdata[0][0])
                fliers[1].append(fdata[1][0])
            # max value
            if fdata[1][-1] > median:
                fliers[0].append(fdata[0][-1])
                fliers[1].append(fdata[1][-1])
            fly.set_data(fliers)

def getDelays(dir_prefix):
    global k, n, dims, ips, vs
    client_delay = [[] for _ in vs]
    sync_delay = [[] for _ in vs]

    rtn_cmds = getNumDevices(r)

    for ip in ips:
        for vi, v in enumerate(vs):
            dims_lbl = dims
            if n == 1:
                dims_lbl = '1,1'
            dev_no = np.prod([int(dim) for dim in dims_lbl.split(",")])
            if ',' in v:
                v1, v2 = [int(v_) for v_ in v.split(",")]
                dev_no = dev_no * v1 * v2 # + n
            else:
                dev_no = dev_no * (int(v) ** 2) # + n
                v1 = v2 = int(v)
            if n != 1 and dims[-1] == '1':
                dev_no -= 1
            if str(dev_no) not in dev_nos:
                dev_nos.append(str(dev_no))

            for e in range(seeds):
                for v1c in range(v1):
                    for v2c in range(v2):
                        client_delay_fn = f"{dir_prefix}/{ip}/{shape}{dims}_v{v}_n{n}_{p}_r{r}_f{f}_k{k}_e{e}/0{v1c}0{v2c}/client_delays.csv"
                        sync_delay_fn = f"{dir_prefix}/{ip}/{shape}{dims}_v{v}_n{n}_{p}_r{r}_f{f}_k{k}_e{e}/0{v1c}0{v2c}/sync_delays.csv"
                        if exists(client_delay_fn):
                            print(client_delay_fn)
                            with open(client_delay_fn, "r") as file:
                                for line in file.readlines():
                                    if line == "RoutineID,SeqNo,ClientDelayUsr,ClientDelaySys,ClientDelayAck\n":
                                        continue
                                    if float(line.split(",")[3]) > 0:
                                        client_delay[vi].append(float(line.split(",")[3])/rtn_cmds[line.split(",")[0]]/div)
                        if exists(sync_delay_fn):
                            print(sync_delay_fn)
                            with open(sync_delay_fn, "r") as file:
                                for line in file.readlines():
                                    if line == "RoutineID,SeqNo,SyncDelay\n":
                                        continue
                                    if int(line.split(",")[2]) > 0:
                                        sync_delay[vi].append(float(line.split(",")[2])/div)
    return client_delay, sync_delay

if __name__ == "__main__":
    lock_acq_delays = [0 for _ in range(4)]
    sync_delays = [0 for _ in range(4)]

    k = 3
    n = 5
    ips = [10, 11, 12, 13, 14]
    dims = "5,1"
    vs = ["1,1"]
    rv = getDelays('outputs/grid5,1_decentral_virtual')
    lock_acq_delays[0], sync_delays[0] = rv[0][0], rv[1][0]

    k = 3
    n = 11
    ips = [10, 11, 12, 13, 14, 16, 17, 19, 22, 23, 25]
    dims = "4,4"
    vs = ["1,1"]
    rv = getDelays('outputs/grid4,4_decentral_virtual')
    lock_acq_delays[1], sync_delays[1] = rv[0][0], rv[1][0]

    k = 3
    n = 26
    ips = list(range(10, 26))
    dims = "4,4"
    vs = ["2,2"]
    rv = getDelays('outputs/grid4,4_decentral_virtual')
    # lock_acq_delays[3], sync_delays[3] = rv[0][0], rv[1][0]
    lock_acq_delays[2], sync_delays[2] = rv[0][0], rv[1][0]

    k = 3
    n = 58
    ips = list(range(10, 26))
    dims = "4,4"
    vs = ["3,3"]
    rv = getDelays('outputs/grid4,4_decentral_virtual')
    lock_acq_delays[3], sync_delays[3] = rv[0][0], rv[1][0]

    lock_acq_medians = []
    sync_medians = []
    for i, d in enumerate(dev_nos):
        if len(lock_acq_delays[i]) > 0:
            client_delay_np = np.asarray(lock_acq_delays[i])
            lock_acq_medians.append(np.median(client_delay_np))
        if len(sync_delays[i]) > 0:
            sync_delay_np = np.asarray(sync_delays[i])
            sync_medians.append(np.median(sync_delay_np))
        
    print("min lock acq delay median:", min(lock_acq_medians))
    print("max lock acq delay median:", max(lock_acq_medians))
    print("min sync delay median:", min(sync_medians))
    print("max sync delay median:", max(sync_medians))

    labels = dev_nos
    x = np.arange(3*len(dev_nos), step=3) # the label locations
    width = 0.75 # the width of the bars

    fontsize = 14
    prop = {'markeredgecolor': 'grey'}
    colors = ["orange", "red", "blue", "green", "yellow", "purple", "cyan", "pink"]
    patterns = ['//', 'xx', '\\\\', '+', '...', 'oo', '\\\\||', '']

    rc('font', size=fontsize)

    fig, ax = plt.subplots(figsize=(6,4))
    ax.set_axisbelow(True)

    lock_acq_delay_bps = ax.boxplot(lock_acq_delays, positions = x - width / 2 , widths = width, flierprops=prop)
    sync_delay_bps = ax.boxplot(sync_delays, positions = x + width / 2, widths = width, flierprops=prop)

    setBoxColors(lock_acq_delay_bps, "orange")
    setBoxColors(sync_delay_bps, "blue")

    keep_min_max_fliers(lock_acq_delay_bps)
    keep_min_max_fliers(sync_delay_bps)

    # Add some text for labels, title and custom x-axis tick labels, etc.
    ax.set_ylabel('Delay (s)', fontsize=fontsize)
    ax.set_xlabel('#Devices', fontsize=fontsize)
    ax.set_yscale('log')
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.grid(axis='y')

    client_delay_handle, = plt.plot([1,1], '-', color='orange')
    sync_delay_handle, = plt.plot([1,1], '-', color='blue')
    ax.legend(
        (client_delay_handle, sync_delay_handle), ('Lock acquisition time', 'Sync delay'),
        loc="lower center", bbox_to_anchor=(0.5, 1.0), ncol=2
    )
    client_delay_handle.set_visible(False)
    sync_delay_handle.set_visible(False)

    plt.tight_layout()

    print("Saving client/sync delay plot at plots/client_sync_delay_xdev.png")
    plt.savefig('plots/client_sync_delay_xdev.png')
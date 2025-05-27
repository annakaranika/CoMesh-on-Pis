import matplotlib.pyplot as plt
import numpy as np
from os.path import exists
import json
import plot_setting

f = 0
k = 3
n = 11
seeds = 3
ips = range(10, 26)
rs = [1, 5, 10, 15, 20]
div = 1000

line_topo = 'MOB' # 'Mini Office Building'
grid_topo = 'MSF' # 'Mini Smart Farm'

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

def getDelays(dir_prefix, prefix):
    client_delay = [[] for _ in rs]
    sync_delay = [[] for _ in rs]

    for ip in ips:
        for i, r in enumerate(rs):
            rtn_cmds = getNumDevices(r)
            for e in range(seeds):
                client_delay_fn = f"{dir_prefix}/{ip}/{prefix}r{r}_n{n}_f{f}_k{k}_e{e}/client_delays.csv"
                sync_delay_fn = f"{dir_prefix}/{ip}/{prefix}r{r}_n{n}_f{f}_k{k}_e{e}/sync_delays.csv"
                if exists(client_delay_fn):
                    print(client_delay_fn)
                    with open(client_delay_fn, "r") as file:
                        for line in file.readlines():
                            if line == "RoutineID,SeqNo,ClientDelayUsr,ClientDelaySys,ClientDelayAck\n":
                                continue
                            if float(line.split(",")[3]) >= 0:
                                client_delay[i].append(float(line.split(",")[3])/rtn_cmds[line.split(",")[0]]/div)
                if exists(sync_delay_fn):
                    print(sync_delay_fn)
                    with open(sync_delay_fn, "r") as file:
                        for line in file.readlines():
                            if line == "RoutineID,SeqNo,SyncDelay\n":
                                continue
                            if int(line.split(",")[2]) != -1:
                                sync_delay[i].append(float(line.split(",")[2])/div)
    return client_delay, sync_delay

if __name__ == "__main__":
    client_delay_grid, sync_delay_grid = getDelays('outputs/grid4,4', 'grid4,4/')
    client_delay_line, sync_delay_line = getDelays('outputs/line', 'line_')

    labels = [str(r) for r in rs]
    x = np.arange(5*len(labels), step=5) # the label locations
    width = 0.75 # the width of the bars

    fontsize = 18
    fig, ax = plt.subplots(figsize=(8, 5))
    font = {'font.size': '15'}
    plt.rcParams.update(font)
    ax.tick_params(axis='both', which='major', labelsize=fontsize)
    ax.tick_params(axis='both', which='minor', labelsize=fontsize)


    for client_delay, sync_delay, topo in [(client_delay_grid, sync_delay_grid, "grid"), (client_delay_line, sync_delay_line, "line")]:
        client_medians = []
        sync_medians = []
        for i, r in enumerate(rs):
            if len(client_delay[i]) > 0:
                client_delay_np = np.asarray(client_delay[i])
                client_medians.append(np.median(client_delay_np))
            if len(sync_delay[i]) > 0:
                sync_delay_np = np.asarray(sync_delay[i])
                sync_medians.append(np.median(sync_delay_np))
        
        print(topo, "min client delay median:", min(client_medians))
        print(topo, "max client delay median:", max(client_medians))
        print(topo, "min sync delay median:", min(sync_medians))
        print(topo, "max sync delay median:", max(sync_medians))

    prop = {'markeredgecolor': 'grey'}
    client_delay_line_bps = ax.boxplot(client_delay_line, positions = x - width * 1.7, widths = width, flierprops=prop)
    client_delay_grid_bps = ax.boxplot(client_delay_grid, positions = x - width * 0.6, widths = width, flierprops=prop)
    sync_delay_line_bps = ax.boxplot(sync_delay_line, positions = x + width * 0.6, widths = width, flierprops=prop)
    sync_delay_grid_bps = ax.boxplot(sync_delay_grid, positions = x + width * 1.7, widths = width, flierprops=prop)

    setBoxColors(client_delay_line_bps, "red")
    setBoxColors(client_delay_grid_bps, "orange")
    setBoxColors(sync_delay_line_bps, "green")
    setBoxColors(sync_delay_grid_bps, "blue")

    keep_min_max_fliers(client_delay_line_bps)
    keep_min_max_fliers(client_delay_grid_bps)
    keep_min_max_fliers(sync_delay_line_bps)
    keep_min_max_fliers(sync_delay_grid_bps)

    # Add some text for labels, title and custom x-axis tick labels, etc.
    ax.set_ylabel('Delay (s)', fontsize=fontsize)
    ax.set_xlabel('#Routines', fontsize=fontsize)
    ax.set_yscale('log')
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.grid(axis='y')
    ax.set_yticks([1, 10, 100])
    ax.set_ylim(0.7, 250)

    client_delay_line_handle, = plt.plot([1,1], '-', color='red')
    client_delay_grid_handle, = plt.plot([1,1], '-', color='orange')
    sync_delay_line_handle, = plt.plot([1,1], '-', color='green')
    sync_delay_grid_handle, = plt.plot([1,1], '-', color='blue')
    ax.legend(
        (client_delay_line_handle, sync_delay_line_handle, client_delay_grid_handle, sync_delay_grid_handle),
        (f'{line_topo} lock acquisition time', f'{line_topo} sync delay', f'{grid_topo} lock acquisition time', f'{grid_topo} sync delay'),
        loc="lower center", bbox_to_anchor=(0.5, 1.0), ncol=2
    )
    client_delay_line_handle.set_visible(False)
    client_delay_grid_handle.set_visible(False)
    sync_delay_line_handle.set_visible(False)
    sync_delay_grid_handle.set_visible(False)

    plt.tight_layout()

    print("Saving client/sync delay plot at plots/client_sync_delay_xrtn.png")
    plt.savefig('plots/client_sync_delay_xrtn.png')
import json
import sys
import matplotlib.pyplot as plt
from matplotlib import rc
import numpy as np
from os.path import exists

seeds = 1
f = 0
shape = "grid"
ps = ["outer"]
rs = [1, 5, 10, 15, 20]
exp_lengths = {1: 150, 3: 250, 5: 300, 10: 400, 15: 500, 20: 600}

dev_nos = []
div = 1000

def setBoxColors(bp, color):
    for item in ['boxes', 'whiskers', 'fliers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)

def getNumDevices(path_chunk, rtnNum):
    rtns_fn = open(f'workloads/routines/{path_chunk}/rtns_r{rtnNum}.txt', 'r')
    rtns = rtns_fn.readlines()
    rtns = [rtn[:-1] for rtn in rtns]
    d = {}
    for rtn in rtns:
        fn = open(f'workloads/routines/{path_chunk}/{rtn}.json', 'r')
        obj = json.load(fn)
        d[rtn] = len(obj['cmds'])
    return d

def getDelays(dir_prefix):
    global k, n, dims, ips, vs

    sys_delays = [[] for _ in rs]
    sync_delays = [[] for _ in rs]

    for ip in ips:
        for p in ps:
            dev_no = np.prod([int(dim) for dim in dims.split(",")]) - n
            v1, v2 = [int(v) for v in vs.split(",")]
            dev_no = dev_no * v1 * v2 + n
            if str(dev_no) not in dev_nos:
                dev_nos.append(str(dev_no))
            for ri, r in enumerate(rs):
                for e in range(seeds):
                    path_chunk = f'{shape}{dims}_v{vs}_n{n}_{p}'
                    rtn_cmds = getNumDevices(path_chunk, 20)
                    delays_fn = f"{dir_prefix}/{ip}/{path_chunk}_r{r}_f{f}_k{k}_e{e}/0000/client_delays.csv"
                    if exists(delays_fn):
                        print(delays_fn)
                        with open(delays_fn, "r") as file:
                            for line in file.readlines():
                                if line == "RoutineID,SeqNo,ClientDelayUsr,ClientDelaySys,ClientDelayAck\n":
                                    continue
                                if int(line.split(",")[3]) > 0:
                                    sys_delays[ri].append(int(line.split(",")[3])/rtn_cmds[line.split(",")[0]]/div)
                    delays_fn = f"{dir_prefix}/{ip}/{path_chunk}_r{r}_f{f}_k{k}_e{e}/0000/sync_delays.csv"
                    if exists(delays_fn):
                        print(delays_fn)
                        with open(delays_fn, "r") as file:
                            for line in file.readlines():
                                if line == "RoutineID,SeqNo,SyncDelay\n":
                                    continue
                                if int(line.split(",")[2]) != -1:
                                    sync_delays[ri].append(float(line.split(",")[2])/div)
    return sys_delays, sync_delays

if __name__ == "__main__":
    type = sys.argv[1]
    ns = [11, 1]
    sys_delays = [[] for _ in ns]
    sync_delays = [[] for _ in ns]
    approaches = ["CoMesh", "Central"]

    k = 3
    n = 11
    ips = [10, 11, 12, 13, 14, 16, 17, 19, 22, 23, 25]
    dims = "4,4"
    vs = "1,1"
    sys_delays[0], sync_delays[0] = getDelays('outputs/grid4,4_new')
    
    k = 1
    n = 1
    ips = [10]
    dims = "2,1"
    vs = "15,1"
    sys_delays[1], sync_delays[1] = getDelays('outputs/central')

    labels = [str(r) for r in rs]
    x = np.arange(3*len(rs), step=3)
    width = 0.75 # the width of the bars

    fontsize = 18
    font = {'font.family': 'Times New Roman', 'font.weight': 'bold', 'font.size': '18'}
    plt.rcParams.update(font)
    prop = {'markeredgecolor': 'grey'}
    colors = ["orange", "red", "blue", "green", "yellow", "purple", "cyan", "pink"]
    patterns = ['//', 'xx', '\\\\', '+', '...', 'oo', '\\\\||', '']

    rc('hatch', linewidth=1)
    rc('patch', linewidth=1.8)
    rc('font', family='Times New Roman', weight='bold', size=fontsize)
    rc('axes', labelweight='bold')
    rc('figure', titleweight='bold')

    for delays, kind in [(sys_delays, "Lock acquisition"), (sync_delays, "Sync")]: #, (ack_delays, "Ack")]:
        fig, ax = plt.subplots(figsize=(4, 4))
        ax.set_axisbelow(True)
        ax.tick_params(axis='both', which='major', labelsize=fontsize)
        ax.tick_params(axis='both', which='minor', labelsize=fontsize)
        delays_bps = []
        for di, delay in enumerate(delays):
            delay_medians = []
            delay_means = []
            delay_stds = []
            for ri, r in enumerate(rs):
                if len(delay[ri]) > 0:
                    delay_np = np.asarray(delay[ri])
                    delay_medians.append(np.median(delay_np))
                    delay_means.append(np.mean(delay_np))
                    delay_stds.append(np.std(delay_np))
                else:
                    delay_means.append(0)
                    delay_stds.append(0)
            
            if type == "bp":
                delays_bps.append(ax.boxplot(
                    delay, positions = x + (width + 0.15) * di , widths = width, flierprops=prop
                ))
                setBoxColors(delays_bps[di], colors[di])
            elif type == "bar":
                ax.bar(
                    x + (width + 0.15) * di, delay_means, width, yerr=delay_stds, color='white',
                    hatch=patterns[di], edgecolor=colors[di],
                    error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1)
                )
            
            if len(delay_medians) > 0:
                print(kind, "min median:", min(delay_medians))
                print(kind, "max median:", max(delay_medians))

        # Add some text for labels, title and custom x-axis tick labels, etc.
        ax.set_ylabel(kind + ' delay (%)', fontsize=fontsize, loc="top")
        ax.set_xlabel('#Routines', fontsize=fontsize)
        if "log" in sys.argv:
            ax.set_yscale('log')
            log_txt = "_log"
        else:
            log_txt = ""
        ax.set_xticks(x)
        ax.set_xticklabels(labels)
        ax.grid(axis='y')

        handles = []
        for ni in range(len(ns)):
            handle, = plt.plot([1,1], '-', color=colors[ni])
            handles.append(handle)
        if len(ns) > 1:
            ax.legend(
                handles, approaches, loc="lower center",
                bbox_to_anchor=(0.4, 1.05), ncol=2
            )
        for handle in handles:
            handle.set_visible(False)

        plt.tight_layout()

        kind_ = kind.replace(" ", "_")
        print(f"Saving {kind} delay plot at plots/{kind_}_delay_cmp_{dims}_xrtn_{type}{log_txt}.png")
        plt.savefig(f'plots/{kind_}_delay_cmp_{dims}_xrtn_{type}{log_txt}.png')
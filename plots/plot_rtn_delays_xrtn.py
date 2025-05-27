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
labels = [str(r) for r in rs]
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
    global k, n, dims, ips, vs, rs

    sys_delays = [[[] for _ in rs] for _ in vs]
    sync_delays = [[[] for _ in rs] for _ in vs]

    for ip in ips:
        for p in ps:
            for vi, v in enumerate(vs):
                dev_no = np.prod([int(dim) for dim in dims.split(",")]) - n
                if ',' in v:
                    v1, v2 = [int(v_) for v_ in v.split(",")]
                    dev_no = dev_no * v1 * v2 + n
                else:
                    dev_no = dev_no * (int(v) ** 2) + n
                if str(dev_no) not in dev_nos:
                    dev_nos.append(str(dev_no))
                for ri, r in enumerate(rs):
                    for e in range(seeds):
                        path_chunk = f'{shape}{dims}_v{v}_n{n}_{p}'
                        rtn_cmds = getNumDevices(path_chunk, r)
                        delays_fn = f"{dir_prefix}/{ip}/{path_chunk}_r{r}_f{f}_k{k}_e{e}/0000/client_delays.csv"
                        if exists(delays_fn):
                            print(delays_fn)
                            with open(delays_fn, "r") as file:
                                for line in file.readlines():
                                    if line == "RoutineID,SeqNo,ClientDelayUsr,ClientDelaySys,ClientDelayAck\n":
                                        continue
                                    if int(line.split(",")[3]) > 0:
                                        sys_delays[vi][ri].append(int(line.split(",")[3])/rtn_cmds[line.split(",")[0]]/div)
                        delays_fn = f"{dir_prefix}/{ip}/{path_chunk}_r{r}_f{f}_k{k}_e{e}/0000/sync_delays.csv"
                        if exists(delays_fn):
                            print(delays_fn)
                            with open(delays_fn, "r") as file:
                                for line in file.readlines():
                                    if line == "RoutineID,SeqNo,SyncDelay\n":
                                        continue
                                    if int(line.split(",")[2]) != -1:
                                        sync_delays[vi][ri].append(float(line.split(",")[2])/div)
    return sys_delays, sync_delays

if __name__ == "__main__":
    dims = sys.argv[1]
    type = sys.argv[2]

    if dims == "4,4":
        figsize = (4, 3.5)
        k = 3
        n = 11
        ips = [10, 11, 12, 13, 14, 16, 17, 19, 22, 23, 25]
        vs = ["1,1"]
        sys_delays, sync_delays = getDelays('outputs/grid4,4_new')
        x = np.arange(len(labels), step=len(dev_nos)) # the label locations
    elif dims == "2,1":
        figsize = (4, 3.5)
        k = 1
        n = 1
        ips = [10]
        dims = "2,1"
        vs = ["15,1"]
        sys_delays, sync_delays = getDelays('outputs/central')
        x = np.arange(len(labels), step=len(dev_nos)) # the label locations
    elif dims == "5,1":
        figsize = (8, 5)
        k = 1
        n = 1
        ips = [10]
        vs = ['1', '2', '4', '8', '12']
        rs = [1, 3, 5, 10, 15, 20, 50, 100, 200, 500]
        labels = [str(r) for r in rs] 
        sys_delays, sync_delays = getDelays('outputs/central')
        x = np.arange(11*len(labels), step=11)

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
        fig, ax = plt.subplots(figsize=figsize)
        # ax.grid()
        ax.set_axisbelow(True)
        ax.tick_params(axis='both', which='major', labelsize=fontsize)
        ax.tick_params(axis='both', which='minor', labelsize=fontsize)
        delays_bps = []
        for vi, delay in enumerate(delays):
            if type == "bp":
                delays_bps.append(ax.boxplot(delay, positions = x + (width + 0.15) * vi , widths = width, flierprops=prop))
                setBoxColors(delays_bps[vi], colors[vi])
            elif type == "bar":
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
                
                ax.bar(
                    x + (width + 0.15) * vi, delay_means, width, yerr=delay_stds, color='white',
                    hatch=patterns[vi], edgecolor=colors[vi],
                    error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1)
                )
            
            if len(delay_medians) > 0:
                print(kind, "min median:", min(delay_medians))
                print(kind, "max median:", max(delay_medians))

        # Add some text for labels, title and custom x-axis tick labels, etc.
        ax.set_ylabel(kind + ' delay (s)', fontsize=fontsize)
        ax.set_xlabel('#Routines', fontsize=fontsize)
        if "log" in sys.argv:
            ax.set_yscale('log')
            log_txt = "_logy"
        else:
            log_txt = ""
        ax.set_xticks(x)
        ax.set_xticklabels(labels)
        ax.grid(axis='y')
        ax.set_ylim(bottom=0)

        handles = []
        for vi in range(len(vs)):
            handle, = plt.plot([1,1], '-', color=colors[vi])
            handles.append(handle)
        if len(dev_nos) > 1:
            ax.legend(
                handles, dev_nos, title="#Devices", loc="lower center",
                bbox_to_anchor=(0.45, 1.0), ncol=5
            )
        for handle in handles:
            handle.set_visible(False)

        plt.tight_layout()

        kind_ = kind.replace(" ", "_")
        print(f"Saving {kind} delay plot at plots/{kind_}_delay_{dims}_xrtn_{type}{log_txt}.png")
        plt.savefig(f'plots/{kind_}_delay_{dims}_xrtn_{type}{log_txt}.png')

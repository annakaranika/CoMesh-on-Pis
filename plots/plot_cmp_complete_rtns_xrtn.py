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

def getComplete(dir_prefix):
    global k, n, dims, ips, vs, rs

    complete = [0 for _ in rs]

    for ip in ips:
        for p in ps:
            dev_no = np.prod([int(dim) for dim in dims.split(",")]) - n
            if ',' in vs:
                v1, v2 = [int(v_) for v_ in vs.split(",")]
                dev_no = dev_no * v1 * v2 + n
            else:
                dev_no = dev_no * (int(vs) ** 2) + n
            if str(dev_no) not in dev_nos:
                dev_nos.append(str(dev_no))
            for ri, r in enumerate(rs):
                for e in range(seeds):
                    path_chunk = f'{shape}{dims}_v{vs}_n{n}_{p}'
                    delays_fn = f"{dir_prefix}/{ip}/{path_chunk}_r{r}_f{f}_k{k}_e{e}/0000/client_delays.csv"
                    if exists(delays_fn):
                        print(delays_fn)
                        with open(delays_fn, "r") as file:
                            for line in file.readlines():
                                if line == "RoutineID,SeqNo,ClientDelayUsr,ClientDelaySys,ClientDelayAck\n":
                                    continue
                                if int(line.split(",")[3]) > 0:
                                    complete[ri] += 1
    return complete

if __name__ == "__main__":
    ns = [11, 1]
    complete = [0 for _ in ns]
    approaches = ["CoMesh", "Central"]

    k = 3
    n = 11
    ips = [10, 11, 12, 13, 14, 16, 17, 19, 22, 23, 25]
    dims = "4,4"
    vs = "1,1"
    complete[0] = getComplete('outputs/grid4,4_new')
    
    k = 1
    n = 1
    ips = [10]
    dims = "2,1"
    vs = "15,1"
    complete[1] = getComplete('outputs/central')

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

    fig, ax = plt.subplots(figsize=(4, 4))
    # ax.grid()
    ax.set_axisbelow(True)
    ax.tick_params(axis='both', which='major', labelsize=fontsize)
    ax.tick_params(axis='both', which='minor', labelsize=fontsize)
    for di, comp in enumerate(complete):
        for ri, r in enumerate(rs):
            ax.bar(
                x + (width + 0.15) * di, comp, width, color='white',
                hatch=patterns[di], edgecolor=colors[di],
                error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1)
            )

    # Add some text for labels, title and custom x-axis tick labels, etc.
    ax.set_ylabel('#Complete routine(s)', fontsize=fontsize, loc="top")
    ax.set_xlabel('#Total routines', fontsize=fontsize)
    if "log" in sys.argv:
        ax.set_yscale('log')
        log_txt = "_log"
    else:
        log_txt = ""
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.grid(axis='y')

    if len(ns) > 1:
        handles = []
        for ni in range(len(ns)):
            handle, = plt.plot([1,1], '-', color=colors[ni])
            handles.append(handle)
        ax.legend(
            handles, approaches, loc="lower center",
            bbox_to_anchor=(0.4, 1.05), ncol=2
        )
        for handle in handles:
            handle.set_visible(False)

    plt.tight_layout()

    print(f"Saving complete routines' plot at plots/complete_rtns_cmp_{dims}_xrtn{log_txt}.png")
    plt.savefig(f'plots/complete_rtns_cmp_{dims}_xrtn{log_txt}.png')
import sys
import matplotlib.pyplot as plt
from matplotlib import rc
import numpy as np
from os.path import exists
import json

seeds = 1
f = 0
shape = "grid"
ps = ["outer"]
rs = [1, 5, 10, 15, 20]
exp_lengths = {1: 150, 3: 250, 5: 300, 10: 400, 15: 500, 20: 600}

dev_nos = []
div = 1000

def getComplete(dir_prefix):
    global k, n, dims, ips, vs, rs
    
    complete = [[0 for _ in vs] for _ in rs]

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
                        delays_fn = f"{dir_prefix}/{ip}/{path_chunk}_r{r}_f{f}_k{k}_e{e}/0000/client_delays.csv"
                        if exists(delays_fn):
                            print(delays_fn)
                            with open(delays_fn, "r") as file:
                                for line in file.readlines():
                                    if line == "RoutineID,SeqNo,ClientDelayUsr,ClientDelaySys,ClientDelayAck\n":
                                        continue
                                    if int(line.split(",")[3]) > 0:
                                        complete[ri][vi] += 1
    return complete

if __name__ == "__main__":
    dims = sys.argv[1]

    if dims == "4,4":
        figsize = (4, 3.5)
        k = 3
        n = 11
        ips = [10, 11, 12, 13, 14, 16, 17, 19, 22, 23, 25]
        vs = ["1,1"]
        complete = getComplete('outputs/grid4,4_new')
    elif dims == "2,1":
        figsize = (7, 5)
        k = 1
        n = 1
        ips = [10]
        vs = ["1", '2', '4', '8', '12']
        complete = getComplete('outputs/central')
    elif dims == "5,1":
        figsize = (8, 5)
        k = 1
        n = 1
        ips = [10]
        vs = ['1', '2', '4', '8', '12'] #, '16', '20', '26']
        rs = [1, 3, 5, 10, 15, 20, 50, 100, 200, 500]
        complete = getComplete('outputs/central')

    labels = dev_nos
    x = np.arange(11*len(labels), step=11) # the label locations
    width = 0.7 # the width of the bars

    fontsize = 18
    font = {'font.family': 'Times New Roman', 'font.weight': 'bold', 'font.size': '18'}
    plt.rcParams.update(font)
    prop = {'markeredgecolor': 'grey'}
    colors = ["orange", "red", "blue", "green", "yellow", "purple", "cyan", "pink", "black", "grey"]
    patterns = ['//', 'xx', '\\\\', '+', '...', 'oo', '\\\\||', '', '/', '\\']

    rc('hatch', linewidth=1)
    rc('patch', linewidth=1.8)
    rc('font', family='Times New Roman', weight='bold', size=fontsize)
    rc('axes', labelweight='bold')
    rc('figure', titleweight='bold')

    fig, ax = plt.subplots(figsize=figsize)
    # ax.grid()
    ax.set_axisbelow(True)
    ax.tick_params(axis='both', which='major', labelsize=fontsize)
    ax.tick_params(axis='both', which='minor', labelsize=fontsize)
    
    for ri, comp in enumerate(complete):
        for vi, v in enumerate(vs):
            ax.bar(
                x - 3 + (width + 0.15) * ri, comp, width, color='white',
                hatch=patterns[ri], edgecolor=colors[ri],
                error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1)
            )

    # Add some text for labels, title and custom x-axis tick labels, etc.
    ax.set_ylabel('#Complete routine(s)', fontsize=fontsize)
    ax.set_xlabel('#Devices', fontsize=fontsize)
    if "log" in sys.argv:
        ax.set_yscale('log')
        log_txt = "_logy"
    else:
        log_txt = ""
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.grid(axis='y')
    ax.set_ylim(bottom=0)

    if len(rs) > 1:
        handles = []
        for ri in range(len(rs)):
            handle, = plt.plot([1,1], '-', color=colors[ri])
            handles.append(handle)
        r_labels = [str(r) for r in rs]
        ax.legend(
            handles, r_labels, title="#Total routines", loc="lower center",
            bbox_to_anchor=(0.45, 1.0), ncol=5
        )
        for handle in handles:
            handle.set_visible(False)

    plt.tight_layout()

    print(f"Saving complete routines' plot at plots/complete_rtns_{dims}_xdev{log_txt}.png")
    plt.savefig(f'plots/complete_rtns_{dims}_xdev{log_txt}.png')
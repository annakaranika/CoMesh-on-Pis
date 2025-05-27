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

def getLoads(dir_prefix):
    global k, n, dims, ips, vs

    mem_loads = [[] for _ in rs]
    cpu_loads = [[] for _ in rs]

    for ip in ips:
        for p in ps:
            v1, v2 = [int(v) for v in vs.split(",")]
            if n == 3:
                dev_no = v1 * v2
            else:
                dev_no = np.prod([int(dim) for dim in dims.split(",")]) - n
                dev_no = dev_no * v1 * v2 + n
            if str(dev_no) not in dev_nos:
                dev_nos.append(str(dev_no))
            for ri, r in enumerate(rs):
                for e in range(seeds):
                    loads_fn = f"{dir_prefix}/{ip}/{shape}{dims}_v{vs}_n{n}_{p}_r{r}_f{f}_k{k}_e{e}/0000/load_histories.csv"
                    if exists(loads_fn):
                        print(loads_fn)
                        mem_load_col = 3
                        cpu_load_col = 4
                        with open(loads_fn, "r") as file:
                            for line in file.readlines():
                                if line == "nodeID,ts,mem_load,mem_load_pc,cpu_load\n":
                                    continue
                                if line == "nodeID,ts,mem_load,cpu_load\n":
                                    mem_load_col -= 1
                                    cpu_load_col -= 1
                                    continue
                                if int(line.split(",")[1]) > exp_lengths[r] * 1000:
                                    break
                                mem_loads[ri].append(float(line.split(",")[mem_load_col]))
                                cpu_load = float(line.split(",")[cpu_load_col])
                                if cpu_load_col == 3:
                                    cpu_load /= 4
                                cpu_loads[ri].append(cpu_load)
    return mem_loads, cpu_loads

if __name__ == "__main__":
    type = sys.argv[1]
    ns = [11, 1, 3]
    mem_loads = [[] for _ in ns]
    cpu_loads = [[] for _ in ns]
    approaches = ["CoMesh", "3-Hubs", "Central"]

    k = 3
    n = 11
    ips = [10, 11, 12, 13, 14, 16, 17, 19, 22, 23, 25]
    dims = "4,4"
    vs = "1,1"
    mem_loads[0], cpu_loads[0] = getLoads('outputs/grid4,4_new')
    
    k=3
    n=3
    ips = [15, 17, 20]
    dims = "4,1"
    vs = "4,4"
    mem_loads[1], cpu_loads[1] = getLoads('outputs/nhubs')

    k = 1
    n = 1
    ips = [10]
    dims = "2,1"
    vs = "15,1"
    mem_loads[2], cpu_loads[2] = getLoads('outputs/central')

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

    for loads, resource in [(mem_loads, "Memory"), (cpu_loads, "CPU")]:
        fig, ax = plt.subplots(figsize=(4, 3))
        ax.set_axisbelow(True)
        ax.tick_params(axis='both', which='major', labelsize=fontsize)
        ax.tick_params(axis='both', which='minor', labelsize=fontsize)
        loads_bps = []
        for li, load in enumerate(loads):
            load_medians = []
            load_means = []
            load_stds = []
            for ri, r in enumerate(rs):
                if len(load[ri]) > 0:
                    load_np = np.asarray(load[ri])
                    load_medians.append(np.median(load_np))
                    load_means.append(np.mean(load_np))
                    load_stds.append(np.std(load_np))
                else:
                    load_means.append(0)
                    load_stds.append(0)
            
            if type == "bp":
                loads_bps.append(ax.boxplot(
                    load, positions = x + (width + 0.15) * li , widths = width, flierprops=prop
                ))
                setBoxColors(loads_bps[li], colors[li])
            elif type == "bar":
                ax.bar(
                    x + (width + 0.2) * li, load_means, width, yerr=load_stds, label=approaches[li],
                    color='white', hatch=patterns[li], edgecolor=colors[li],
                    error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1)
                )
            
            if len(load_means) > 0:
                print(resource, "min means:", min(load_means))
                print(resource, "max means:", max(load_means))

        # Add some text for labels, title and custom x-axis tick labels, etc.
        ax.set_ylabel(resource + ' Load (%)', fontsize=fontsize, loc="top")
        ax.set_xlabel('#Routines', fontsize=fontsize)
        ax.set_xticks([xi+0.95 for xi in x])
        ax.set_xticklabels(labels)
        ax.grid(axis='y')
        ax.set_ylim(bottom=0)
        if resource == 'CPU':
            ax.set_yticks([10, 20, 30, 40, 50, 60])
            ax.set_ylim(top=65)
        else:
            ax.set_yticks([10, 20, 30, 40, 50])
            ax.set_ylim(top=50)

        ax.legend(ncol=2, fontsize=13, columnspacing=0.5, loc="upper left"

        plt.tight_layout()

        print(f"Saving {resource} plot at plots/{resource}_cmp_xrtn_{type}.pdf")
        plt.savefig(f'plots/{resource}_cmp_xrtn_{type}.pdf')
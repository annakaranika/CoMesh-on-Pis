import sys
import matplotlib.pyplot as plt
from matplotlib import rc
import numpy as np
from os.path import exists

f = 0
k = 3
n = 11
seeds = 1
ips = [10, 11, 12, 13, 14, 16, 17, 19, 22, 23, 25]
shape = "grid"
dims = "4,4"
vs = [1, 2, 4, 8, 12]
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
    mem_loads = [[[] for _ in vs] for _ in rs]
    cpu_loads = [[[] for _ in vs] for _ in rs]

    for ip in ips:
        for p in ps:
            for vi, v in enumerate(vs):
                dev_no = np.prod([int(dim) for dim in dims.split(",")]) - n
                dev_no = dev_no * (v ** 2) + n
                if dev_no not in dev_nos:
                    dev_nos.append(str(dev_no))
                for ri, r in enumerate(rs):
                    for e in range(seeds):
                        loads_fn = f"{dir_prefix}/{ip}/{shape}{dims}_v{v}_n{n}_{p}_r{r}_f{f}_k{k}_e{e}/0000/load_histories.csv"
                        if exists(loads_fn):
                            print(loads_fn)
                            with open(loads_fn, "r") as file:
                                for line in file.readlines():
                                    if line == "nodeID,ts,mem_load,mem_load_pc,cpu_load\n":
                                        continue
                                    if int(line.split(",")[1]) > exp_lengths[r] * 1000:
                                        break
                                    mem_loads[ri][vi].append(float(line.split(",")[3]))
                                    cpu_loads[ri][vi].append(float(line.split(",")[4]))
    return mem_loads, cpu_loads

if __name__ == "__main__":
    dims = sys.argv[1]
    type = sys.argv[2]

    if dims == "4,4":
        k = 3
        n = 11
        ips = [10, 11, 12, 13, 14, 16, 17, 19, 22, 23, 25]
        vs = ["1,1"]
        mem_loads, cpu_loads = getLoads('outputs/grid4,4_new')
    elif dims == "2,1":
        k = 1
        n = 1
        ips = [10]
        vs = [1, 2, 4, 8, 12]
        mem_loads, cpu_loads = getLoads('outputs/central')

    labels = dev_nos
    x = np.arange(len(dev_nos), step=len(rs)) # the label locations
    width = 0.4 # the width of the bars

    fontsize = 18
    font = {'font.family': 'Times New Roman', 'font.weight': 'bold', 'font.size': '18'}
    plt.rcParams.update(font)
    prop = {'markeredgecolor': 'grey'}
    colors = ["red", "orange", "blue", "green", "yellow", "purple", "cyan", "pink", "black", "grey"]
    patterns = ['xx', '//', '\\\\', '+', '...', 'oo', '\\\\||', '', '/', '\\']

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
        for ri, load in enumerate(loads):
            load_medians = []
            load_means = []
            load_stds = []
            for vi, v in enumerate(vs):
                if len(load[vi]) > 0:
                    load_np = np.asarray(load[vi])
                    load_medians.append(np.median(load_np))
                    load_means.append(np.mean(load_np))
                    load_stds.append(np.std(load_np))
                else:
                    load_means.append(0)
                    load_stds.append(0)
            
            if type == "bp":
                loads_bps.append(ax.boxplot(load, positions = x - 3 + (width + 0.15) * ri , widths = width, flierprops=prop))
                setBoxColors(loads_bps[ri], colors[ri])
            elif type == "bar":
                ax.bar(
                    x + (width + 0.15) * ri, load_means, width, yerr=load_stds, color='white',
                    hatch=patterns[ri], edgecolor=colors[ri],
                    error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1)
                )
            
            if len(load_medians) > 0:
                print(resource, "min median:", min(load_medians))
                print(resource, "max median:", max(load_medians))

        # Add some text for labels, title and custom x-axis tick labels, etc.
        ax.set_ylabel(resource + ' Load (%)', fontsize=fontsize)
        ax.set_xlabel('#Devices', fontsize=fontsize)
        ax.set_xticks(x)
        ax.set_xticklabels(labels)
        ax.grid(axis='y')
        if resource == 'CPU':
            ax.set_ylim(top=65)
        else:
            ax.set_ylim(top=50)

        if len(rs) > 0:
            handles = []
            for ri in range(len(rs)):
                handle, = plt.plot([1,1], '-', color=colors[ri])
                handles.append(handle)
            r_labels = ['Central']
            ax.legend(handles, r_labels, fontsize=12)
            for handle in handles:
                handle.set_visible(False)

        plt.tight_layout()

        print(f"Saving {resource} plot at plots/{resource}_{dims}_xdev.png")
        plt.savefig(f'plots/{resource}_{dims}_xdev.png')
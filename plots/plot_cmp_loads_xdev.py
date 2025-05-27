import sys
import matplotlib.pyplot as plt
from matplotlib import rc
import numpy as np
from os.path import exists

seeds = 1
f = 0
shape = "grid"
ps = ["outer"]
r = 20
exp_lengths = {1: 150, 3: 250, 5: 300, 10: 400, 15: 500, 20: 600}
cpu_cores = 4

dev_nos = []
div = 1000

def setBoxColors(bp, color):
    for item in ['boxes', 'whiskers', 'fliers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)

def exponential_fit(x, resource):
    # parameters found using Google Sheets trendline on scatter chart
    if resource == 'Memory':
        a = 17.4
        b = 0.0298
    else:
        a = 12.3
        b = 0.0263
    return a * np.exp(-b * x)

def getLoads(dir_prefix):
    global k, n, dims, ips, vs
    mem_loads = [[] for _ in vs]
    cpu_loads = [[] for _ in vs]

    for ip in ips:
        for p in ps:
            for vi, v in enumerate(vs):
                dims_lbl = dims
                if n in [1, 3]:
                    dims_lbl = '1,1'
                dev_no = np.prod([int(dim) for dim in dims_lbl.split(",")])
                if ',' in v:
                    v1, v2 = [int(v_) for v_ in v.split(",")]
                    dev_no = dev_no * v1 * v2 # + n
                else:
                    dev_no = dev_no * (int(v) ** 2) # + n
                if n not in [1, 3] and dims[-1] == '1':
                    dev_no -= 1
                if str(dev_no) not in dev_nos:
                    dev_nos.append(str(dev_no))

                if dir_prefix == 'outputs/grid4,4_decentral_virtual':
                    plain_mem_loads = []
                    plain_cpu_loads = []
                    plain_loads_fn = f"outputs/plain_loads/{ip}/plain_loads/plain_load_hist.csv"
                    if exists(plain_loads_fn):
                        print(plain_loads_fn)
                        with open(plain_loads_fn, "r") as file:
                            for line in file.readlines():
                                if line == "nodeID,ts,mem_load,cpu_load\n":
                                    continue
                                plain_mem_loads.append(float(line.split(",")[2]))
                                plain_cpu_loads.append(float(line.split(",")[3]) / cpu_cores)
                    median_plain_mem_load = np.median(plain_mem_loads)
                    median_plain_cpu_load = np.median(plain_cpu_loads)

                    for e in range(seeds):
                        for v1c in range(v1):
                            for v2c in range(v2):
                                loads_fn = f"{dir_prefix}/{ip}/{shape}{dims}_v{v}_n{n}_{p}_r{r}_f{f}_k{k}_e{e}/0{v1c}0{v2c}/load_histories.csv"
                                if exists(loads_fn):
                                    print(loads_fn)
                                    with open(loads_fn, "r") as file:
                                        for line in file.readlines():
                                            if line == "nodeID,ts,mem_load,cpu_load\n":
                                                continue
                                            if int(line.split(",")[1]) > exp_lengths[r] * 1000:
                                                break
                                            mem_loads[vi].append(median_plain_mem_load + float(line.split(",")[2]))
                                            cpu_loads[vi].append(median_plain_cpu_load + float(line.split(",")[3]) / cpu_cores)
                else:
                    for e in range(seeds):
                        loads_fn = f"{dir_prefix}/{ip}/{shape}{dims}_v{v}_n{n}_{p}_r{r}_f{f}_k{k}_e{e}/0000/load_histories.csv"
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
                                    mem_loads[vi].append(float(line.split(",")[mem_load_col]))
                                    cpu_load = float(line.split(",")[cpu_load_col])
                                    if cpu_load_col == 3:
                                        cpu_load /= cpu_cores
                                    cpu_loads[vi].append(cpu_load)
    return mem_loads, cpu_loads

if __name__ == "__main__":
    type = sys.argv[1]
    approaches = ["CoMesh", "3-Hubs", "Central"]
    mem_loads = [[0 for _ in range(5)] for _ in approaches]
    cpu_loads = [[0 for _ in range(5)] for _ in approaches]

    k = 1
    n = 2
    ips = [10, 11]
    dims = "2,1"
    vs = ["1,1"]
    rv = getLoads('outputs/grid2,1_decentral')
    print
    mem_loads[0][0], cpu_loads[0][0] = rv[0][0], rv[1][0]

    k = 3
    n = 5
    ips = [10, 11, 12, 13, 14]
    dims = "5,1"
    vs = ["1,1"]
    rv = getLoads('outputs/grid5,1_decentral')
    mem_loads[0][1], cpu_loads[0][1] = rv[0][0], rv[1][0]

    k = 3
    n = 11
    ips = [10, 11, 12, 13, 14, 16, 17, 19, 22, 23, 25]
    dims = "4,4"
    vs = ["1,1"]
    rv = getLoads('outputs/grid4,4_new')
    mem_loads[0][2], cpu_loads[0][2] = rv[0][0], rv[1][0]

    k = 3
    n = 26
    ips = list(range(10, 26))
    dims = "4,4"
    vs = ["2,2"]
    rv = getLoads('outputs/grid4,4_decentral_virtual')
    mem_loads[0][3], cpu_loads[0][3] = rv[0][0], rv[1][0]

    k = 3
    n = 58
    ips = list(range(10, 26))
    dims = "4,4"
    vs = ["3,3"]
    rv = getLoads('outputs/grid4,4_decentral_virtual')
    mem_loads[0][4], cpu_loads[0][4] = rv[0][0], rv[1][0]

    fit_y = {"Memory": [], "CPU": []}

    k = 3
    n = 3
    dims = "4,1"
    vs = ["1,1", "2,2", "4,4", "8,8", "12,12"]
    mem_loads[1], cpu_loads[1] = getLoads('outputs/nhubs')
    
    k = 1
    n = 1
    ips = [10]
    dims = "2,1"
    vs = ['1', '2', '4', '8', '12']
    mem_loads[2], cpu_loads[2] = getLoads('outputs/central')

    labels = dev_nos
    x = np.arange(3*len(dev_nos), step=3) # the label locations
    # x = np.arange(len(rs) * len(dev_nos), step=len(rs)) # the label locations
    width = 0.75 # the width of the bars
    # width = 0.7 # the width of the bars

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
    print(dev_nos)

    for loads, resource in [(mem_loads, "Memory"), (cpu_loads, "CPU")]:
        fig, ax = plt.subplots(figsize=(4, 3)) # (8,5))
        # ax.grid()
        ax.set_axisbelow(True)
        ax.tick_params(axis='both', which='major', labelsize=fontsize)
        ax.tick_params(axis='both', which='minor', labelsize=fontsize)
        loads_bps = []
        for li, load in enumerate(loads):
            load_medians = []
            load_means = []
            load_stds = []
            for vi, v in enumerate(dev_nos):
                if load[vi] != 0:
                    load_np = np.asarray(load[vi])
                    load_medians.append(np.median(load_np))
                    load_means.append(np.mean(load_np))
                    load_stds.append(np.std(load_np))
                else:
                    load_means.append(0)
                    load_stds.append(0)
                fit_y[resource].append(exponential_fit(int(v), resource))
            
            if type == "bp":
                loads_bps.append(ax.boxplot(
                    load, positions = x - width / 2 + (width / 2) * li , widths = width, flierprops=prop
                ))
                setBoxColors(loads_bps[li], colors[li])
            elif type == "bar":
                ax.bar(
                    x + (width + 0.15) * li, load_means, width, yerr=load_stds, label=approaches[li],
                    color='white', hatch=patterns[li], edgecolor=colors[li],
                    error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1)
                )
            
            if len(load_means) > 0:
                while 0 in load_means:
                    load_means.remove(0)
                print(resource, "min mean:", min(load_means))
                print(resource, "max mean:", max(load_means))

        # Add some text for labels, title and custom x-axis tick labels, etc.
        ax.set_ylabel(resource + ' Load (%)', fontsize=fontsize, loc="top")
        ax.set_xlabel('#Devices', fontsize=fontsize)
        ax.set_xticks([xi + 0.9 for xi in x])
        ax.set_xticklabels(labels)
        ax.grid(axis='y')
        ax.set_ylim(bottom=0)
        if resource == 'CPU':
            ax.set_yticks([0, 10, 20, 30, 40, 50, 60])
            ax.set_ylim(top=65)
        else:
            ax.set_yticks([0, 10, 20, 30, 40, 50])
            ax.set_ylim(top=50)

        ax.legend(ncol=1, fontsize=13)

        plt.tight_layout()

        print(f"Saving {resource} plot at plots/{resource}_cmp_xdev_{type}.pdf")
        plt.savefig(f'plots/{resource}_cmp_xdev_{type}.pdf')
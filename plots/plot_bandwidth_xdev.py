import sys
import matplotlib.pyplot as plt
from matplotlib import rc
import numpy as np
from os.path import exists
import plot_setting

metrics = ['e2e', 'e2e_bg', 'e2e_fg', 'h2h', 'h2h_bg', 'h2h_fg']
shape = 'grid'
p = 'outer'
f = 0
r = 20
exp_lens = {1: 150, 3: 250, 5: 300, 10: 400, 15: 500, 20: 600}
ips = range(10, 26)

arg2type = {'phy': 'phy', 'p': 'phy', 'virt': 'virt', 'v': 'virt'}

grid_topo = 'MSF' # 'Mini Smart Farm'
bg = 'Background' # 'BG'
fg = 'Foreground' # 'FG'
centr = 'Centralized' # 'Centr.'

def setBoxColors(bp, color):
    for item in ['boxes', 'whiskers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)

def getBandwidthOld(dir_prefix, prefix, n, k, seeds):
    bandwidth = {
        metric: {} for metric in metrics
    }

    node_list_filename = f'workloads/node_list/node_list_d16_n{n}.txt'
    with open(node_list_filename, 'r') as f:
        node_list = f.read().split()
        for node in node_list:
            for metric in metrics:
                bandwidth[metric][node] = 0

    for d in ips:
        for metric in metrics:
            bandwidth[metric][str(d)] = 0

    for seed in range(seeds):
        for node in ips:
            bandwidth_filename = f"{dir_prefix}/{node}/{prefix}r{r}_n{n}_f0_k{k}_e{seed}/bandwidth.csv"
            if not exists(bandwidth_filename):
                continue
            print(bandwidth_filename)
            with open(bandwidth_filename, "r") as f:
                for line in f.readlines():
                    if line == "Src,Dst,E2EMsg,E2E_BG,E2E_FG,H2HMsg,H2H_BG,H2H_FG\n":
                        continue
                    src = line.split(",")[0].split('.')[3]
                    for i, metric in enumerate(metrics):
                        bandwidth[metric][src] += int(line.split(",")[2 + i]) / seeds
    for metric in metrics:
        for node in bandwidth[metric]:
            bandwidth[metric][node] /= exp_lens[r]
        bandwidth[metric] = list(bandwidth[metric].values())

    return bandwidth

def getBandwidth(dir_prefix, seeds):
    global k, f, n, dims, ips, v
    bandwidth = {
        metric: {} for metric in metrics
    }

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
    dev_no = str(dev_no)
    if dev_no not in dev_nos:
        print('new dev_no:', dev_no)

    for metric in metrics:
        if type == 'phy':
            for ip in ips:
                bandwidth[metric][f'{ip}'] = 0
        elif type == 'virt':
            for v1c in range(v1):
                for v2c in range(v2):
                    for ip in ips:
                        bandwidth[metric][f'{ip}_0{v1c}0{v2c}'] = 0
    
    for e in range(seeds):
        for v1c in range(v1):
            for v2c in range(v2):
                for ip in ips:
                    bandwidth_filename = f"{dir_prefix}/{ip}/{shape}{dims}_v{v}_n{n}_{p}_r{r}_f{f}_k{k}_e{e}/0{v1c}0{v2c}/bandwidth.csv"
                    if not exists(bandwidth_filename):
                        print(bandwidth_filename, 'does not exist')
                        continue
                    with open(bandwidth_filename, "r") as file:
                        for line in file.readlines():
                            if line == "Src,Dst,E2EMsg,E2E_BG,E2E_FG,H2HMsg,H2H_BG,H2H_FG\n":
                                continue
                            if type == 'phy':
                                src = line.split(",")[0].split('.')[3].split('_')[0]
                            elif type == 'virt':
                                src = line.split(",")[0].split('.')[3]
                            for i, metric in enumerate(metrics):
                                bandwidth[metric][src] += int(line.split(",")[2 + i]) / exp_lens[r] / seeds
    for metric in metrics:
        bandwidth[metric] = list(bandwidth[metric].values())

    return bandwidth

if __name__ == "__main__":
    if len(sys.argv) > 1:
        if sys.argv[1] in arg2type:
            type = arg2type[sys.argv[1]]
        else:
            print('unrecognized type: setting to default (physical)')
            type = 'phy'
    dev_nos = ['4', '16', '64', '144']
    bandwidths = {
        metric: [
            0 for _ in dev_nos
        ] for metric in metrics
    }

    k = 3
    n = 5
    ips = list(range(10, 15))
    dims = "5,1"
    v = "1,1"
    rv = getBandwidth('outputs/grid5,1_decentral_virtual', 1)
    for metric in metrics:
        bandwidths[metric][0] = rv[metric]

    k = 3
    n = 11
    ips = list(range(10, 26))
    dims = "4,4"
    v = "1,1"
    rv = getBandwidth('outputs/grid4,4_new', 1)
    for metric in metrics:
        bandwidths[metric][1] = rv[metric]

    k = 3
    n = 26
    ips = list(range(10, 26))
    dims = "4,4"
    v = "2,2"
    rv = getBandwidth('outputs/grid4,4_decentral_virtual', 1)
    for metric in metrics:
        bandwidths[metric][2] = rv[metric]

    k = 3
    n = 58
    ips = list(range(10, 26))
    dims = "4,4"
    v = "3,3"
    rv = getBandwidth('outputs/grid4,4_decentral_virtual', 1)
    for metric in metrics:
        bandwidths[metric][3] = rv[metric]

    fgme = [np.mean(l) for l in bandwidths['e2e_fg']]
    bgme = [np.mean(l) for l in bandwidths['e2e_bg']]
    fgse = [np.std(l) for l in bandwidths['e2e_fg']]
    bgse = [np.std(l) for l in bandwidths['e2e_bg']]

    fgmh = [np.mean(l) for l in bandwidths['h2h_fg']]
    bgmh = [np.mean(l) for l in bandwidths['h2h_bg']]
    fgsh = [np.std(l) for l in bandwidths['h2h_fg']]
    bgsh = [np.std(l) for l in bandwidths['h2h_bg']]

    labels = dev_nos
    x = np.arange(len(labels)) # the label locations
    width = 0.75 # the width of the bars

    fig, ax = plt.subplots(figsize=(8, 5))

    ax.grid(axis='y')
    ax.set_axisbelow(True)
    colors = ['orange', 'red', 'blue', 'green']
    patterns1 = ['//', 'xx', '\\\\', '+']
    patterns2 = ['...', 'oo', '\\\\||', '']
    fontsize = 15
    rc('hatch', linewidth=1)
    rc('patch', linewidth=1.8)
    rc('font', size=fontsize)
    offset = 0.05

    ax.bar(x-0.19, bgme, 0.35, label=f'{grid_topo} E2E {bg}',
           color='white', hatch=patterns1[0], edgecolor=colors[0])
    ax.errorbar(x-0.19-offset, bgme, yerr=bgse, fmt='none', # end to end background
                ecolor='k', lw=1, capsize=2, capthick=1)
    ax.bar(x-0.19, fgme, 0.35, yerr=fgse, bottom=bgme, label=f'{grid_topo} E2E {fg}',
           color='white', hatch=patterns2[0],
           edgecolor=colors[0],
           error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1))
    ax.bar(x+0.19, bgmh, 0.35, label=f'{grid_topo} H2H {bg}',
           color='white', hatch=patterns1[2], edgecolor=colors[2])
    ax.errorbar(x+0.19-offset, bgmh, yerr=bgsh, fmt='none', # hop to hop background
                ecolor='k', lw=1, capsize=2, capthick=1)
    ax.bar(x+0.19, fgmh, 0.35, yerr=fgsh, bottom=bgmh, label=f'{grid_topo} H2H {fg}',
           color='white', hatch=patterns2[2],
           edgecolor=colors[2],
           error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1))

    ax.set_ylabel('Sent messages per second', fontsize=fontsize)
    ax.set_xlabel('#Devices', fontsize=fontsize)
    ax.tick_params(axis='both', which='major', labelsize=fontsize)
    ax.tick_params(axis='both', which='minor', labelsize=fontsize)
    ax.set_xticks(x)
    ax.set_xticklabels(dev_nos)
    ax.set_ylim(bottom=0)
    handles, labels = ax.get_legend_handles_labels()
    order = [1, 0, 3, 2]
    ax.legend(
              [handles[i] for i in order], [labels[i] for i in order],
              loc="lower center",
              bbox_to_anchor=(0.5, 1.0),
              ncol=2,
              fontsize=fontsize)

    fig.tight_layout()
    plt.savefig(f'plots/bandwidth_xdev_{type}.pdf')
    plt.savefig(f'plots/bandwidth_xdev.pdf')
    print(f"Bandwidth plot saved at plots/bandwidth_xdev_{type}.pdf and plots/bandwidth_xdev.pdf")

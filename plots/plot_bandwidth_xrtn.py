import matplotlib.pyplot as plt
from matplotlib import rc
import numpy as np
from os.path import exists
import plot_setting

metrics = ['e2e', 'e2e_bg', 'e2e_fg', 'h2h', 'h2h_bg', 'h2h_fg']
rs = [1, 5, 10, 15, 20]
exp_lens = {1: 150, 3: 250, 5: 300, 10: 400, 15: 500, 20: 600}
nodes = range(10, 26)

line_topo = 'MOB' # 'Mini Office Building'
grid_topo = 'MSF' # 'Mini Smart Farm'
bg = 'Background' # 'BG'
fg = 'Foreground' # 'FG'
centr = 'Centralized' # 'Centr.'

def setBoxColors(bp, color):
    for item in ['boxes', 'whiskers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)

def getBandwidth(dir_prefix, prefix, n, k, seeds):
    bandwidth = {
        metric: {
            rNo: {} for rNo in rs
        } for metric in metrics
    }

    for r in rs:
        node_list_filename = f'workloads/node_list/node_list_d16_n{n}.txt'
        with open(node_list_filename, 'r') as f:
            node_list = f.read().split()
            for node in node_list:
                for metric in metrics:
                    bandwidth[metric][r][node] = 0

        for d in nodes:
            for metric in metrics:
                bandwidth[metric][r][str(d)] = 0

        for seed in range(seeds):
            for node in nodes:
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
                            bandwidth[metric][r][src] += int(line.split(",")[2 + i]) / seeds
        for metric in metrics:
            for node in bandwidth[metric][r]:
                bandwidth[metric][r][node] /= exp_lens[r]
        for metric in metrics:
            bandwidth[metric][r] = list(bandwidth[metric][r].values())
    for metric in metrics:
        bandwidth[metric] = list(bandwidth[metric].values())

    return bandwidth

if __name__ == "__main__":
    bandwidth_grid = getBandwidth('outputs/grid4,4', 'grid4,4/', 11, 3, 3)
    bandwidth_line = getBandwidth('outputs/line', 'line_', 11, 3, 3)
    bandwidth_grid_c = getBandwidth('outputs/grid4,4', 'grid4,4/', 1, 1, 1)
    bandwidth_line_c = getBandwidth('outputs/line', 'line_', 1, 1, 1)

    fgme = np.mean(bandwidth_grid['e2e_fg'])
    bgme = np.mean(bandwidth_grid['e2e_bg'])
    flme = np.mean(bandwidth_line['e2e_fg'])
    blme = np.mean(bandwidth_line['e2e_bg'])
    fgse = np.std(bandwidth_grid['e2e_fg'])
    bgse = np.std(bandwidth_grid['e2e_bg'])
    flse = np.std(bandwidth_line['e2e_fg'])
    blse = np.std(bandwidth_line['e2e_bg'])

    fgmh = np.mean(bandwidth_grid['h2h_fg'])
    bgmh = np.mean(bandwidth_grid['h2h_bg'])
    flmh = np.mean(bandwidth_line['h2h_fg'])
    blmh = np.mean(bandwidth_line['h2h_bg'])
    fgsh = np.std(bandwidth_grid['h2h_fg'])
    bgsh = np.std(bandwidth_grid['h2h_bg'])
    flsh = np.std(bandwidth_line['h2h_fg'])
    blsh = np.std(bandwidth_line['h2h_bg'])

    gce = list(map(max, bandwidth_grid_c['e2e']))
    lce = list(map(max, bandwidth_line_c['e2e']))

    labels = [str(r) for r in rs]
    x = np.arange(len(labels)) # the label locations
    width = 0.75 # the width of the bars

    fig, ax = plt.subplots(figsize=(8, 6))

    ax.grid()
    ax.set_axisbelow(True)
    colors = ['orange', 'red', 'blue', 'green']
    patterns1 = ['//', 'xx', '\\\\', '+']
    patterns2 = ['...', 'oo', '\\\\||', '']
    fontsize = 16
    rc('hatch', linewidth=1)
    rc('patch', linewidth=1.8)
    rc('font', size=fontsize)

    ax.bar(x-0.3, blme, 0.18, yerr=blse, label=f'{line_topo} E2E {bg}',
           color='white', hatch=patterns1[1],
           edgecolor=colors[1],
           error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1))
    ax.bar(x-0.3, flme, 0.18, yerr=flse, bottom=blme, label=f'{line_topo} E2E {fg}',
           color='white', hatch=patterns2[1],
           edgecolor=colors[1],
           error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1))
    ax.bar(x-0.1, bgme, 0.18, yerr=bgse, label=f'{grid_topo} E2E {bg}',
           color='white', hatch=patterns1[0],
           edgecolor=colors[0],
           error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1))
    ax.bar(x-0.1, fgme, 0.18, yerr=fgse, bottom=bgme, label=f'{grid_topo} E2E {fg}',
           color='white', hatch=patterns2[0],
           edgecolor=colors[0],
           error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1))
    ax.bar(x+0.1, blmh, 0.18, yerr=blsh, label=f'{line_topo} H2H {bg}',
           color='white', hatch=patterns1[3],
           edgecolor=colors[3],
           error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1))
    ax.bar(x+0.1, flmh, 0.18, yerr=flsh, bottom=blmh, label=f'{line_topo} H2H {fg}',
           color='white', hatch=patterns2[3],
           edgecolor=colors[3],
           error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1))
    ax.bar(x+0.3, bgmh, 0.18, yerr=bgsh, label=f'{grid_topo} H2H {bg}',
           color='white', hatch=patterns1[2],
           edgecolor=colors[2],
           error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1))
    ax.bar(x+0.3, fgmh, 0.18, yerr=fgsh, bottom=bgmh, label=f'{grid_topo} H2H {fg}',
           color='white', hatch=patterns2[2],
           edgecolor=colors[2],
           error_kw=dict(ecolor='k', lw=1, capsize=2, capthick=1))
    ax.plot(x, lce, color='y', marker='X', label=f'{line_topo} E2E {centr}')
    ax.plot(x, gce, color='m', marker="p", label=f'{grid_topo} E2E {centr}')

    ax.set_ylabel('Sent messages per second', fontsize=fontsize)
    ax.set_xlabel('#Routines', fontsize=fontsize)
    ax.tick_params(axis='both', which='major', labelsize=fontsize)
    ax.tick_params(axis='both', which='minor', labelsize=fontsize)
    ax.set_xticks(x)
    ax.set_xticklabels(rs)
    ax.set_ylim(-1, 18)
    yticks = [0, 2, 4, 8, 16]
    ax.set_yticks(yticks)
    ax.set_yticklabels([str(xtick) for xtick in yticks])
    handles, labels = ax.get_legend_handles_labels()
    order = [0, 3, 2, 7, 6, 1, 5, 4, 9, 8]
    ax.legend([handles[i] for i in order], [labels[i] for i in order],
              loc="lower center",
              bbox_to_anchor=(0.5, 1.0),
              ncol=2,
              fontsize=fontsize)

    fig.tight_layout()
    plt.savefig('plots/bandwidth_xrtn.png')
    print("Bandwidth plot saved at plots/bandwidth_xrtn.png")

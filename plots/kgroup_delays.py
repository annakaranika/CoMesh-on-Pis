import matplotlib.pyplot as plt
import numpy as np
from os.path import exists
import plot_setting

dir = 'outputs'
topo = 'line'
prefix = 'line_'

Rs = [1, 5, 10, 15, 20]
Fs = ['0'] # , '1', '2', '3']
Ks = {
    # '1': '0',
    '3': '1',
    # '5': '2'
}
Ns = [
    # 1,
    11
]
Es = range(5)
IPs = range(10, 26)

metrics = ['quorum_delays', 'elctn_delays', 'state_delays']

metric2title = {'quorum_delays': 'Quorum+Reply',
                'elctn_delays' : 'Leader Election',
                'state_delays' : 'State Transfer'}

width = 0.75 # the width of the bars
metric2pos = {'quorum_delays': -width,
              'elctn_delays' :      0,
              'state_delays' :  width}

delays = {
    metric: {
        r: {
            n: {
                f: {
                    k: {
                        e: [] for e in Es
                    } for k in Ks
                } for f in Fs
            } for n in Ns
        } for r in Rs
    } for metric in metrics
}

def flatten(list):
    return [item for sublist in list for item in sublist]

def setBoxColors(bp, color):
    for item in ['boxes', 'whiskers', 'fliers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)

def draw_plots(variable, xlabel, delays, png_filename):
        # labels = variable
        x = np.arange(3, step=3) # the label locations
        # x = np.arange(3*len(labels), step=3) # the label locations

        # fig, ax = plt.subplots(figsize=(len(labels)*3, 5))
        fig, ax = plt.subplots(figsize=(8, 5))
        bps = {}
        for metric in metrics:
            bps[metric] = ax.boxplot(delays[metric],
                positions = x + metric2pos[metric], widths = width, meanline=True, showmeans=True)

        setBoxColors(bps['quorum_delays'], "green")
        setBoxColors(bps['elctn_delays'], "blue")
        setBoxColors(bps['state_delays'], "red")

        # Add some text for labels, title and custom x-axis tick labels, etc.
        ax.set_ylabel('Delay (ms)')
        ax.set_xlabel(xlabel)
        # ax.set_xticks(x)
        # ax.set_xticklabels(labels)

        handles = {}
        handles['quorum_delays'], = plt.plot([1,1],'g-')
        handles['elctn_delays'], = plt.plot([1,1],'b-')
        handles['state_delays'], = plt.plot([1,1],'r-')
        ax.legend([handles[metric] for metric in metrics],
                  [metric2title[metric] for metric in metrics],
                  title='Delays of communication within a k-group')
        for handle in handles.values():
            handle.set_visible(False)

        fig.tight_layout()
        # plt.show()
        print("Saving in-k-group delays plot at", png_filename)
        fig.savefig(png_filename)

for IP in IPs:
    for _metric in metrics:
        for r in Rs:
            for n in Ns:
                for f in Fs:
                    for k in Ks:
                        for e in Es:
                            fname = f'{dir}/{topo}/{IP}/{prefix}r{r}_n{n}_f{f}_k{k}_e{e}/{_metric}.csv'
                            if not exists(fname):
                                continue
                            print(fname)
                            with open(fname, 'r') as file:
                                for line in file.readlines():
                                    delay = line.split(',')[-1]
                                    if delay[0].isdigit():
                                        # print(delay)
                                        delays[_metric][r][n][f][k][e].append(int(delay))

for r in Rs:
    for n in Ns:
        for f in Fs:
            for k in Ks:
                for metric in metrics:
                    # print(delays[metric][r][n][f][k].values())
                    delays[metric][r][n][f][k] = list(delays[metric][r][n][f][k].values())
            for metric in metrics:
                delays[metric][r][n][f] = list(delays[metric][r][n][f].values())
        for metric in metrics:
            delays[metric][r][n] = list(delays[metric][r][n].values())
    for metric in metrics:
        delays[metric][r] = list(delays[metric][r].values())
for metric in metrics:
    delays[metric] = list(delays[metric].values())

for metric in metrics:
    delays[metric] = np.asarray(delays[metric]).flatten()
    delays[metric] = sum(delays[metric], [])
    delays_np = np.asarray(delays[metric])
    median = np.median(delays_np)
    mean = np.mean(delays_np)
    q90 = np.quantile(delays_np, 0.9)
    print(metric, median, mean, q90)
    
png_filename = 'plots/kgroup_delays.png'
draw_plots(None, None, delays, png_filename)

# labels = [k + ' (f=' + f +')' for k, f in Ks.items()]
# vname = 'K'
# data = {metric: [flatten(delays[metric]['0'][k].values()) for k in Ks] for metric in metrics}
# png_filename = dir + '/f0_varK.png'
# draw_plots(labels, vname, data, png_filename)

# labels = Fs
# vname = 'Failed Nodes'
# data = {metric: [flatten(delays[metric][f]['3'].values()) for f in Fs] for metric in metrics}
# png_filename = dir + '/varf_K3.png'
# draw_plots(labels, vname, data, png_filename)

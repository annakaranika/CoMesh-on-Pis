import matplotlib.pyplot as plt
import numpy as np
import sys
import plot_setting
from os.path import exists

def setBoxColors(bp, color):
    for item in ['boxes', 'whiskers', 'medians', 'caps']:
        plt.setp(bp[item], color=color)

if __name__ == "__main__":
    metrics = ['e2e', 'e2e_bg', 'e2e_fg', 'h2h', 'h2h_bg', 'h2h_fg']
    routine_nos = [1, 5, 10, 15, 20]
    seed_no = 5
    nodes = range(10, 26)
    n = 1
    k = 1

    labels = [str(seed) for seed in range(seed_no)]
    x = np.arange(len(labels)) # the label locations
    width = 0.25 # the width of the bars

    for r in routine_nos:
        bandwidth = {
            rNo: {
                metric: {
                    seed: {} for seed in range(seed_no)
                } for metric in metrics
            } for rNo in routine_nos
        }

        for seed in range(seed_no):
            node_list_filename = f'../workloads/node_list/node_list_d16_n{n}.txt'
            with open(node_list_filename, 'r') as f:
                node_list = f.read().split()
                for node in node_list:
                    for metric in metrics:
                        bandwidth[r][metric][seed][node] = 0
            for d in nodes:
                for metric in metrics:
                    bandwidth[r][metric][seed][str(d)] = 0

            for node in nodes:
                bandwidth_filename = f"../outputs/data/{node}/r{r}_n{n}_f0_k{k}_e{seed}/bandwidth.csv"
                if not exists(bandwidth_filename):
                    continue
                print(bandwidth_filename)
                with open(bandwidth_filename, "r") as f:
                    for line in f.readlines():
                        if line == "Src,Dst,E2EMsg,E2E_BG,E2E_FG,H2HMsg,H2H_BG,H2H_FG\n":
                            continue

                        src = line.split(",")[0].split('.')[3]
                        for i, metric in enumerate(metrics):
                            bandwidth[r][metric][seed][src] += int(line.split(",")[2 + i])

            for metric in metrics:
                bandwidth[r][metric][seed] = list(bandwidth[r][metric][seed].values())

        for metric in metrics:
            bandwidth[r][metric] = list(bandwidth[r][metric].values())

        fig, ax = plt.subplots(1, 2, figsize=(15, 6))
        bandwidth_bps = {}
        handles = {}
        for metric in metrics:
            if metric[-2:] == 'bg':
                positions = x
            elif metric[-2:] == 'fg':
                positions = x + width
            else:
                positions = x - width

            if metric[:3] == 'e2e':
                axes = ax[0]
            else:
                axes = ax[1]
            bandwidth_bps[metric] = axes.boxplot(bandwidth[r][metric], positions = positions, widths = width)

            if metric[-2:] == 'bg':
                setBoxColors(bandwidth_bps[metric], "green")
                handles[metric], = plt.plot([1,1],'g-')
            elif metric[-2:] == 'fg':
                setBoxColors(bandwidth_bps[metric], "red")
                handles[metric], = plt.plot([1,1],'r-')
            else:
                setBoxColors(bandwidth_bps[metric], "blue")
                handles[metric], = plt.plot([1,1],'b-')

        # Add some text for labels, title and custom x-axis tick labels, etc.
        # fig.suptitle('Communication Balancing\nRoutine # = ' + str(r))
        fig.supylabel('Sent messages')
        fig.supxlabel('Seed #')

        ax[0].set_xticks(x)
        ax[0].set_xticklabels(labels)
        ax[1].set_xticks(x)
        ax[1].set_xticklabels(labels)

        ax[0].legend((handles["e2e"], handles["e2e_bg"], handles["e2e_fg"]), ("All", "Background", "Foreground"), title="End to end")
        ax[1].legend((handles["h2h"], handles["h2h_bg"], handles["h2h_fg"]), ("All", "Background", "Foreground"), title="Hop to hop")

        for metric in metrics:
            handles[metric].set_visible(False)

        fig.tight_layout()

        fig_filename = f"bandwidth_d16_r{r}_n{n}_f0_k{k}.png"
        print("Saving bandwidth plot at", fig_filename)
        fig.savefig(fig_filename)
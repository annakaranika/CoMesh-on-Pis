# based on the node number and requested probability for node churn
# create the node churn schedule

import sys
from numpy.random import default_rng

if __name__ == "__main__":
    virtual = ""
    seed = 0
    distr = ""
    for i, arg in enumerate(sys.argv):
        if arg == "-d" or arg == "-D" or arg == "-dimensions":
            dims_str = sys.argv[i+1]
            dims = [int(d) for d in dims_str.split(",")]
            # print("Dimensions:", dims)
        elif arg == "-s" or arg == "-S" or arg == "-shape":
            shape = sys.argv[i+1]
        elif arg == "-v" or arg == "-V" or arg == "-virtual":
            virtual_dims = sys.argv[i+1]
        elif arg == "-p" or arg == "-P" or arg == "-position":
            pos = sys.argv[i+1] # "inner" or "outer"
        elif arg == "-cp" or arg == "-CP" or arg == "-churnProbability":
            churn_prob = float(sys.argv[i+1])
            # print("Probability for node churn:", churn_prob)
        elif arg == "-dc" or arg == "-DC" or arg == "-distribution":
            distr = sys.argv[i+1]
            # print("Distribution:", distr)
        elif arg == "-c" or arg == "-C" or arg == "-clusters":
            clusters = int(sys.argv[i+1])
            # print("Number of clusters:", sys.argv[i+1])
        elif arg == "-e" or arg == "-E" or arg == "-end":
            end = int(sys.argv[i+1])
            # timestamps are integers in [0, end]
            # print("Schedule length:", sys.argv[i+1])
        elif arg == "-seed":
            seed = int(sys.argv[i+1])
            # print('Seed:', sys.argv[i+1])
        elif arg == "-n" or arg == "-N" or arg == "-smart":
            n = int(sys.argv[i+1])

    path_chunk = f'{shape}{dims_str}_v{virtual_dims}_n{n}_{pos}'
    node_list_fn = f"workloads/node_list/node_list_{path_chunk}.txt"
    with open(node_list_fn ,"r") as f:
        lines = f.readlines()
        nodes = [line.split()[0] for line in lines]

    rng = default_rng(seed)
    churn_probs = rng.uniform(size=len(nodes))
    # churn_type = ["j" if temp > 0.5 else "f" for temp in rng.uniform(size=len(nodes))]
    churn_type = ["f" for _ in nodes]
    # TODO: rejoin them later?
    
    if distr == "uniform":
        time = [int(p) for p in rng.uniform(low=0, high=end, size=len(nodes))]
    elif distr == "poisson":
        temp = rng.poisson(lam=end/2, size=len(nodes))
        time = [int((p-min(temp))/(max(temp)-min(temp))*end) for p in sorted(temp)]
    elif distr == "exponential":
        temp = rng.exponential(scale=1.0, size=len(nodes))
        time = [int((p-min(temp))/(max(temp)-min(temp))*end) for p in sorted(temp)]
    elif distr == "zipf": # heavy-tail
        temp = rng.zipf(a=2, size=len(nodes))
        time = [int((p-min(temp))/(max(temp)-min(temp))*end) for p in sorted(temp)]
    elif distr == "pareto": # heavy-tail
        temp = rng.pareto(a=2, size=len(nodes))
        time = [int((p-min(temp))/(max(temp)-min(temp))*end) for p in sorted(temp)]
    elif distr == "power":
        temp = rng.power(a=2, size=len(nodes))
        time = [int((p-min(temp))/(max(temp)-min(temp))*end) for p in sorted(temp)]
    elif distr == "clusters":
        time = []
        loop_nodes = len(nodes) // clusters
        mod_nodes = len(nodes) % clusters
        for c in range(clusters):
            t = int(rng.uniform(low=0, high=end))
            i_loop_nodes = loop_nodes
            if c < mod_nodes:
                i_loop_nodes = i_loop_nodes + 1
            time.extend([t for _ in range(i_loop_nodes)])
    else:
        time = [0 for _ in range(len(nodes))]
    
    schedule = [
        (time[i], churn_type[i], nodes[i]) for i in range(len(nodes)) if churn_probs[i] < churn_prob
    ]

    schedule = sorted(schedule)
    # print(schedule)
    for ts, t, n in schedule:
        print(f"{ts}: {t} {n}")
    
    distr_str = f"{'_' + distr + (clusters if distr=='clusters' else '') if distr else ''}"
    node_sched_fn = f"workloads/node_sched/node_sched_{path_chunk}_cp{churn_prob}{distr_str}.txt"
    with open(node_sched_fn, "w+") as f:
        for ts, t, n in schedule:
            f.write(f"{ts} {t} {n}\n")
    
    print("Node schedule workload saved in", node_sched_fn)
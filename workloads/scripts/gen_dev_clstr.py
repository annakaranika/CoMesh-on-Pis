import math
import sys
from numpy import unique, where, mean, std
from scipy.cluster.vq import whiten
from sklearn.cluster import KMeans

if __name__ == "__main__":
    virtual = ""
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
        elif arg == "-n" or arg == "-N" or arg == "-smart":
            n = int(sys.argv[i+1])
        elif arg == "-t" or arg == "-T" or arg == "-threshold":
            threshold = int(sys.argv[i+1])

    fn_chunk = f'{shape}{dims_str}_v{virtual_dims}_n{n}_{pos}'

    # virtual_dims = [int(v) for v in virtual_dims.split(',')]
    # virtual_no = np.prod(virtual_dims)
    # device_no = dims[0] * dims[1] * virtual_no
    loc_fn = f"workloads/dev_loc/dev_loc_{fn_chunk}.txt"
    dev_ids = []
    with open(loc_fn, "r") as loc_f:
        lines = loc_f.readlines()
        dev_ids = [line.split()[0] for line in lines]
        locations = [[float(x) for x in line.split(" ")[1:3]] for line in lines]

        clstr_fn = f"workloads/dev_clstr/dev_clstr_{fn_chunk}.txt"
        with open(clstr_fn, "w+") as clstr_f:
            if n in [1, 3]:
                clstr_f.write(" ".join(dev_ids))
            else:
                data = whiten(locations)
                # print("size of system:", len(data))

                groups = [-1] * len(locations)
                curmax = -1
                # cluster2index = {}
                large_clusters = [[i for i in range(len(data))]]
                # cluster2index[-1] = [i for i in range(len(data))]
                while large_clusters:
                    # print("########################")
                    index_set = large_clusters.pop()
                    # print("index_set", index_set)
                    # index_set = cluster2index.get(l_cluster)
                    data = [locations[index] for index in index_set]
                    n_cluster = math.ceil(len(data)/threshold)
                    # print("size of this cluster:", len(data))
                    # print("num of clusters in this training:", n_cluster)
                    model = KMeans(n_clusters=n_cluster, random_state=0)
                    model.fit(data)
                    yhat = model.predict(data)
                    clusters = unique(yhat)
                    curmax += len(clusters)
                    for cluster in clusters:
                        cur_index_set = tuple(where(yhat==cluster)[0])
                        sub_index_set = [index_set[index] for index in cur_index_set]
                        # print("size of each sub_cluster after algo:", len(cur_index_set))
                        if len(cur_index_set) > threshold:
                            large_clusters.append(sub_index_set)
                        else:
                            for index in sub_index_set:
                                groups[index] = curmax + 1
                            curmax += 1


                clusters, counts = unique(groups, return_counts=True)
                # print("clusters:", clusters)
                # print("groups: " , groups)
                print("\n")
                print("--------------final result-------------------")
                print("threshold:", threshold)
                print("minimum number of clusters:", math.ceil(len(locations)/threshold))
                print("number of clusters:", len(counts))
                print("cluster size: ", counts)
                print("mean and std of cluster size:", mean(counts), std(counts))

                for cluster in clusters:
                    # print("cluster: ", cluster)
                    row_ix = where(groups == cluster)[0]
                    string = ""
                    for id in row_ix:
                        string += " " + dev_ids[id]
                    clstr_f.write(string.lstrip() + "\n")
        print("Device cluster workload saved in", clstr_fn)
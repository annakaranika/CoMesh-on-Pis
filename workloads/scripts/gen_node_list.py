import sys

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
            virtual_dim = int(sys.argv[i+1])
            virtual = f"_v{virtual_dim}"
        elif arg == "-p" or arg == "-P" or arg == "-position":
            pos = sys.argv[i+1] # "inner" or "outer"

    # device_no = dims[0] * dims[1] * (virtual_dim ** 2)
    path_chunk = f'{shape}{dims_str}_v{virtual_dim}_n1_{pos}'
    loc_fn = f"workloads/dev_loc/dev_loc_{path_chunk}.txt"
    dev_ids = []
    with open(loc_fn, "r") as loc_f:
        lines = loc_f.readlines()
        dev_ids = [line.split(" ")[0] for line in lines]

        node_fn = f"workloads/node_list/node_list_{path_chunk}.txt"
        with open(node_fn, "w+") as node_f:
            if pos == "outer":
                node_f.write(dev_ids[0])
            else:
                if shape == "grid":
                    x, y = [dim * virtual_dim // 2 for dim in dims]
                    node_f.write(dev_ids[(dims[1] * virtual_dim + 1) * x + y])
                else:
                    node_f.write(dev_ids[len(dev_ids) // 2])
            print("Node list workload saved in", node_fn)
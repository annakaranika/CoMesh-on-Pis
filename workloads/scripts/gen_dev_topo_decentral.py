import sys
import math
from numpy.random import default_rng
import numpy as np

class IndexDis:
    # will be used to sort the neighbors based on distance between them
    def __init__(self, index, dis):
        self.dis = dis
        self.index = index

def get_distance(v1, v2):
    # get distance between 2 vectors
    dis = 0.
    for i in range(len(v1)):
        dis += (v1[i] - v2[i]) ** 2
    dis = math.sqrt(dis)
    return dis

if __name__ == "__main__":
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
    
    fn_chunk = f'{shape}{dims_str}_v{virtual_dims}_n{n}_{pos}'
    virtual_dims = [int(v) for v in virtual_dims.split(',')]
    virtual_no = np.prod(virtual_dims)
    device_no = dims[0] * dims[1] * virtual_no

    ip_prefix = "192.168.4."
    ip_ends = list(range(10, 26))

    dev_ids = [
        f'{ip_prefix}{ip_end}_{vd1:02d}{vd2:02d}'
        for vd1 in range(virtual_dims[0])
        for vd2 in range(virtual_dims[1])
        for ip_end in ip_ends
    ]
    print(dev_ids)

    rng = default_rng(seed=0)
    node_fn = f"workloads/node_list/node_list_{fn_chunk}.txt"
    with open(node_fn, "w+") as node_f:
        smart = rng.choice(dev_ids, n, replace=False)
        node_f.write(' 100\n'.join(smart) + " 100\n")
    print("Node list workload saved in", node_fn)

    step1 = round(1/virtual_dims[0], 2)
    step2 = round(1/virtual_dims[1], 2)
    dev_fn = f"workloads/dev_loc/dev_loc_{fn_chunk}.txt"
    device_locations = {}
    with open(dev_fn, "w+") as f:
        for d1 in range(dims[0]):
            for d2 in range(dims[1]):
                ip_end = ip_ends[d1 + dims[0] * d2]
                ip = f"{ip_prefix}{ip_end}"
                for vd1 in range(virtual_dims[0]):
                    for vd2 in range(virtual_dims[1]):
                        dev_id = f"{ip}_{vd1:02d}{vd2:02d}"
                        dev_loc = (d1 + step1 * vd1, d2 + step2 * vd2)
                        dev_loc_str = ' '.join([str(dim) for dim in dev_loc])
                        device_locations[dev_id] = dev_loc
                        val_type = rng.choice(["numeric", "discrete"], p=[0.3, 0.7])
                        f.write(f"{dev_id} {dev_loc_str} {val_type}\n")
    print("Device locations workload saved in", dev_fn)

    # create the topology file: neighbors
    neighbors = {}
    threshold = 1.01
    for device in device_locations:
        distances = []
        for another_device in device_locations:
            if device == another_device:
                continue
            dis = get_distance(device_locations[device], device_locations[another_device])
            distances.append(IndexDis(another_device, dis))
        distances = sorted(distances, key=lambda a: a.dis)
        neighbor = []
        for node in distances:
            if node.dis <= threshold:
                neighbor.append(node.index)
        neighbors[device] = neighbor

    filename = f"workloads/dev_topo/dev_topo_{fn_chunk}.txt"
    with open(filename, 'w+') as f:
        for dev_id in dev_ids:
            f.write(f"{dev_id} {' '.join(neighbors[dev_id])}\n")
    print("Device topology workload saved in", filename)


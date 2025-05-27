import sys
import math
from numpy.random import default_rng

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
            virtual_dim = sys.argv[i+1]
        elif arg == "-p" or arg == "-P" or arg == "-position":
            pos = sys.argv[i+1] # "inner" or "outer"
    
    fn_chunk = f'{shape}{dims_str}_v{virtual_dim}_n1_{pos}'
    virtual_dim = [int(v) for v in virtual_dim.split(',')]

    ip_prefix = "192.168.4."
    ip_ends = [10] + list(range(30, 30 + math.prod(dims) - 1))

    node_fn = f"workloads/node_list/node_list_{fn_chunk}.txt"
    with open(node_fn, "w+") as node_f:
        central = f'{ip_prefix}{ip_ends[0]}_0000'
        node_f.write(central + "\n")
    print("Node list workload saved in", node_fn)

    # device_no = dims[0] * dims[1] * (virtual_dim ** 2)
    step1 = round(1/virtual_dim[0], 2)
    step2 = round(1/virtual_dim[1], 2)
    dev_fn = f"workloads/dev_loc/dev_loc_{fn_chunk}.txt"
    device_locations = {}
    rng = default_rng(seed=0)
    with open(dev_fn, "w+") as f:
        for d1 in range(dims[0]):
            for d2 in range(dims[1]):
                ip_end = ip_ends[d1 + dims[0] * d2]
                ip = f"{ip_prefix}{ip_end}"
                for vd1 in range(virtual_dim[0]):
                    for vd2 in range(virtual_dim[1]):
                        dev_id = f"{ip}_{vd1:02d}{vd2:02d}"
                        dev_loc = (d1 + step1 * vd1, d2 + step2 * vd2)
                        dev_loc_str = ' '.join([str(dim) for dim in dev_loc])
                        device_locations[dev_id] = dev_loc
                        val_type = rng.choice(["numeric", "discrete"], p=[0.3, 0.7])
                        f.write(f"{dev_id} {dev_loc_str} {val_type}\n")
                        if dev_id == central:
                            break
                    if dev_id == central:
                        break
    print("Device locations workload saved in", dev_fn)

    # create the topology file: neighbors
    simple = [dev_id for dev_id in device_locations.keys() if dev_id != central]
    filename = f"workloads/dev_topo/dev_topo_{fn_chunk}.txt"
    with open(filename, 'w+') as f:
        f.write(f"{central} {' '.join(simple)}\n")
        for dev_id in simple:
            f.write(f"{dev_id} {central}\n")
    print("Device topology workload saved in", filename)


import os
import sys
import numpy as np
import json
from scipy import stats

# params
schedule_fn = 'workloads/dev_sched/dev_sched_d16.txt'
routine_fn_prefix = 'workloads/routines/rtn'
num_rtns = 20

# consts
ops = ['==', '!=', '<', '<=', '>', '>=']

# useful functions
def sample_gaussian_mixture(low, high, c1, c2, sigma=1):
    # sample a value in the range(low, high) inclusive from
    # gaussian mixture distribution with mean at c1 * c2 and sigma
    weight1 = 1
    weight2 = 1
    d = np.concatenate((stats.norm.rvs(c1, sigma, 100 * weight1), stats.norm.rvs(c2, sigma, 100 * weight2)))
    val = round(np.random.choice(d, 1)[0])
    return min(max(val, low), high)

def get_op(node):
    if devs[node] == 'discrete':
        return np.random.choice(ops[:2], 1)[0]
    else:
        return np.random.choice(ops[2:], 1)[0]

def get_val(node):
    if devs[node] == 'discrete':
        return 'val' + str(np.random.randint(discrete_limits[node]+1))
    else:
        return str(round(np.random.uniform(), 2))

def gen_conds(nodes):
    num_node = len(nodes)
    ret = {}
    if num_node == 1:
        # with 10% probability add a not relationship
        if np.random.uniform() < 0.1:
            curr = gen_conds(nodes)
            ret['kind'] = 'not'
            ret['op1'] = curr
        else:
            ret['kind'] = get_op(nodes[0])
            ret['op1'] = nodes[0]
            ret['op2'] = get_val(nodes[0])
    else:
        split = np.random.randint(1, num_node)
        ret['op1'] = gen_conds(nodes[:split])
        ret['op2'] = gen_conds(nodes[split:])
        ret['kind'] = np.random.choice(['and', 'or'], 1)[0]
    return ret

def gen_cmds(nodes):
    ret = []
    for d in nodes:
        curr = {}
        curr['actDevID'] = d
        curr['newState'] = get_val(d)
        curr['len'] = str(np.random.randint(1, 10) * 1000)
        ret.append(curr)
    return ret

if __name__ == "__main__":
    end_time = 200000
    n = 1
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
        elif arg == "-r" or arg == "-R" or arg == "-routines":
            num_rtns = int(sys.argv[i+1])
        elif arg == "-n" or arg == "-N" or arg == "-smart":
            n = int(sys.argv[i+1])

    path_chunk = f'{shape}{dims_str}_v{virtual_dims}_n{n}_{pos}'
    schedule_fn = f'workloads/dev_sched/dev_sched_{path_chunk}.txt'
    routine_fn_prefix = f'workloads/routines/{path_chunk}/rtn'

    # read necessary data
    f = open(schedule_fn, 'r')
    lines = f.readlines()
    devs = {}
    discrete_limits = {}
    for l in lines:
        vals = l.split()
        if vals[1] not in devs:
            if vals[2][:3] == 'val':
                devs[vals[1]] = 'discrete'
                discrete_limits[vals[1]] = 0
            else:
                devs[vals[1]] = 'numeric'
        if devs[vals[1]] == 'discrete':
            discrete_limits[vals[1]] = max(discrete_limits[vals[1]], int(vals[2][3:]))
    all_nodes = [k for k in devs]
    # print(devs, discrete_limits)
    d = len(devs)

    # generate random routines
    for i in range(num_rtns):
        if d < 4:
            num_sensors = sample_gaussian_mixture(1, d//2, 2, d//2+1)
            num_actuators = sample_gaussian_mixture(1, d//2, 2, d//2+1)
        else:
            num_sensors = sample_gaussian_mixture(2, d//4, 2, d//2+1)
            num_actuators = sample_gaussian_mixture(1, d//4, 2, d//2+1)
        sensor_nodes = np.random.choice(all_nodes, num_sensors)
        np.random.shuffle(sensor_nodes)
        actuator_nodes = np.random.choice(all_nodes, num_actuators)

        conds = gen_conds(sensor_nodes)
        cmds = gen_cmds(actuator_nodes)

        curr = {}
        curr['rtnID'] = 'rtn' + str(i)
        curr['conds'] = conds
        curr['cmds'] = cmds
        json_object = json.dumps(curr, indent=4)

        if not os.path.isdir(routine_fn_prefix[:-4]):
            os.makedirs(routine_fn_prefix[:-4])
        with open(routine_fn_prefix + str(i) + '.json', 'w+') as fr:
            fr.write(json_object)
    
    fn = f'{routine_fn_prefix}s_r{num_rtns}.txt'
    with open(fn, "w+") as f:
        for r in range(num_rtns):
            f.write(f'rtn{r}\n')
    print("Routine workload saved in file", fn)

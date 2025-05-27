import sys
from random import randint, choice
from numpy.random import exponential as npexp
from numpy.random import normal
from start_check import check

if __name__ == "__main__":
    virtual = ""
    end_time = 200000
    num_rtns = 20
    n = 1
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
        elif arg == "-r" or arg == "-R" or arg == "-routines":
            num_rtns = int(sys.argv[i+1])
        elif arg == "-n" or arg == "-N" or arg == "-smart":
            n = int(sys.argv[i+1])

    # device_no = dims[0] * dims[1] * (virtual_dim ** 2)
    path_chunk = f'{shape}{dims_str}_v{virtual_dim}_n{n}_{pos}'
    fn = f"workloads/dev_loc/dev_loc_{path_chunk}.txt"
    with open(fn, "r") as f:
        lines = f.readlines()
        dev_mode = {line.split()[0]: line.split()[-1] for line in lines}

    discrete_num = {}
    prev_states = {}

    fn = f"workloads/dev_sched/dev_sched_{path_chunk}.txt"
    final_str = ''
    min_count = float('inf')
    min_cal_count = float('inf')
    trials = 10000
    # c = 0
    for trial in range(trials):
        curr_str = ''
        for dev_id in dev_mode:
            if dev_mode[dev_id] == "discrete":
                discrete_num[dev_id] = randint(0, 6)
                prev_states[dev_id] = "val0"
            else:
                prev_states[dev_id] = 0.0
            curr_str += f"5000.0 {dev_id} {prev_states[dev_id]}\n"
        t = 10000.0

        while t < end_time:
            t = t + randint(500, 1000)
            dev_id = choice(list(dev_mode.keys()))
            ps = prev_states[dev_id]

            if ps == 'val0' or ps == '0':
                sign = 1
            elif dev_id in discrete_num and ps == f'val{discrete_num[dev_id]-1}':
                sign = -1
            elif ps == '1':
                sign = -1
            else:
                sign = 1 if normal() > 0.5 else -1
            sign /= 2

            # change states so that with high probability the new value is
            # not too far away from the previous one
            if dev_mode[dev_id] == "numeric":
                s = str(min(1.0, max(0.0, round(float(prev_states[dev_id]) + sign * npexp(), 3))))
            else:
                pre = int(prev_states[dev_id][3])
                s = f"val{min(discrete_num[dev_id], max(0, pre + int(discrete_num[dev_id] * sign * npexp())))}"

            if s != prev_states[dev_id]:
                prev_states[dev_id] = s
                curr_str += f"{round(t, 2)} {dev_id} {s}\n"
        final_str = curr_str
        with open(fn, 'w') as f:
            f.write(curr_str)

        if num_rtns != 0:
            counts, counts2 = check(f'workloads/routines/{path_chunk}/rtns_r{num_rtns}.txt', fn, path_chunk)
            if len(counts) < 15:
                continue
            # c += 1
            if trial % 100 == 0:
                with open(fn, "w") as f:
                    f.write(final_str)
                print(trial)
            count = sum([counts[k] for k in counts])
            cal_count = sum([counts2[k] for k in counts2])
            if min_cal_count > cal_count:
                min_cal_count = cal_count
                min_count = count
                final_str = curr_str
            print(count, min_count, cal_count, min_cal_count)
        else:
            break

    with open(fn, "w") as f:
        f.write(final_str)
    print("Device schedule workload saved in", fn)

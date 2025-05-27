#!/bin/bash

s=grid
d="2,1"
vs=(1,1 2,2 4,4 8,8 12,12 16,16 20,20 26,26)
p=outer
rs=(1 3 5 10 15 20 50 100 200 500)

for v in ${vs[@]}
do
    python3 workloads/scripts/gen_dev_topo_central.py -s $s -d $d -v $v -p $p

    python3 workloads/scripts/gen_dev_clstr.py -s $s -d $d -v $v -p $p

    python3 workloads/scripts/gen_dev_sched.py -s $s -d $d -v $v -p $p -r 0

    python3 workloads/scripts/gen_node_sched.py -s $s -d $d -v $v -p $p -cp 0

    for r in ${rs[@]}
    do
        python3 workloads/scripts/gen_routines.py -s $s -d $d -v $v -p $p -r $r
    done
done

python3 workloads/scripts/gen_configs.py
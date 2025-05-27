#!/bin/bash

s=grid
d="4,4"
vs=("2,1" "2,2" "3,2" "4,2" "3,3" "5,2" "4,3" "5,3" "4,4" "5,5" "6,6")
ns=(1 1 1 1 1 1 1 1 1 1 1)
# ns=(58 64 77 96) # (13 26 38 51 58 64 77 96 102 160 230)
p=outer
rs=(20)

for i in ${!vs[@]}
do
    python3 workloads/scripts/gen_dev_topo_decentral.py -s $s -d $d -v ${vs[i]} -p $p -n ${ns[i]}

    # python3 workloads/scripts/gen_def_medley_output.py -s $s -d $d -v ${vs[i]} -p $p -n ${ns[i]}

    python3 workloads/scripts/gen_dev_clstr.py -s $s -d $d -v ${vs[i]} -p $p -n ${ns[i]} -t 20

    python3 workloads/scripts/gen_dev_sched.py -s $s -d $d -v ${vs[i]} -p $p -n ${ns[i]} -r 0

    python3 workloads/scripts/gen_node_sched.py -s $s -d $d -v ${vs[i]} -p $p -n ${ns[i]} -cp 0

    for r in ${rs[@]}
    do
        python3 workloads/scripts/gen_routines.py -s $s -d $d -v ${vs[i]} -p $p -n ${ns[i]} -r $r
    done

    python3 workloads/scripts/gen_dev_sched.py -s $s -d $d -v ${vs[i]} -p $p -n ${ns[i]} -r 20
done

python3 workloads/scripts/gen_configs.py
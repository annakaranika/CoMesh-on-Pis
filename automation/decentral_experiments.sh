s=grid
dims="5,1"
vs=("1,1")
ns=(5)
p=outer
rs=(1 5 10 15 20)
f=0
k=3
es=(1 2 3 4 5 6 7 8 9)
start=10
end=25

function clean() {
    sh ~/CoMesh-on-Pis/automation/kill_all.sh $start $end
    for pid in ${pids[@]}; do
        kill $pid
    done
}

trap clean EXIT

./automation/copy_all.sh $start $end $1

for i in ${!vs[@]}
do
    local_dir=$s$dims\_decentral\_virtual
    v=${vs[i]}
    n=${ns[i]}
    prefix=$s$dims\_v$v\_n$n\_$p

    cd ../medley
    echo "\n\n\n======= Starting up medley ========="
    ./automation/run.sh $end
    cd ../CoMesh-on-Pis

    for e in ${es[@]}
    do
        for r in ${rs[@]}
        do
            pi_dir=$prefix\_r$r\_f$f\_k$k\_e$e
            config_fn="configs/config_$pi_dir.json"
            echo $config_fn
            clean

            ./automation/copy_files.sh $start $end $config_fn configs

            echo "======= Starting up CoMesh ========="
            ./automation/run_decentral.sh $start $end $config_fn $v
            sleep $(($(jq '.expLength' $config_fn) / 1000 + 20)) # seconds

            pids=()
            for (( i=$start; i<=$end; i++ )); do
                ssh pi@192.168.4.$i "./CoMesh-on-Pis/automation/move_on.sh ~/CoMesh-on-Pis/outputs/$pi_dir $v $i" &
                pids+=($!)
            done
            for pid in ${pids[@]}; do
                wait $pid
            done
            
            ./automation/stop.sh $start $end $pi_dir $local_dir
            sleep 2
        done
    done
done
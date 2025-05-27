start=10
end=25

function clean() {
  sh ~/CoMesh-on-Pis/automation/kill_all.sh $end
}

trap clean EXIT

./automation/copy_all.sh $end $1

cd ../medley
echo "\n\n\n======= Starting up medley ========="
./automation/run.sh $end
cd ../CoMesh-on-Pis

rs=(1 5 10 15 20)
f=0
k=3
n=11
es=(0 1 2 3 4)
ds=(4 8 16)
topo="grid4,4"

for e in "${es[@]}"
do
    for r in "${rs[@]}"
    do
        config="configs/config_d16_n${n}_r${r}_f${f}_k${k}_e${e}.json"
        ./automation/kill_all.sh $end

        echo "======= Starting up CoMesh ========="
        ./automation/run.sh $config $r $f $k $e $end
        sleep $(($(jq '.expLength' $config) / 1000 + 20)) # seconds

        pids=()
        dir="r${r}_n${n}_f${f}_k${k}_e${e}/"
        # nodes=(10)
        nodes=(10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25)
        for i in "${nodes[@]}"; do
            ssh pi@192.168.4.$i "./CoMesh-on-Pis/automation/move_on.sh ~/CoMesh-on-Pis/outputs/$dir" &
            pids+=($!)
        done

        for pid in ${pids[@]}; do
            wait $pid
        done
        ./automation/stop.sh $end $dir $topo

        sleep 2
    done
done
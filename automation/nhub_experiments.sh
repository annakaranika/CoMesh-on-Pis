s=grid
d="4,1"
n=3
vs=(1,1 2,2 4,4 8,8 12,12)
p=outer
rs=(1 5 10 15 20)
f=0
k=3
es=(0)
hubs=(15 17 20)
local_start=100
local_end=100

function clean() {
    echo Killing at the pis
    for hub in ${hubs[@]}
    do
        sh ~/CoMesh-on-Pis/automation/kill_all.sh $hub $hub
    done
    kill $pid
    echo Killing at laptop $local_start
    killall -9 java
    if [[ $(($local_start + 1)) -le $local_end ]]
    then
        echo Killing at laptops $(($local_start + 1)) to $local_end
        sh ~/CoMesh-on-Pis/automation/kill_all.sh $(($local_start + 1)) $local_end
    fi
}

trap clean EXIT

for hub in ${hubs[@]}
do
    ./automation/copy_all.sh $hub $hub $1
done
if [[ $(($local_start + 1)) -le $local_end ]]
then
    ./automation/copy_all.sh $(($local_start + 1)) $local_end $1
fi
local_dir=nhubs

for e in ${es[@]}
do
    for r in ${rs[@]}
    do
        for v in ${vs[@]}
        do
            pi_dir=$s$d\_v$v\_n$n\_$p\_r$r\_f$f\_k$k\_e$e
            config_fn="configs/config_$pi_dir.json"
            clean

            cd ../medley
            echo "\n\n\n======= Starting up medley ========="
            ./automation/run.sh $end
            cd ../CoMesh-on-Pis

            echo "======= Starting up CoMesh ========="
            ./automation/run_nhubs.sh $local_start $local_end $config_fn $v &
            sleep_secs=$(($(jq '.expLength' $config_fn) / 1000 + 20))
            echo "Sleep for $sleep_secs seconds"
            sleep $sleep_secs

            for hub in ${hubs[@]}
            do
                ssh pi@192.168.4.$hub "./CoMesh-on-Pis/automation/move_on.sh ~/CoMesh-on-Pis/outputs/$pi_dir 1,1 $hub"
            done
            
            pid=$!
            echo $pid
            wait $pid
            
            for hub in ${hubs[@]}
            do
                # Copy the output from each hub to the local machine
                ./automation/stop.sh $hub $hub $pi_dir $local_dir
            done
            sleep 2
        done
    done
done

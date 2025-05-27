s=grid
d="2,1"
n=1
vs=(15,1) # (1,1 2,2 4,4 8,8 12,12 16,16 20,20 26,26)
p=outer
rs=(1 5 10 15 20)
f=0
k=1
es=(0)
central=10
local_start=30
local_end=30

function clean() {
    echo Killing at the pi
    sh ~/CoMesh-on-Pis/automation/kill_all.sh $central $central
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

./automation/copy_all.sh $central $central $1
if [[ $(($local_start + 1)) -le $local_end ]]
then
    ./automation/copy_all.sh $(($local_start + 1)) $local_end $1
fi
local_dir=central

for e in ${es[@]}
do
    for r in ${rs[@]}
    do
        for v in ${vs[@]}
        do
            pi_dir=$s$d\_v$v\_n$n\_$p\_r$r\_f$f\_k$k\_e$e
            config_fn="configs/config_$pi_dir.json"
            clean

            python3 workloads/scripts/gen_def_medley_output.py -s $s -d $d -v $v -p $p
            cp workloads/medley_output/outputMedley.txt ../medley/outputMedley.txt
            ./automation/copy_all.sh $central $central m

            cd ../medley
            # mvn clean package
            echo "\n\n\n======= Starting up medley ========="
            ./automation/run.sh $end

            cd ../CoMesh-on-Pis
            echo "======= Starting up CoMesh ========="
            ./automation/run_central.sh $central $local_start $local_end $config_fn $v
            sleep $(($(jq '.expLength' $config_fn) / 1000 + 20)) # seconds

            ssh pi@192.168.4.$central "./CoMesh-on-Pis/automation/move_on.sh ~/CoMesh-on-Pis/outputs/$pi_dir 1 $central" &
            pid=$!
            echo $pid
            wait $pid
            
            ./automation/stop.sh $central $central $pi_dir $local_dir
            sleep 2
        done
    done
done

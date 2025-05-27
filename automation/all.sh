s=grid
dims=2,2
v=1
n=1
p=inner
r=10
f=0
k=1
e=0
start=10
end=13

local_dir=central/
pi_dir=$s$dims\_v$v\_n$n\_$p\_r$r\_f$f\_k$k\_e$e
config_fn=configs/config_$pi_dir.json

function clean() {
  sh ~/CoMesh-on-Pis/automation/kill_all.sh $start $end
}

trap clean EXIT
./automation/kill_all.sh $start $end

cd ../medley
mvn clean package
./automation/copy_jar.sh
echo "\n\n\n======= Starting up medley ========="
./automation/run.sh $end

cd ../CoMesh-on-Pis
python3 workloads/scripts/gen_def_medley_output.py -s $s -d $dims -v $v -p $p
./automation/copy_all.sh $start $end "m$1"
sleep 1
echo "\n\n\n======= Starting up CoMesh ========="
./automation/run.sh $start $end $config_fn $v
sleep 500 # seconds

pids=()
for (( i=$start; i<=$end; i++ )); do
  ssh pi@192.168.4.$i "./CoMesh-on-Pis/automation/move_on.sh ~/CoMesh-on-Pis/outputs/$pi_dir" &
  pids+=($!)
done
for pid in ${pids[@]}; do
    wait $pid
done

./automation/stop.sh $start $end $pi_dir $local_dir

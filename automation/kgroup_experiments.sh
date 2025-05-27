Ks=( 1 3 5 )
fs=( 0 1 2 3 )

./automation/copy_all.sh $1

for e in $(seq 0 9); do
    for f in "${fs[@]}"; do
        for k in "${Ks[@]}"; do
            config_fn=configs/config_f"$f"_k"$k"_e"$e".json
            echo $config_fn $i
            ./automation/kill_all.sh
            sleep 30

            cd ../medley
            echo "\n\n\n======= Starting up medley ========="
            ./automation/run.sh
            sleep 30

            cd ../CoMesh-on-Pis
            echo "\n\n\n======= Starting up CoMesh ========="
            ./automation/run.sh $config_fn $f $k $e

            sleep 400 # seconds
        done
    done
done

./automation/stop.sh
python3 plots/kgroup_delays.py
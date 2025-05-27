start=$1
end=$2
config_fn=$3
vs=$4

# sync time
for (( i=$start; i<=$end; i++ )) do
   ssh pi@192.168.4.$i sudo date -s \"$(gdate +"%Y-%m-%d %H:%M:%S,%3N")\" &
done
sleep 2
start_time=$((12000+$(date +%s)*1000)) # millisecond for the next 12 seconds

# run script
for (( i=$(($start)); i<=$end; i++)) do
    # bash: linux default is dash, which will not run the split instruction in run_local_virtual.sh
    ssh pi@192.168.4.$i "cd CoMesh-on-Pis/ && bash automation/run_local_virtual.sh $config_fn $vs $start_time" &
done

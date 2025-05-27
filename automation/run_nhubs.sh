local_start=$1
local_end=$2
config_fn=$3
vs=$4

hubs=(15 17 20)

# sync time
for hub in ${hubs[@]}
do
   ssh pi@192.168.4.$hub sudo date -s \"$(gdate +"%Y-%m-%d %H:%M:%S,%3N")\" &
done
sleep 2
start_time=$((12000+$(date +%s)*1000)) # millisecond for the next 12 seconds

# run script
# hub nodes
for hub in ${hubs[@]}
do
    echo "Running on 192.168.4.$hub"
    ssh pi@192.168.4.$hub "cd CoMesh-on-Pis/ && java -Xmx1324m -cp target/CoMesh-on-Pis-1.0-SNAPSHOT-shaded.jar deploy.Deployer $config_fn 0000 $start_time" &
done

# laptop nodes
echo "Running on local"
sh automation/run_local_virtual.sh $config_fn $v $start_time &

for (( i=$(($local_start + 1)); i<=$local_end; i++)) do
    echo "Running on 192.168.4.$i"
    ssh pi@192.168.4.$i "cd CoMesh-on-Pis/ && sh automation/run_local_virtual.sh $config_fn $v $start_time" &
done

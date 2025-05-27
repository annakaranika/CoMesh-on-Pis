central=$1
local_start=$2
local_end=$3
config_fn=$4
v=$5

# sync time
ssh pi@192.168.4.$central sudo date -s \"$(gdate +"%Y-%m-%d %H:%M:%S,%3N")\" &
sleep 2
start_time=$((12000+$(date +%s)*1000)) # millisecond for the next 12 seconds

# run script
# central pi node
ssh pi@192.168.4.$central "cd CoMesh-on-Pis/ && java -Xmx1324m -cp target/CoMesh-on-Pis-1.0-SNAPSHOT-shaded.jar deploy.Deployer $config_fn 0000 $start_time" &

# laptop nodes
sh automation/run_local_virtual.sh $config_fn $v $start_time

for (( i=$(($local_start + 1)); i<=$local_end; i++)) do
    ssh pi@192.168.4.$i "cd CoMesh-on-Pis/ && sh automation/run_local_virtual.sh $config_fn $v $start_time" &
done

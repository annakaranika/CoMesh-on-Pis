start=$1
end=$2

config_fn=$3
v=$4

# sync time
for (( i=$start; i<=$end; i++ )) do
   ssh pi@192.168.4.$i sudo date -s \"$(gdate +"%Y-%m-%d %H:%M:%S,%3N")\" &
done
sleep 2
start_time=$((12000+$(date +%s)*1000)) # millisecond for the next 12 seconds

# run script
for (( i=$((start)); i<=$end; i++ )) do
    for (( vc1=0; vc1<=$((v-1)); vc1++ )) do
        vc01=$(printf "%02d" $vc1)
        for (( vc2=0; vc2<=$((v-1)); vc2++ )) do
            vc02=$(printf "%02d" $vc2)
            ssh pi@192.168.4.$i "cd CoMesh-on-Pis/ && java -Xmx1324m -cp target/CoMesh-on-Pis-1.0-SNAPSHOT-shaded.jar deploy.Deployer $config_fn $vc01$vc02 $start_time" &
        done
    done
done

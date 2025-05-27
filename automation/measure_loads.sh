start=15
end=15

function clean() {
    sh ~/CoMesh-on-Pis/automation/kill_all.sh $start $end
}

trap clean EXIT

clean

pi_dir="outputs/plain_loads"
local_dir=/Users/anna/GitHub/CoMesh-on-Pis/outputs/plain_loads/$4

for (( i=$start; i<=$end; i++ )) do
    echo "Creating directory $pi_dir at RPi 192.168.4.$i"
    ssh pi@192.168.4.$i "cd CoMesh-on-Pis/ && mkdir -p $pi_dir"
done

for (( i=$start; i<=$end; i++ )) do
    echo "Measuring loads on 192.168.4.$i"
    ssh pi@192.168.4.$i "cd CoMesh-on-Pis/ && java -cp target/CoMesh-on-Pis-1.0-SNAPSHOT-shaded.jar deploy.MeasureLoad" &
done

sleep 110

# copy result from pi to local
rm -r $local_dir/*
for (( i=$start; i<=$end; i++ )) do
    # echo "Creating local directory $local_dir/$i/"
    mkdir -p $local_dir/$i/
    scp -r pi@192.168.4.$i:~/CoMesh-on-Pis/$pi_dir $local_dir/$i/
done

clean
start=10
end=25

for (( i=$start; i<=$end; i++ )) do
do
    ssh pi@192.168.4.$i 'mkdir CoMesh-on-Pis'
    ssh pi@192.168.4.$i 'mkdir CoMesh-on-Pis/target'
    ssh pi@192.168.4.$i 'mkdir CoMesh-on-Pis/workloads'
    ssh pi@192.168.4.$i 'mkdir CoMesh-on-Pis/automation'
    ssh pi@192.168.4.$i 'mkdir CoMesh-on-Pis/outputs' &
done
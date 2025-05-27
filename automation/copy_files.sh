start=$1
end=$2
from=$3
to=$4

echo "Copying $from for nodes 192.168.4.$start to 192.168.4.$end"
for (( i=$start; i<=$end; i++ )) do
    echo "Copying to 192.168.4.$i:"
    ssh pi@192.168.4.$i "mkdir CoMesh-on-Pis/$to"
    scp -r $from pi@192.168.4.$i:CoMesh-on-Pis/$to
done

echo "File transfer completed"

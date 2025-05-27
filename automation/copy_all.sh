start=$1
end=$2

if [[ "$3" == *"t"* ]]; then
    mvn clean package -DskipTests
fi

echo "Copying files for nodes 192.168.4.$start to 192.168.4.$end"
for (( i=$start; i<=$end; i++ )) do
    echo "Copying to 192.168.4.$i:"
    if [[ "$3" == *"t"* ]]; then
        scp -r target/CoMesh-on-Pis-1.0-SNAPSHOT-shaded.jar pi@192.168.4.$i:CoMesh-on-Pis/target/
    fi
    if [[ "$3" == *"c"* ]]; then
        rsync -ru configs pi@192.168.4.$i:CoMesh-on-Pis
    fi
    if [[ "$3" == *"w"* ]]; then
        rsync -ru workloads pi@192.168.4.$i:CoMesh-on-Pis
    fi
    if [[ "$3" == *"a"* ]]; then
        rsync -ru automation pi@192.168.4.$i:CoMesh-on-Pis
    fi
    if [[ "$3" == *"m"* ]]; then
        rsync -ru ../medley/bin/config.json pi@192.168.4.$i:medley/bin/
    fi
done

echo "File transfer completed"

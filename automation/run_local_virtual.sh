config_fn=$1
v=$2
start_time=$3

vs=(${v/,/ })
v1=${vs[0]}
v2=${vs[1]}

for (( vc1=0; vc1<=$((v1-1)); vc1++ )) do
    vc01=$(printf "%02d" $vc1)
    for (( vc2=0; vc2<=$((v2-1)); vc2++ )) do
        vc02=$(printf "%02d" $vc2)
        echo java -Xmx50m -cp target/CoMesh-on-Pis-1.0-SNAPSHOT-shaded.jar deploy.Deployer $config_fn $vc01$vc02 $start_time
        java -XX:-OmitStackTraceInFastThrow -Xmx50m -cp target/CoMesh-on-Pis-1.0-SNAPSHOT-shaded.jar deploy.Deployer $config_fn $vc01$vc02 $start_time &
    done
done
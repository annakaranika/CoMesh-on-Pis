path=$1
v=$2
me=$3
file="load_histories.csv"

vs=(${v/,/ })
v1=${vs[0]}
v2=${vs[1]}

for (( vc1=0; vc1<=$((v1-1)); vc1++ )) do
    vc01=$(printf "%02d" $vc1)
    for (( vc2=0; vc2<=$((v2-1)); vc2++ )) do
        vc02=$(printf "%02d" $vc2)
        echo "$me: checking for file ${path}/${vc01}${vc02}/${file}"
        while [ ! -f "${path}/${vc01}${vc02}/${file}" ]
        do
            echo $me\_${vc01}${vc02}: still waiting to move on
            sleep 2
        done
    done
done

echo $me: moving on

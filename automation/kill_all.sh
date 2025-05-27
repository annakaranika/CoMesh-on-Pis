start=$1
end=$2

for (( i=$start; i<=$end; i++ )) do
    ssh pi@192.168.4.$i 'killall -9 java' &
done

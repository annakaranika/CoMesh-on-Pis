start=$1
end=$2

for (( i=$start; i<=$end; i++ )) do
  ping 192.168.4.$i
done
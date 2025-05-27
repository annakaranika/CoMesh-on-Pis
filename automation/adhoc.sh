start=10
end=25

for (( i=$start; i<=$end; i++ )) do
    echo "Disabling internet connection for 192.168.4.$i:"
    ssh pi@192.168.4.$i "cd medley/adhoc/ && sudo ./adhoc.sh" &
done

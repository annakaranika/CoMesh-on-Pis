start=$1
end=$2

pi_dir=$3
local_dir=~/CoMesh-on-Pis/outputs/$4

# copy result from pi to local
rm -r $local_dir/*
for (( i=$start; i<=$end; i++ )) do
    echo "Creating directory $local_dir/$i/"
    mkdir -p $local_dir/$i/
    scp -r pi@192.168.4.$i:~/CoMesh-on-Pis/outputs/$pi_dir $local_dir/$i/
done

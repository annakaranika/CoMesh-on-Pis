# CoMesh-on-Pis

This is the primary repo for deployment experiments of the paper
'There is More Control in Egalitarian Edge IoT Meshes'.

## Requirements:
* JDK 15+
* Apache Maven 3.6.3+

This is a Maven project.
You can compile it by running:

```
mvn clean package -DskipTests
```

The source code is located at the `src` directory.

The `configs` directory is where configuration files for a CoMesh node are saved.

The `workloads\scripts` directory contains scripts that create the workloads.
The use of specific workloads is specified in the passed configuration file.

The `automation` directory contains scripts for the automated management of the Raspberry Pis (RPis) in preparation for a CoMesh deployment.

The `outputs` directory is where metrics are aggregated from each RPi.

To run the project, you need to be connected to the Raspberry Pi mesh WiFi network
called IBSS-RPiNet.
It is a device network.

On macOS, one first needs to allow the system to

`Show legacy networks and options`.

This option can be found in (depending on the MacOS version):

`System preferences > (Network >) Wi-Fi > Advanced > tab Wi-Fi`.

After connecting to the network, you must change the MacOS system's IP to be in the same subnet as the Raspberry Pis, excluding the Pis' IP range.

After that, it is a good idea to ping all Pis' IPs.
This can be facilitated by the script `automation/connectivity_check.sh`.

CoMesh uses the [Medley](https://github.com/RuiRitaYang/medley) membership service system to be notified whenever nodes leave or join the system.

To run the system, you can execute script `automation/all.sh`.
This script starts Medley and then starts CoMesh.
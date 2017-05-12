#ISP
#sets the download bandwidth to the client
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc qdisc add dev ifb0 root handle 1: htb default 10
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc class add dev ifb0 parent 1: classid 1:1 htb rate 1mbit
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc class add dev ifb0 parent 1:1 classid 1:10 htb rate 1mbit
vivek@Dobby:~/Work/dobby-android/scripts$ 
vivek@Dobby:~/Work/dobby-android/scripts$ 
vivek@Dobby:~/Work/dobby-android/scripts$ 

#Sets the upload bandwidth from the client
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc qdisc add dev enp2s0 root handle 1: htb default 10
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc class add dev enp2s0 parent 1: classid 1:1 htb rate 1mbit
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc class add dev enp2s0 parent 1:1 classid 1:10 htb rate 1mbit

#Wifi
#Set upload speed
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc qdisc add dev wlxec086b132588 handle ffff: ingress
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc filter add dev wlxec086b132588 parent ffff: protocol ip u32 match u32 0 0 action mirred egress redirect dev ifb1
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc qdisc add dev ifb1 root handle 1: htb default 10
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc class add dev ifb1 parent 1: classid 1:1 htb rate 1mbit
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc class add dev ifb1 parent 1:1 classid 1:10 htb rate 1mbit

#Set download speed
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc qdisc add dev wlxec086b132588 root handle 1: htb default 10
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc class add dev wlxec086b132588 parent 1: classid 1:1 htb rate 1mbit
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc class add dev wlxec086b132588 parent 1:1 classid 1:10 htb rate 1mbit

# To delete the rule
sudo tc filter show dev enp2s0 parent ffff:
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc filter show dev enp2s0 parent ffff:
filter protocol ip pref 49152 u32 
filter protocol ip pref 49152 u32 fh 800: ht divisor 1 
filter protocol ip pref 49152 u32 fh 800::800 order 2048 key ht 800 bkt 0 terminal flowid ??? 
  match 00000000/00000000 at 0
	action order 1: mirred (Egress Redirect to device if5) stolen
 	index 1 ref 1 bind 1
 
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc filter del dev enp2s0 pref 49152
RTNETLINK answers: Invalid argument
We have an error talking to the kernel
vivek@Dobby:~/Work/dobby-android/scripts$ sudo tc filter del dev enp2s0 parent ffff: handle 800::800 prio 49152 protocol ip u32


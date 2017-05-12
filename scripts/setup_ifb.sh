# POSIX
# Usage info
show_help() {
cat << EOF
        Usage: ${0##*/} [-hv] [-i ISP_BW] [-w WIFI_BW] [-d ISP_INTERFACE] [-a WIFI_INTERFACE] [-t AP_TXPOWER]

        -h           display this help and exit
        -d|--ispdev  isp interface name (like eth0)
        -a|--apdev   wifi AP link interface name (like wlan0)
        -v           verbose mode. Can be used multiple times for increased verbosity.
EOF
}

# Reset all variables that might be set
isp_interface=
ap_interface=
verbose=0 # Variables to be evaluated as shell arithmetic should be initialized to a default or validated beforehand.


verbose_echo () {
    if [ $verbose -gt 0 ]; then
        echo $1
    fi
}

delete_rules () {
    #Delete ingress rules
    tc qdisc del dev $interface root
}


cleanup_ifb() {
	ifconfig_ifb0=`ifconfig ifb0`	
	ifconfig_ifb1=`ifconfig ifb1`
	if [ -n "$ifconfig_ifb0" ]; then
		delete_rules ifb0
	fi
	if [ -n "$ifconfig_ifb1" ]; then
		delete_rules ifb1
	fi
	rmmod ifb
}
	

#set up ifb interfaces
setup_ifb () {
	echo "num ifbs $num_ifbs"
	if [ $num_ifbs -gt 0 ]; then
    	rmmod ifb
    	modprobe ifb numifbs=$num_ifbs
    	ifb_counter=0
    	while [  $ifb_counter -lt $num_ifbs ]; do
        	echo "ip link set dev ifb{ifb_counter} up"
        	ip link set dev ifb${ifb_counter} up
        	let ifb_counter=ifb_counter+1
    	done
	fi
}

if [ "$#" -lt 1 ]; then
	show_help
	exit
fi

while :; do
    case $1 in
        -h|-\?|--help)   # Call a "show_help" function to display a synopsis, then exit.
            show_help
            exit
            ;;
        -d|--ispdev)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                isp_interface=$2
                shift
            else
                printf 'ERROR: "--ispdev|-d" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
        -a|--apdev)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                ap_interface=$2
                shift
            else
                printf 'ERROR: "--apdev|-a" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
        -v|--verbose)
            verbose=$((verbose + 1)) # Each -v argument adds 1 to verbosity.
            ;;
        --)              # End of all options.
            shift
            break
            ;;
        -?*)
            printf 'WARN: Unknown option (ignored): %s\n' "$1" >&2
            ;;
        *)               # Default case: If no more options then break out of the loop.
            break
    esac
    shift
done

num_ifbs=0
if [ -n "$isp_interface" ]; then
    num_ifbs=$(( $num_ifbs + 1 ))    
fi
if [ -n "$wifi_interface" ]; then
    num_ifbs=$(( $num_ifbs + 1 ))    
fi


#Cleanup previous stuff
cleanup_ifb
#set up isp stuff
setup_ifb

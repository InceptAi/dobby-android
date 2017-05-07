#!/bin/sh
# POSIX
# Usage info
show_help() {
cat << EOF
        Usage: ${0##*/} [-hv] [-f OUTFILE] [FILE]...
        Do stuff with FILE and write the result to standard output. With no FILE
        or when FILE is -, read standard input.

        -h           display this help and exit
        -i|--ispbw   isp bandwidth (in Mbps, e.g. -i 2 == 2Mbps)
        -w|--wifibw  wifi bandwidth (in Mbps e.g. -w 2 == 2Mbps)
        -d|--ispdev  isp interface name (like eth0)
        -a|--apdev   wifi AP link interface name (like wlan0)
        -w|--wifibw  wifi bandwidth
        -t|--txpower transmit power for AP
        -v          verbose mode. Can be used multiple times for increased
        verbosity.
EOF
}

# Reset all variables that might be set
isp_interface=
ap_interface=
isp_bw=0
wifi_bw=0
txpower=0
verbose=0 # Variables to be evaluated as shell arithmetic should be initialized to a default or validated beforehand.


verbose_echo () {
    if [ $verbose -gt 0 ]; then
        echo $1
    fi
}

setup_bw () { #Call with real_interface, ifb_interface, download, upload mbps
    if [ "$#" -ne 4 ]; then
        echo "ERROR: need to call setup bw like -- setup_bw real_interface ifb_interface download_mbps upload_mbps"
        return;
    fi
    real_interface=$1
    ifb_interface=$2
    download_mbps=$3
    upload_mbps=$4
    verbose_echo "In setup_bw with real:$real_interface ifb:$ifb_interface download_mbps:$download_mbps upload_mbps:$upload_mbps"
    #Forward ingress traffic to ifb0
    tc qdisc add dev $real_interface handle ffff: ingress
    tc filter add dev $real_interface parent ffff: protocol ip u32 match u32 0 0 action mirred egress redirect dev $ifb_interface

    #Set up dev egress bandwidth (outgoing)
    tc qdisc add dev $real_interface root handle 1: htb default 10
    tc class add dev $real_interface parent 1: classid 1:1 htb rate ${download_mbps}mbit
    tc class add dev $real_interface parent 1:1 classid 1:10 htb rate ${download_mbps}mbit

    #Set up dev ingress bandwidth (incoming)
    tc qdisc add dev $ifb_interface root handle 1: htb default 10
    tc class add dev $ifb_interface parent 1: classid 1:1 htb rate ${upload_mbps}mbit
    tc class add dev $ifb_interface parent 1:1 classid 1:10 htb rate ${upload_mbps}mbit
}


while :; do
    case $1 in
        -h|-\?|--help)   # Call a "show_help" function to display a synopsis, then exit.
            show_help
            exit
            ;;
        -i|--ispbw)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                isp_bw=$2
                shift
            else
                printf 'ERROR: "--ispbw|-i" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
        -w|--wifibw)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                wifi_bw=$2
                shift
            else
                printf 'ERROR: "--wifibw|-w" requires a non-empty option argument.\n' >&2
                exit 1
            fi
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
        -t|--txpower)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                tx_power=$2
                shift
            else
                printf 'ERROR: "--txpower|-t" requires a non-empty option argument.\n' >&2
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

#Error checking

# if --file was provided, open it for writing, else duplicate stdout
num_ifbs=0
if [ $isp_bw -gt 0 ]; then
    if [ ! -n "$isp_interface" ]; then
        printf 'ERROR: need to specify isp interface if isp bw (-ispbw|-i) is set to non zero'
        exit 0;
    fi
    num_ifbs=`$num_ifbs + 1`
fi
if [ $wifi_bw -gt 0 ]; then
    if [ ! -n "$ap_interface" ]; then
        printf 'ERROR: need to specify isp interface if ap bw (-wifibw|-w) is set to non zero'
        exit 0;
    fi
    num_ifbs=`$num_ifbs + 1`
fi

#set up ifb interfaces
if [ $num_ifbs -gt 0]; then
    rmmod ifb
    modprobe ifb numifbs=$num_ifbs
    ifb_counter=0
    while [  $ifb_counter -lt $num_ifbs ]; do
        echo "ip link set dev ifb{ifb_counter} up"
        ip link set dev ifb${ifb_counter} up
        let ifb_counter=ifb_counter+1
    done
fi

#set up isp stuff
if [ $isp_bw -gt 0 ]; then
    setup_bw $isp_interface ifb0 $isp_bw $isp_bw
fi

#set up wifi stuff
if [ $wifi_bw -gt 0 ]; then
    setup_bw $wifi_interface ifb1 $wifi_bw $wifi_bw
fi

#set up wifi power
if [ $txpower -gt 0 ]; then
    iwconfig $wifi_interface txpower $txpower
fi

# Rest of the program here.
# If there are input files (for example) that follow the options, they
# will remain in the "$@" positional parameters.


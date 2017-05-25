#!/bin/bash
# POSIX
# Usage info
show_help() {
cat << EOF
        Usage: ${0##*/} [-hvr] [-d TESTDIR] [-i MIN_TEST_INTERVAL_MINS] [-g githubrepo]

        -h            display this help and exit
		-r 			  check for git updates after interval and run tests if new updates (default: run once)
        -d|--testdir  test dir (/tmp/SpoonTests) by default
        -p|--gmplayerpath  Genymotion player path
		-i|--interval min interval between tests in mins(default is 10 mins)
		-g|--github   github clone repo (default:dobby-android -- https://github.com/InceptAi/dobby-android.git)
		-f|--emulator_file_list   github clone repo (default:dobby-android -- https://github.com/InceptAi/dobby-android.git)
        -v            verbose mode. Can be used multiple times for increased verbosity.
EOF
}

# Reset all variables that might be set
genymotion_player_path=
TEST_DIR="/tmp/SpoonTests"
GITHUB_REPO="https://github.com/InceptAi/dobby-android.git"
test_interval=0
verbose=0 # Variables to be evaluated as shell arithmetic should be initialized to a default or validated beforehand.
repeat_tests=0
emulator_list_file=
verbose_echo () {
    if [ $verbose -gt 0 ]; then
        echo $1
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
        -d|--testdir)    # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                TEST_DIR=$2
                shift
            else
                printf 'ERROR: "-d|--testdir" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
        -g|--github)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                GITHUB_REPO=$2
                shift
            else
                printf 'ERROR: "--github|-b" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
        -i|--interval)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                test_interval=$2
                shift
            else
                printf 'ERROR: "--interval|-i" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
        -f|--emulatorfile)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                emulator_list_file=$2
                shift
            else
                printf 'ERROR: "--emulatorfile|-f" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
 

        -v|--verbose)
            verbose=$((verbose + 1)) # Each -v argument adds 1 to verbosity.
            ;;
        -r|--repeat)
            repeat=$((repeat + 1)) 
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


one_time_setup () {
	rm -rf $TEST_DIR
	mkdir -p $TEST_DIR
	cd $TEST_DIR
	git clone $GITHUB_REPO
	cd $GITHUB_REPO_NAME
}


check_git () {
	cd ${PATH_TO_REPO_DIR}
	git fetch --quiet
	UPSTREAM=${1:-'@{u}'}
	LOCAL=$(git rev-parse @)
	REMOTE=$(git rev-parse "$UPSTREAM")
	BASE=$(git merge-base @ "$UPSTREAM")
	if [ $LOCAL = $REMOTE ]; then
    	echo "up-to-date"
	elif [ $LOCAL = $BASE ]; then
    	echo "Need to pull"
	elif [ $REMOTE = $BASE ]; then
    	echo "Need to push"
	else
    	echo "Diverged"
	fi
}

# https://github.com/InceptAi/dobby-android.git
GITHUB_REPO_NAME=`echo $GITHUB_REPO | rev | cut -d "/" -f 1 | rev | cut -d "." -f 1`
PATH_TO_REPO_DIR="$TEST_DIR/$GITHUB_REPO_NAME"
GRADLEW_PATH="$PATH_TO_REPO_DIR/Dobby"
DOBBY_SERVER_HOME="$PATH_TO_REPO_DIR/server"

ARUNESH_EMAIL="arunesh@obiai.tech"
VIVEK_EMAIL="vivek@obiai.tech"
BODY="Test Email"
SUBJECT="Gradle Build"
REL_PATH_DEBUG_APK="app/build/outputs/apk/app-wifidoc-debug.apk"
REL_PATH_INSTRUMENTATION_APK="app/build/outputs/apk/app-wifidoc-debug-androidTest.apk"
MIN_API_LEVEL_TO_USE_G_INSTALL_OPTION=23
OUTPUT_PATH="/home/vivek/Work/dobby-android/Dobby/app/build/spoon/wifidoc"

uninstall_app () {
    #uninstall the apks from the device
    adb shell pm uninstall com.inceptai.dobby.wifidoc.debug >> /tmp/gradle.log
    adb shell pm uninstall com.inceptai.dobby.wifidoc.debug.test >> /tmp/gradle.log
}

install_app () {
    if [ $# -ne 1 ]; then
		echo "Need API level for installation"
		exit
	fi
	api_level=$1
    #install the apks on the target device
    adb push $REL_PATH_DEBUG_APK /data/local/tmp/com.inceptai.dobby.wifidoc.debug >> /tmp/gradle.log
	if [ $api_level -lt 23 ]; then
		adb shell pm install -r "/data/local/tmp/com.inceptai.dobby.wifidoc.debug" >> /tmp/gradle.log
	else
    	adb shell pm install -r -g "/data/local/tmp/com.inceptai.dobby.wifidoc.debug" >> /tmp/gradle.log
	fi
    adb push $REL_PATH_INSTRUMENTATION_APK /data/local/tmp/com.inceptai.dobby.wifidoc.debug.test >> /tmp/gradle.log
    adb shell pm install -r "/data/local/tmp/com.inceptai.dobby.wifidoc.debug.test" >> /tmp/gradle.log
}

notify_failure () {
	echo "BUILD FAILED"
	BODY="Gradle build failed: See http://dobby1743.duckdns.org:5187/ for UI test results"
	ATTACHMENT="/tmp/gradle.log"
	echo ${BODY}| mail -s "Gradle Build Failed for WifiDoc" -A ${ATTACHMENT} $VIVEK_EMAIL $ARUNESH_EMAIL
}

notify_success () {
	echo "BUILD SUCCEEDED"
	BODY="Gradle build succeeded: See http://dobby1743.duckdns.org:5187/ for UI screenshots"
	echo ${BODY}| mail -s "Gradle Build Success for WifiDoc" $VIVEK_EMAIL $ARUNESH_EMAIL
}

wait_for_emulator () {
	echo "Coming in wait_for_emulator"
    bootanim=""
    failcounter=0
    timeout_in_sec=360

    until [[ "$bootanim" =~ "stopped" ]]; do
        #bootanim=`adb -e shell getprop init.svc.bootanim 2>&1 &`
        bootanim=`adb wait-for-device shell getprop init.svc.bootanim 2>&1 &`
        if [[ "$bootanim" =~ "device not found" || "$bootanim" =~ "device offline"
            || "$bootanim" =~ "running" ]]; then
            let "failcounter += 1"
            echo "Waiting for emulator to start"
            if [[ $failcounter -gt timeout_in_sec ]]; then
				echo "Timing out"
                echo "Timeout ($timeout_in_sec seconds) reached; failed to start emulator"
                exit 1
            fi
		fi
        sleep 1
    done
    echo "Emulator is ready"
}

kill_emulators () {

	#Killing the genymotion player
	player_pids=`ps aux | grep player | tr -s " " | cut -d " " -f 2`
	if [ ! -z "$player_pids" ]; then
		num_players=`echo $player_pids | wc -w`
		for (( i=1; i<=$(( $num_players )); i++ ))
		do 
			pid_to_kill=`echo $player_pids | cut -d " " -f $i`
    		echo -n "killing pid " $pid_to_kill
			kill $pid_to_kill
		done
	fi

	#Killing the VBoxHeadless
	headless_pids=`ps aux | grep VBoxHeadless | tr -s " " | cut -d " " -f 2`
	if [ ! -z "$headless_pids" ]; then
		num_headless=`echo $headless_pids | wc -w`
		for (( i=1; i<=$(( $num_headless )); i++ ))
		do 
			pid_to_kill=`echo $headless_pids | cut -d " " -f $i`
    		echo -n "killing pid " $pid_to_kill
			kill $pid_to_kill
		done
	fi
	killall -9 VBoxHeadless
}

clean_slate () {
	kill_emulators
	adb kill-server
	adb start-server
}


git_pull () {
	cd ${PATH_TO_REPO_DIR}
	git pull
}

wait_for_git_changes () {
	git_status="up-to-date"
	while [[ "$git_status" == "up-to-date" ]]; do
		sleep ${test_interval}m #Sleep for interval mins
		git_status=check_git
	done
}

run_emulator_tests () {

    echo "List of emulators:"
    cat $emulator_list_file
    should_report_failure=0


    rm -rf $DOBBY_SERVER_HOME/spoon/wifidoc
    clean_slate
    num_lines=`wc -l $emulator_list_file | cut -d ' ' -f1`
    for i in $(seq 1 $num_lines); do
   	    line=`sed -n ${i}p $emulator_list_file`
        emulator_info="$line"
        if [ -z "$emulator_info" ]; then
            continue
        fi
        
        is_commented=`echo $emulator_info | grep "#"`
        if [ ! -z "$is_commented" ]; then
            echo "Emulator $emulator_info"
            continue
        fi
        
        echo "Running test for id $emulator_info"
        emulator_id=`echo $emulator_info | cut -d " " -f 1`
        api_level=`echo $emulator_info | cut -d " " -f 2`
        echo "emulator id, api level" $emulator_id $api_level 
        
        #Start the emulator
        $GENYMOTION_HOME/player --vm-name $emulator_id &	
        EMULATOR_PID=$!
        echo "Started emulator with pid:" $EMULATOR_PID

        #Slight delay before we check
        sleep 5

        #Wait for it to start
        wait_for_emulator

        #Sleep for a few secs	
        sleep 15

        #Uninstall the app
        uninstall_app

        #Install the app
        install_app $api_level

        #Run the test
        ./gradlew spoonWifidocDebugAndroidTest --stacktrace >>  /tmp/gradle.log
        
        #Store the output in a diff location
        mkdir -p $DOBBY_SERVER_HOME/spoon/wifidoc/$api_level
        cp -r $OUTPUT_PATH/debug/* $DOBBY_SERVER_HOME/spoon/wifidoc/$api_level/
        
        if [ $? -gt 0 ]; then
            should_report_failure=1
        fi

        #Repeat for different emulators
        uninstall_app
        
        #Kill the emulator
        kill $EMULATOR_PID
        sleep 10
        
        clean_slate
        echo "Done sleeping, onto the next one"
    done
}


build_test_targets () {
    cd $GRADLEW_PATH
    echo "Starting gradle build" > /tmp/gradle.log
    ./gradlew clean build assembleAndroidTest >> /tmp/gradle.log
    if [ $? -gt 0 ]; then
        notify_failure
        exit 1
    fi
}
	 
#Starting tests
#set up the dirs
one_time_setup

while [ $repeat_tests -gt 0 ]; do 

    #git changed, do a pull and run tests
    git_pull

    #build the tests
    build_test_targets

    #Check emulator list exists
    if [ ! -f $emulator_list_file ]; then
        echo "$emulator_list_file does not exist. Exiting"
        exit 0
    fi

    if [ ! -n $genymotion_player_path ]; then
        echo "Genymotion player path not defined, can't run emulator tests"
        exit 0
    fi


    #run the tests
    run_emulator_tests


    if [ $should_report_failure -gt 0 ]; then
        notify_failure
        exit 1
    else 
        notify_success
        exit 0
    fi
    
    #Wait for more git changes
    wait_for_git_changes 
done

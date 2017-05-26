#!/bin/bash
# POSIX
# Usage info
show_help() {
cat << EOF
        Usage: ${0##*/} [-hvr] [-d TESTDIR] [-i MIN_TEST_INTERVAL_MINS] [-g githubrepo]

        -h|--help display this help and exit
        -r|--repeat   check for git updates after interval and run tests if new updates (default: run once)
        -d|--testdir  test dir (/tmp/SpoonTests) by default
        -p|--gmplayerpath  Genymotion player path
        -j|--javahomepath  Path to java installation (Make sure javahome/bin/java and javahome/bin/javac exist, only needed if JAVA_HOME is not set) 
        -b|--vboxmanagepath Path to virtual box installtion (Make sure virtualbox/bin/VBoxManage exist, only needed if VBoxManage is NOT in your path)
        -a|--androidhomepath Android Home path (Only needed if ANDROID_HOME is not set)
        -i|--interval min interval between tests in mins(default is 10 mins)
        -g|--github   github clone repo (default:dobby-android -- https://github.com/InceptAi/dobby-android.git)
        -f|--emulator_file_list   github clone repo (default:dobby-android -- https://github.com/InceptAi/dobby-android.git)
        -v|--verbose  verbose mode. Can be used multiple times for increased verbosity.
        -c|--checkconfig            check config and exit.
        -e|--emailtonotify   email address to notify (make sure your mail is set up).
EOF
}

verbose_echo () {
    if [ $VERBOSE -gt 0 ]; then
        echo $1
    fi
}

gen_fake_keystore () {
	mkdir -p $TEST_DIR/Keystore
	cd $TEST_DIR/Keystore
	touch foo.jks
	echo "storePassword=foo" > wifidoc-keystore.properties
	echo "keyPassword=foo" >> wifidoc-keystore.properties
	echo "keyAlias=foo" >> wifidoc-keystore.properties
	echo "storeFile=$TEST_DIR/Keystore/foo.jks" >> wifidoc-keystore.properties
	cp wifidoc-keystore.properties wifiexpert-keystore.properties
}

one_time_setup () {
	rm -rf $TEST_DIR
	mkdir -p $TEST_DIR
	cd $TEST_DIR
	git clone $GITHUB_REPO
	cd $GITHUB_REPO_NAME
	echo "sdk.dir=$ANDROID_HOME" > $GRADLEW_PATH/local.properties	
	gen_fake_keystore
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

uninstall_app () {
    #uninstall the apks from the device
    $ADB shell pm uninstall com.inceptai.dobby.wifidoc.debug >> /tmp/gradle.log
    $ADB shell pm uninstall com.inceptai.dobby.wifidoc.debug.test >> /tmp/gradle.log
}

install_app () {
    if [ $# -ne 1 ]; then
		echo "Need API level for installation"
		exit
	fi
	api_level=$1
    #install the apks on the target device
    $ADB push $DOBBY_PATH/$REL_PATH_DEBUG_APK /data/local/tmp/com.inceptai.dobby.wifidoc.debug >> /tmp/gradle.log
	if [ $api_level -lt 23 ]; then
		$ADB shell pm install -r "/data/local/tmp/com.inceptai.dobby.wifidoc.debug" >> /tmp/gradle.log
	else
    	$ADB shell pm install -r -g "/data/local/tmp/com.inceptai.dobby.wifidoc.debug" >> /tmp/gradle.log
	fi
    $ADB push $DOBBY_PATH/$REL_PATH_INSTRUMENTATION_APK /data/local/tmp/com.inceptai.dobby.wifidoc.debug.test >> /tmp/gradle.log
    $ADB shell pm install -r "/data/local/tmp/com.inceptai.dobby.wifidoc.debug.test" >> /tmp/gradle.log
}

notify_failure () {
	echo "BUILD FAILED"
	BODY="Gradle build failed: See the results at $DOBBY_SERVER_HOME/spoon for UI test results. You can also just run the server like python $DOBBY_SERVER_HOME/server.py and see the results at http://0.0.0.0/index.html"
	ATTACHMENT="/tmp/gradle.log"
	echo ${BODY}| mail -s "Gradle Build Failed for WifiDoc" -A ${ATTACHMENT} $EMAIL_TO_NOTIFY
}

notify_success () {
	echo "BUILD SUCCEEDED"
	BODY="Gradle build succeeded: See the results at $DOBBY_SERVER_HOME/spoon for UI test results. You can also just run the server like python $DOBBY_SERVER_HOME/server.py and see the results at http://0.0.0.0/index.html"
	echo ${BODY}| mail -s "Gradle Build Success for WifiDoc" $EMAIL_TO_NOTIFY
}

wait_for_emulator () {
	echo "Coming in wait_for_emulator"
    bootanim=""
    failcounter=0
    timeout_in_sec=360

    until [[ "$bootanim" =~ "stopped" ]]; do
        #bootanim=`adb -e shell getprop init.svc.bootanim 2>&1 &`
        bootanim=`$ADB wait-for-device shell getprop init.svc.bootanim 2>&1 &`
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
	player_pids=`ps aux | grep -v "automate" | grep player | tr -s " " | cut -d " " -f 2`
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
	$ADB kill-server
	$ADB start-server
}


git_pull () {
	cd ${PATH_TO_REPO_DIR}
	git pull
}

wait_for_git_changes () {
	git_status="up-to-date"
	while [[ "$git_status" == "up-to-date" ]]; do
		sleep ${TEST_INTERVAL}m #Sleep for interval mins
		git_status=check_git
	done
}

run_emulator_tests () {

    echo "List of emulators:"
    cat $EMULATOR_LIST_FILE
    should_report_failure=0


    rm -rf $DOBBY_SERVER_HOME/spoon/wifidoc
    clean_slate
    num_lines=`wc -l $EMULATOR_LIST_FILE | cut -d ' ' -f1`
    for i in $(seq 1 $num_lines); do
   	    line=`sed -n ${i}p $EMULATOR_LIST_FILE`
        emulator_info="$line"
		echo "processing $emulator_info"
        if [ -z "$emulator_info" ]; then
            continue
        fi
        is_commented=`echo $emulator_info | grep "#"`
        if [ ! -z "$is_commented" ]; then
            echo "Emulator $emulator_info"
            continue
        fi
        
        echo "Running test for id $emulator_info"
        emulator_id=`echo $emulator_info | cut -d "{" -f 2 | cut -d "}" -f 1`
    	api_level=`echo $emulator_info | grep -o -P 'API.{0,3}' | cut -d " " -f 2`
		#emulator_id=`echo $emulator_info | cut -d " " -f 1`
        #api_level=`echo $emulator_info | cut -d " " -f 2`
        echo "emulator id, api level" $emulator_id $api_level 
        
        #Start the emulator
        $GENYMOTION_PLAYER --vm-name $emulator_id &	
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
		cd $GRADLEW_PATH
		echo $PWD
        $GRADLEW_PATH/gradlew spoonWifidocDebugAndroidTest --stacktrace >>  /tmp/gradle.log
        
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
    #./gradlew clean assembleDebug assembleAndroidTest >> /tmp/gradle.log
    ./gradlew clean assembleDebug >> /tmp/gradle.log
    if [ $? -gt 0 ]; then
        notify_failure
        exit 1
    fi
}

generate_emulator_list_file () {
	EMULATOR_LIST_FILE="/tmp/vms"
	$VBOX_MANAGE list vms > $EMULATOR_LIST_FILE
}

check_config () {
	all_reqs_met=1
	#Prereqs == Genymotion player, VBox, Java Home, Android platform tools
	
	#Set java home if not set
	current_java_home=`echo $JAVA_HOME`
	if [ ! -z $JAVA_HOME_PATH ]; then
		if [ ! -f $JAVA_HOME_PATH/bin/java ]; then
			echo "Incorrect Java Home path; $JAVA_HOME_PATH/bin/java not found"		
			all_reqs_met=0
		fi
		JAVA_HOME=$JAVA_HOME_PATH
	elif [ ! -z $current_java_home ]; then
		if [ ! -f $current_java_home/bin/java ]; then
			echo "Incorrect Java Home path; $current_java_home/bin/java not found"		
			all_reqs_met=0
		fi
	else
		#TODO:Install java and set java home
		echo "JAVA_HOME not set, either set in shell or provide java path to script"		
		all_reqs_met=0
	fi

	#Set adb path if not set
	current_android_home=`echo $ANDROID_HOME`
	echo "current_android_home is $ANDROID_HOME"
	if [ ! -z $ANDROID_HOME_PATH ]; then
		if [ ! -f $ANDROID_HOME_PATH/platform-tools/adb ]; then
			echo "Incorrect ADB path; adb not found at $ANDROID_HOME_PATH/platform-tools/adb"		
			all_reqs_met=0
		fi
		ANDROID_HOME=$ANDROID_HOME_PATH
		ADB=$ANDROID_HOME/platform-tools/adb
	elif [ -z $current_android_home ]; then
		echo "ANDROID_HOME not set, make sure it is in the path or specify path via -a|--androidhomepath"
		all_reqs_met=0
	else
		ADB=$ANDROID_HOME/platform-tools/adb
	fi

	#Set adb path if not set
	current_vboxmanage=`which VBoxManage`
	if [ ! -z $VBOX_MANAGE ]; then
		if [ ! -f $VBOX_MANAGE ]; then
			echo "Incorrect VBoxManage path; VBoxManage not found at $VBOX_MANAGE"		
			all_reqs_met=0
		fi
	elif [ -z $current_vboxmanage ]; then
		echo "VBoxManage not found, make sure it is in the path or specify it via -b|--vboxmanagepath"
		all_reqs_met=0
	else
		VBOX_MANAGE=$current_vboxmanage
	fi

	if [ ! -z $GENYMOTION_PLAYER ]; then
		if [ ! -f $GENYMOTION_PLAYER ]; then
			echo "Incorrect Genymotion player path; player not found at $GENYMOTION_PLAYER"		
			all_reqs_met=0
		fi
	else 
		echo "Must specify genymotion player path via -p|--gmplayerpath"
		all_reqs_met=0
	fi


	if [ $all_reqs_met -eq 0 ]; then
		echo "Fix issues with script pre-reqs. And then restart"
		exit 1
	fi

	if [ $CHECK_CONFIG_ONLY -gt 0 ]; then
		echo "Configs are all good. You can run the tests"
		exit 0
	fi
}

run_tests_one_iteration () {
    cd $PATH_TO_REPO_DIR
	#git changed, do a pull and run tests
    git_pull

    #build the tests
    #build_test_targets

    #Check emulator list exists
    if [ -z $EMULATOR_LIST_FILE ]; then
		echo "Generating emulator test file"
		generate_emulator_list_file
    fi

    if [ ! -f $EMULATOR_LIST_FILE ]; then
        echo "Emulator list file not found"
        exit 0
    fi

    if [ -z $GENYMOTION_PLAYER ]; then
        echo "Genymotion player path not defined, can't run emulator tests"
        exit 0
    fi


    #run the tests
    run_emulator_tests


    if [ $should_report_failure -gt 0 ]; then
        echo "notify_failure"
		notify_failure
    else 
		echo "notify_success"
        notify_success
    fi
}

echo "Android home is $ANDROID_HOME"
echo "Android SDK home is $ANDROID_SDK_HOME"

# Reset all variables that might be set
GENYMOTION_PLAYER=
TEST_DIR="/tmp/SpoonTests"
GITHUB_REPO="https://github.com/InceptAi/dobby-android.git"
TEST_INTERVAL=0
VERBOSE=0 # Variables to be evaluated as shell arithmetic should be initialized to a default or validated beforehand.
REPEAT_TESTS=0
EMULATOR_LIST_FILE=
CHECK_CONFIG_ONLY=0
VBOX_MANAGE=
ANDROID_HOME_PATH=
ADB=
JAVA_HOME_PATH=
EMAIL_TO_NOTIFY=hello@obiai.tech

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
                TEST_DIR=$2/SpoonTests
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
                TEST_INTERVAL=$2
                shift
            else
                printf 'ERROR: "--interval|-i" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
        -f|--emulatorfile)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                EMULATOR_LIST_FILE=$2
                shift
            else
                printf 'ERROR: "--emulatorfile|-f" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
         -p|--gmplayerpath)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                GENYMOTION_PLAYER=$2
                shift
            else
                printf 'ERROR: "--gmplayerpath|-p" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
        -j|--javahomepath)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                JAVA_HOME_PATH=$2
                shift
            else
                printf 'ERROR: "--javahomepath|-j" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
         -b|--vboxmanagepath)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                VBOX_MANAGE=$2
                shift
            else
                printf 'ERROR: "--vboxmanagepath|-b" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
          -a|--androidhomepath)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                ANDROID_HOME_PATH=$2
                shift
            else
                printf 'ERROR: "--androidhomepath|-a" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
          -e|--emailtonotify)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                EMAIL_TO_NOTIFY=$2
                shift
            else
                printf 'ERROR: "--emailtonotify|-e" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
 
          -v|--verbose)
            VERBOSE=$((VERBOSE + 1)) # Each -v argument adds 1 to verbosity.
            ;;
          -r|--repeat)
            REPEAT_TESTS=$((REPEAT_TESTS + 1)) 
            ;;
          -c|--checkconfig)
            CHECK_CONFIG_ONLY=$((CHECK_CONFIG_ONLY + 1)) # Each -v argument adds 1 to verbosity.
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



GITHUB_REPO_NAME=`echo $GITHUB_REPO | rev | cut -d "/" -f 1 | rev | cut -d "." -f 1`
PATH_TO_REPO_DIR="$TEST_DIR/$GITHUB_REPO_NAME"
GRADLEW_PATH="$PATH_TO_REPO_DIR/Dobby"
DOBBY_PATH="$PATH_TO_REPO_DIR/Dobby"
DOBBY_SERVER_HOME="$PATH_TO_REPO_DIR/server"

ARUNESH_EMAIL="arunesh@obiai.tech"
VIVEK_EMAIL="vivek@obiai.tech"
BODY="Test Email"
SUBJECT="Gradle Build"
REL_PATH_DEBUG_APK="app/build/outputs/apk/app-wifidoc-debug.apk"
REL_PATH_INSTRUMENTATION_APK="app/build/outputs/apk/app-wifidoc-debug-androidTest.apk"
MIN_API_LEVEL_TO_USE_G_INSTALL_OPTION=23
OUTPUT_PATH="$PATH_TO_REPO_DIR/Dobby/app/build/spoon/wifidoc"


#Check config first
check_config
 
#Starting tests
#set up the dirs
one_time_setup

#One iteration of tests
run_tests_one_iteration

while [ $REPEAT_TESTS -gt 0 ]; do 
    #Wait for more git changes
    wait_for_git_changes
	
	#Run another iteration
	run_tests_one_iteration
done 

#!/bin/bash
# POSIX
# Usage info
show_help() {
cat << EOF
        Usage: ${0##*/} [-hvrc] [-d TESTDIR] [-i MIN_TEST_INTERVAL_MINS] [-p
        GENYMOTION_PLAYER_PATH] [-k JAVA_HOME_PATH] [-b VBOX_MANAGE_PATH] [-a
        ANDROID_HOME_PATH] [-f EMULATOR_LIST] [-e EMAILS_TO_NOTIFY] [-o
        OUTPUT_DIR] [-n MAX_VMS_TO_RUN] [-g GITHUB_CLONE_URL] [-u RESULT_URL]
        [-z BUILD_FLAVORS]

        -h|--help display this help and exit
        -r|--repeat   check for git updates after interval and run tests if new updates (default: run once)
        -d|--testdir  test dir (/tmp/SpoonTests) by default
        -p|--gmplayerpath  Genymotion player path
        -j|--javahomepath  Path to java installation (Make sure javahome/bin/java and javahome/bin/javac exist, only needed if JAVA_HOME is not set) 
        -b|--vboxmanagepath Path to virtual box installtion (Make sure virtualbox/bin/VBoxManage exist, only needed if VBoxManage is NOT in your path)
        -a|--androidhomepath Android Home path (Only needed if ANDROID_HOME is not set)
        -i|--interval min interval between tests in mins(default is 10 mins)
        -g|--github github clone repo (default:dobby-android -- https://github.com/InceptAi/dobby-android.git)
        -f|--emulator_file_list (Optional -- specify explicitly which vms to use) 
        -v|--verbose  verbose mode. Can be used multiple times for increased verbosity.
        -c|--checkconfig            check config and exit.
        -e|--emailstonotify   email address to notify (make sure your mail is set up).
        -o|--outputdir  Output dir to copy all the results
        -n|--numvms Number of vms to run test on (def: 1, will pick top n from the list returned by VBoxManage)
        -u|--resulturl URL for viewing results (default: 0.0.0.0:5187/ -- if running server.py in the repo).
        -z|--buildflavors (comma separated. default:wifidoc,dobby)
EOF
}

generate_space_separated_emails () {
	emails_to_separate=$1
	IFS=',' read -r -a email_list <<< "$emails_to_separate"
	emails_space_sep=""
	for email_id in "${email_list[@]}"
	do
		emails_space_sep="$emails_space_sep $email_id"
	done
	echo "$emails_space_sep"	
}

verbose_echo () {
    if [ $VERBOSE -gt 0 ]; then
        echo $1
    fi
}

# Define a timestamp function
timestamp() {
  date +"%Y-%m-%d-%H-%M-%S"
}

echo_ts () {
    echo "$(timestamp) : $1"
}

gen_fake_keystore () {
	cd $PATH_TO_REPO_DIR
	mkdir ../Keystore
	cp dummy-keystore.properties ../Keystore/wifidoc-keystore.properties
	cp dummy-keystore.properties ../Keystore/wifiexpert-keystore.properties
	cp dummy-keystore.jks ../Keystore/
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
    	echo "need-to-pull"
	elif [ $REMOTE = $BASE ]; then
    	echo "need-to-push"
	else
    	echo "diverged"
	fi
}

uninstall_app () {
	current_build_flavor=$1
    #uninstall the apks from the device
    $ADB shell pm uninstall com.inceptai.dobby.${current_build_flavor}.debug >> /tmp/gradle.log
    $ADB shell pm uninstall com.inceptai.dobby.${current_build_flavor}.debug.test >> /tmp/gradle.log
}

install_app () {
    if [ $# -ne 2 ]; then
		echo "Need API level, BUILD flavor for installation"
		exit
	fi
	api_level=$1
	current_build_flavor=$2
	relavtive_path_debug_apk="app/build/outputs/apk/app-${current_build_flavor}-debug.apk"
	relative_path_instrumentation_apk="app/build/outputs/apk/app-${current_build_flavor}-debug-androidTest.apk"
	#install the apks on the target device
	
    $ADB push $DOBBY_PATH/$relavtive_path_debug_apk /data/local/tmp/com.inceptai.dobby.${current_build_flavor}.debug >> /tmp/gradle.log
	if [ $api_level -lt 23 ]; then
		$ADB shell pm install -r "/data/local/tmp/com.inceptai.dobby.${current_build_flavor}.debug" >> /tmp/gradle.log
	else
    	$ADB shell pm install -r -g "/data/local/tmp/com.inceptai.dobby.${current_build_flavor}.debug" >> /tmp/gradle.log
	fi
    $ADB push $DOBBY_PATH/$relative_path_instrumentation_apk /data/local/tmp/com.inceptai.dobby.${current_build_flavor}.debug.test >> /tmp/gradle.log
    $ADB shell pm install -r "/data/local/tmp/com.inceptai.dobby.${current_build_flavor}.debug.test" >> /tmp/gradle.log
}

notify_failure () {
	current_build_flavor=$1
	echo "BUILD FAILED"
	if [ -z $RESULT_URL ]; then
		BODY="Gradle build failed: You can also just run the server like python $DOBBY_SERVER_HOME/server.py and see the results at http://0.0.0.0/index.html"
	else
		BODY="Gradle build failed: See the results $RESULT_URL for UI test results."
	fi
	ATTACHMENT="/tmp/gradle.log"
	emails_to_send=`generate_space_separated_emails $EMAILS_TO_NOTIFY`
	echo "Running echo ${BODY}| mail -s \"Gradle Build Failed for $current_build_flavor\" -A ${ATTACHMENT} $emails_to_send"
	echo ${BODY}| mail -s "Gradle Build Failed for $current_build_flavor" -A ${ATTACHMENT} $emails_to_send
}

notify_success () {
	current_build_flavor=$1
	echo "BUILD SUCCEEDED"
	if [ -z $RESULT_URL ]; then
		BODY="Gradle build succeeded: You can also just run the server like python $DOBBY_SERVER_HOME/server.py and see the results at http://0.0.0.0/index.html"
	else
		BODY="Gradle build succeeded: See the results $RESULT_URL for UI test results."
	fi
	emails_to_send=`generate_space_separated_emails $EMAILS_TO_NOTIFY`
	echo "echo ${BODY}| mail -s \"Gradle Build Success for $current_build_flavor\" $emails_to_send"
	echo ${BODY}| mail -s "Gradle Build Success for $current_build_flavor" $emails_to_send
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
	player_pids=`ps aux | grep -v "automate" | grep -v "spoon" | grep player | tr -s " " | cut -d " " -f 2`
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
		git_status=`check_git`
		echo_ts "New git status is $git_status"
	done
}

run_emulator_tests () {
	current_build_flavor=$1
    echo "List of emulators:"
    cat $EMULATOR_LIST_FILE
    should_report_failure=0


    rm -rf $DOBBY_SERVER_HOME/spoon/${current_build_flavor}
    clean_slate
    num_lines=`wc -l $EMULATOR_LIST_FILE | cut -d ' ' -f1`
	vm_number=0
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
        uninstall_app $current_build_flavor

        #Install the app
        install_app $api_level $current_build_flavor

        #Run the test
		cd $GRADLEW_PATH
		echo $PWD

		#Run unit tests
        UNIT_TEST_TASK=`./gradlew tasks | grep -i test${current_build_flavor}DebugUnitTest | cut -d " " -f 1`
		./gradlew $UNIT_TEST_TASK --stacktrace >>  /tmp/gradle.log
        if [ $? -gt 0 ]; then
            should_report_failure=1
        fi
		
		#Run espresso tests
        SPOON_TASK=`./gradlew tasks | grep -i spoon${current_build_flavor}DebugAndroidTest | cut -d " " -f 1`
		echo "RUNNING gradle task $SPOON_TASK"
		TEST_CLASS=""
		if [ $current_build_flavor = "dobby" ]; then
			TEST_CLASS="com.inceptai.dobby.ui.WifiExpertUITests"
		elif [ $current_build_flavor = "wifidoc" ]; then
			TEST_CLASS="com.inceptai.dobby.ui.CheckMainScreenWifiDocTest"
		else
			echo "UNSUPPORTED BUILD FLAVOR FOR TESTS, FAILING" >> /tmp/gradle.log
            should_report_failure=1
			return
		fi
		echo "./gradlew $SPOON_TASK -PspoonClassName=$TEST_CLASS --stacktrace >>  /tmp/gradle.log"
		#./gradlew $SPOON_TASK -Dspoon.test.class=$TEST_CLASS --stacktrace >>  /tmp/gradle.log
		./gradlew $SPOON_TASK -PspoonClassName=$TEST_CLASS --stacktrace >>  /tmp/gradle.log
        if [ $? -gt 0 ]; then
            should_report_failure=1
        fi

        #Store the output in a diff location
		output_path_spoon_results="$PATH_TO_REPO_DIR/Dobby/app/build/spoon/${current_build_flavor}"
        mkdir -p $DOBBY_SERVER_HOME/spoon/${current_build_flavor}/$api_level
        cp -r $output_path_spoon_results/debug/* $DOBBY_SERVER_HOME/spoon/${current_build_flavor}/$api_level/
        
		if [ ! -z $OUTPUT_DIR_TO_SERVE_FILES ]; then
			echo "$OUTPUT_DIR_TO_SERVE_FILES is specified, so copying results there"
			rm -rf $OUTPUT_DIR_TO_SERVE_FILES/${current_build_flavor}
			mkdir -p $OUTPUT_DIR_TO_SERVE_FILES/${current_build_flavor}/$api_level
        	cp -r $output_path_spoon_results/debug/* $OUTPUT_DIR_TO_SERVE_FILES/${current_build_flavor}/$api_level/
		fi

        #Repeat for different emulators
        uninstall_app $current_build_flavor
        
        #Kill the emulator
        kill $EMULATOR_PID
        sleep 10
        
        clean_slate
		
		#Check if we need to run more
        vm_number=$((vm_number + 1))
		if [ $vm_number -ge $MAX_VMS_TO_RUN ]; then
			echo "Reached max vm limit. Returning"
			return 0
		fi
        echo "Done sleeping, onto the next one"
    done
}


build_test_targets () {
	current_build_flavor=$1
	echo "CHECKED:3 CONFIG: JAVA_HOME:$JAVA_HOME ANDROID_SDK:$ANDROID_HOME" 
    cd $GRADLEW_PATH
    echo "Starting gradle build" > /tmp/gradle.log
    ./gradlew clean build assembleDebug assembleAndroidTest >> /tmp/gradle.log
    if [ $? -gt 0 ]; then
        notify_failure $current_build_flavor 
        return 1
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
		#export $JAVA_HOME
	elif [ ! -z $current_java_home ]; then
		if [ ! -f $current_java_home/bin/java ]; then
			echo "Incorrect Java Home path; $current_java_home/bin/java not found"		
			all_reqs_met=0
		fi
		JAVA_HOME=$current_java_home
		#export $JAVA_HOME
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
		#export $ANDROID_HOME
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

	if [ ! -z $EMULATOR_LIST_FILE ]; then
	    if [ ! -f $EMULATOR_LIST_FILE ]; then
        	echo "Emulator list file not found at $EMULATOR_LIST_FILE"
        	all_reqs_met=0
		fi
    fi

	if [ $all_reqs_met -eq 0 ]; then
		echo "Fix issues with script pre-reqs. And then restart"
		exit 1
	fi

	echo "Done checking config: JAVA_HOME:$JAVA_HOME, ANDROID_HOME:$ANDROID_HOME"
	if [ $CHECK_CONFIG_ONLY -gt 0 ]; then
		echo "Configs are all good. You can run the tests"
		exit 0
	fi
}

run_tests_one_iteration () {
	current_build_flavor=$1
	echo "CHECKED:2 CONFIG: JAVA_HOME:$JAVA_HOME ANDROID_SDK:$ANDROID_HOME" 
    
	cd $PATH_TO_REPO_DIR
	#git changed, do a pull and run tests
    git_pull

    #build the tests
    build_test_targets $current_build_flavor
	
	echo "CHECKED:4 CONFIG: JAVA_HOME:$JAVA_HOME ANDROID_SDK:$ANDROID_HOME" 
    #Check emulator list exists
    if [ -z $EMULATOR_LIST_FILE ]; then
		echo "Generating emulator test file"
		generate_emulator_list_file
    fi

    #run the tests
    run_emulator_tests $current_build_flavor

    if [ $should_report_failure -gt 0 ]; then
        echo "notify_failure"
		notify_failure $current_build_flavor
    else 
		echo "notify_success"
        notify_success $current_build_flavor
    fi
}

export_display () {
	export DISPLAY=:0 
}

run_tests_one_iteration_all_flavors () {
	IFS=',' read -r -a build_flavor_list <<< "$BUILD_FLAVORS"
	for build_flavor in "${build_flavor_list[@]}"
	do
    	echo "Running for $build_flavor"
		run_tests_one_iteration $build_flavor
	done
}



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
EMAILS_TO_NOTIFY=hello@obiai.tech
OUTPUT_DIR_TO_SERVE_FILES=
MAX_VMS_TO_RUN=1
RESULT_URL=
BUILD_FLAVORS="wifidoc,dobby"

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
          -e|--emailstonotify)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                EMAILS_TO_NOTIFY=$2
                shift
            else
                printf 'ERROR: "--emailstonotify|-e" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
         -o|--outputdir)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                OUTPUT_DIR_TO_SERVE_FILES=$2
                shift
            else
                printf 'ERROR: "--outputdir|-o" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
         -n|--numvms)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                MAX_VMS_TO_RUN=$2
                shift
            else
                printf 'ERROR: "--numvms|-n" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
         -u|--resulturl)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                RESULT_URL=$2
                shift
            else
                printf 'ERROR: "--resulturl|-u" requires a non-empty option argument.\n' >&2
                exit 1
            fi
            ;;
          -z|--buildflavor)       # Takes an option argument, ensuring it has been specified.
            if [ -n "$2" ]; then
                BUILD_FLAVORS=$2
                shift
            else
                printf 'ERROR: "--buildflavor|-z" requires a non-empty option argument.\n' >&2
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
MIN_API_LEVEL_TO_USE_G_INSTALL_OPTION=23

#Export display, so it runs well if run from cron or ssh
export_display

#Check config first
check_config

echo "CHECKED:1 CONFIG: JAVA_HOME:$JAVA_HOME ANDROID_SDK:$ANDROID_HOME" 

#Starting tests
#set up the dirs
one_time_setup

#One iteration of tests
run_tests_one_iteration_all_flavors
#run_tests_one_iteration $current_build_flavor

while [ $REPEAT_TESTS -gt 0 ]; do 
    #Wait for more git changes
    wait_for_git_changes
	
	#Run another iteration
	run_tests_one_iteration_all_flavors
	#run_tests_one_iteration $current_build_flavor
done 

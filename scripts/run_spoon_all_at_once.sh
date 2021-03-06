if [ $# -ne 2 ]; then
    echo "Usage: $0 dobby-android_GIT_PATH emulator_id_list"
    exit
fi

PATH_TO_BASE_DIR=$1
EMULATOR_LIST_FILE=$2
ARUNESH_EMAIL="arunesh@obiai.tech"
VIVEK_EMAIL="vivek@obiai.tech"
BODY="Test Email"
SUBJECT="Gradle Build"
REL_PATH_DEBUG_APK="app/build/outputs/apk/app-wifidoc-debug.apk"
REL_PATH_INSTRUMENTATION_APK="app/build/outputs/apk/app-wifidoc-debug-androidTest.apk"


uninstall_app () {
    #uninstall the apks from the device
    adb shell pm uninstall com.inceptai.dobby.wifidoc.debug >> /tmp/gradle.log
    adb shell pm uninstall com.inceptai.dobby.wifidoc.debug.test >> /tmp/gradle.log
}

install_app () {
    #install the apks on the target device
    adb push $REL_PATH_DEBUG_APK /data/local/tmp/com.inceptai.dobby.wifidoc.debug >> /tmp/gradle.log
    adb shell pm install -r -g  "/data/local/tmp/com.inceptai.dobby.wifidoc.debug" >> /tmp/gradle.log
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

cd $PATH_TO_BASE_DIR
cd Dobby


echo "Starting gradle build" > /tmp/gradle.log
./gradlew clean build assembleAndroidTest >> /tmp/gradle.log


if [ $? -gt 0 ]; then
    notify_failure
    exit 1
fi

#Check emulator list exists
if [ ! -f $EMULATOR_LIST_FILE ]; then
	echo "$EMULATOR_LIST_FILE does not exist. Exiting"
	exit 0
fi

echo "List of emulators:"
cat $EMULATOR_LIST_FILE
should_report_failure=0

clean_slate
num_lines=`wc -l $EMULATOR_LIST_FILE | cut -d ' ' -f1`
for i in $(seq 1 $num_lines); do
   	line=`sed -n ${i}p $EMULATOR_LIST_FILE`
    emulator_id="$line"
    if [ -z "$emulator_id" ]; then
        continue
    fi
    is_commented=`echo $emulator_id | grep "#"`
    if [ ! -z "$is_commented" ]; then
        echo "Emulator $emulator_id"
        continue
    fi
    echo "Running test for id $emulator_id"

	#Start the emulator
	$GENYMOTION_HOME/player --vm-name $emulator_id &	
	EMULATOR_PID=$!
	echo "Started emulator with pid:" $EMULATOR_PID

	#Slight delay before we check
	sleep 5

	#Wait for it to start
	echo "Waiting for emulator:" $emulator_id
	wait_for_emulator

	#Sleep for a few secs	
	sleep 15

	#Uninstall the app
	echo "Uninstalling app on emulator:" $emulator_id
	uninstall_app

	#Install the app
	echo "Installing app on emulator:" $emulator_id
	install_app

	#Kill the emulator
	echo "Killing emulator:" $emulator_id
	kill $EMULATOR_PID
	sleep 10
	
	clean_slate
	echo "Done sleeping, onto the next one"
done

clean_slate
num_lines=`wc -l $EMULATOR_LIST_FILE | cut -d ' ' -f1`
for i in $(seq 1 $num_lines); do
   	line=`sed -n ${i}p $EMULATOR_LIST_FILE`
    emulator_id="$line"
    if [ -z "$emulator_id" ]; then
        continue
    fi
    is_commented=`echo $emulator_id | grep "#"`
    if [ ! -z "$is_commented" ]; then
        echo "Emulator $emulator_id"
        continue
    fi
    echo "Running test for id $emulator_id"

	#Start the emulator
	$GENYMOTION_HOME/player --vm-name $emulator_id &	
	EMULATOR_PID=$!
	echo "Started emulator with pid:" $EMULATOR_PID

	#Slight delay before we check
	sleep 15
	echo "Done sleeping, onto the next one"
done


#Run the test
./gradlew spoonWifidocDebugAndroidTest --stacktrace >>  /tmp/gradle.log
if [ $? -gt 0 ]; then
	should_report_failure=1
fi

clean_slate
num_lines=`wc -l $EMULATOR_LIST_FILE | cut -d ' ' -f1`
for i in $(seq 1 $num_lines); do
   	line=`sed -n ${i}p $EMULATOR_LIST_FILE`
    emulator_id="$line"
    if [ -z "$emulator_id" ]; then
        continue
    fi
    is_commented=`echo $emulator_id | grep "#"`
    if [ ! -z "$is_commented" ]; then
        echo "Emulator $emulator_id"
        continue
    fi
    echo "Running test for id $emulator_id"

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
	echo "Uninstalling app on emulator:" $emulator_id
	uninstall_app

	#Kill the emulator
	kill $EMULATOR_PID
	sleep 10
	
	clean_slate
	echo "Done sleeping, onto the next one"
done

if [ $should_report_failure -gt 0 ]; then
	notify_failure
	exit 1
else 
	notify_success
	exit 0
fi

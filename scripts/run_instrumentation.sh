if [ $# -ne 1 ]; then
    echo "Usage: $0 dobby-android_GIT_PATH"
    exit
fi

PATH_TO_BASE_DIR=$1
ARUNESH_EMAIL="arunesh@obiai.tech"
VIVEK_EMAIL="vivek@obiai.tech"
BODY="Test Email"
SUBJECT="Gradle Build"
REL_PATH_DEBUG_APK="app/build/outputs/apk/app-wifidoc-debug.apk"
REL_PATH_INSTRUMENTATION_APK="app/build/outputs/apk/app-wifidoc-debug-androidTest.apk"

cd $PATH_TO_BASE_DIR
cd Dobby

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

echo "Starting gradle build" > /tmp/gradle.log
./gradlew clean build assembleAndroidTest >> /tmp/gradle.log


if [ $? -gt 0 ]; then
    notify_failure
    exit 1
fi

#Uninstall the app
uninstall_app

#Install the app
install_app

./gradlew spoonWifidocDebugAndroidTest >>  /tmp/gradle.log

if [ $? -gt 0 ]; then
    notify_failure
    exit 1
else
    uninstall_app
    notify_success
    exit 0
fi

exit 0

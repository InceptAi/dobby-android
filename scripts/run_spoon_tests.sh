ARUNESH_EMAIL="arunesh@obiai.tech"
VIVEK_EMAIL="vivek@obiai.tech"
BODY="Test Email"
SUBJECT="Gradle Build"

cd ../Dobby
echo "Starting gradle test" > /tmp/gradle.log

./gradlew clean build spoonWifidocDebugAndroidTest >>  /tmp/gradle.log

if [ $? -gt 0 ]; then
	echo "BUILD FAILED"
	SUBJECT="Gradle Build Failed for WifiDoc"
	BODY="Gradle build failed: See http://dobby1743.duckdns.org:5187/ for UI test results"
	ATTACHMENT="/tmp/gradle.log"
	echo ${BODY}| mail -s ${SUBJECT} -a ${ATTACHMENT} $VIVEK_EMAIL $ARUNESH_EMAIL
else
	echo "BUILD SUCCEEDED"
	SUBJECT="Gradle Build Success for WifiDoc"
	BODY="Gradle build succeeded: See http://dobby1743.duckdns.org:5187/ for UI screenshots"
	echo ${BODY}| mail -s ${SUBJECT} $VIVEK_EMAIL $ARUNESH_EMAIL
fi

exit 0

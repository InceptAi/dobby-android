cd ../Dobby
echo "Starting gradle test" > gradle.log
./gradlew clean build spoonWifidocDebugAndroidTest
if [ $? -eq 0 ]; then
	echo "SUCCEEDED"
	exit 0
fi
#Test failed, send an email
#TO_ADDRESS="vivek@obiai.tech"
#FROM_ADDRESS="vivek@obiai.tech"
#SUBJECT="Gradle build failed"
#BODY="Test"
#
#echo ${BODY}| mail -s ${SUBJECT} ${TO_ADDRESS} -- -r ${FROM_ADDRESS}

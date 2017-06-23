const functions = require('firebase-functions');

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });
//
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);


exports.sendNotification = functions.database.ref('/notifications/messages/{pushId}')
  .onWrite(event => {

    const message = event.data.current.val();
    if (message == null) {
        console.log('null message received.');
        return;
    }
    console.log('message ' + message);
    console.log('message.from: ' + message.from);
    console.log('message.to: ' + message.to);
    console.log('message.title: ' + message.title);
    console.log('message.body: ' + message.body);
    console.log('message.fcmIdPath: ' + message.fcmIdPath);

    const senderUid = message.from;
    const receiverUid = message.to;
    const fcmIdPath = message.fcmIdPath;
    const promises = [];

    if (senderUid == receiverUid) {
        // if sender is receiver, don't send notification
        promises.push(event.data.current.ref.remove());
        return Promise.all(promises);
    }

    const getInstanceIdPromise = admin.database().ref(`${fcmIdPath}`).once('value');
    return Promise.all([getInstanceIdPromise]).then(results => {
        const instanceId = results[0].val();
	    console.log('Got fcm id: ' + instanceId);
        console.log('notifying ' + receiverUid + ' about ' + message.body + ' from ' + senderUid);

        const payload = {
            data: {
                titleText: message.title,
                bodyText: message.body,
                source: message.from,
                destination: message.to,
                messagePushId: event.params.pushId,
	        fcmIdPathForRecipient: message.fcmIdPath
            }
        };

        admin.messaging().sendToDevice(instanceId, payload)
            .then(function (response) {
                console.log("Successfully sent message:", response);
                event.data.current.ref.remove();
            })
        .catch(function (error) {
            console.log("Error sending message:", error);
        });
    });
});

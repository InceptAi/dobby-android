'use strict';

// Initializes ExpertChat.
function ExpertChat() {
  this.checkSetup();

  // Shortcuts to DOM Elements.
  this.messageList = document.getElementById('messages');
  this.messageForm = document.getElementById('message-form');
  this.messageInput = document.getElementById('message');
  this.submitButton = document.getElementById('submit');
  this.submitImageButton = document.getElementById('submitImage');
  this.imageForm = document.getElementById('image-form');
  this.mediaCapture = document.getElementById('mediaCapture');
  this.userPic = document.getElementById('user-pic');
  this.userName = document.getElementById('user-name');
  this.signInButton = document.getElementById('sign-in');
  this.signOutButton = document.getElementById('sign-out');
  this.signInSnackbar = document.getElementById('must-signin-snackbar');
  this.welcomeTitle = document.getElementById('welcome-text-title');
  this.supportingTextbar = document.getElementById('supporting-text-div');

  // Saves message on form submit.
  this.messageForm.addEventListener('submit', this.saveMessage.bind(this));
  this.signOutButton.addEventListener('click', this.signOut.bind(this));
  this.signInButton.addEventListener('click', this.signIn.bind(this));

  // Toggle for the button.
  var buttonTogglingHandler = this.toggleButton.bind(this);
  this.messageInput.addEventListener('keyup', buttonTogglingHandler);
  this.messageInput.addEventListener('change', buttonTogglingHandler);

  // Events for image upload.
  this.submitImageButton.addEventListener('click', function(e) {
    e.preventDefault();
    this.mediaCapture.click();
  }.bind(this));
  this.mediaCapture.addEventListener('change', this.saveImageMessage.bind(this));

  this.initFirebase();
}

ExpertChat.prototype.loadExpertList = function(userName) {
  this.expertListRef = this.database.ref('/expert/');
  this.expertList = [];
  this.expertListRef.once('value').then(snapshot => {
      for (var key in snapshot.val()) {
          console.log("Expert: " + key);
          this.getExpert(key, userName);
      }
  });
}

ExpertChat.prototype.getExpert = function(avatarName, userName) {
    var tempRef = this.database.ref('/expert/' + avatarName + '/');
    tempRef.once('value').then(expert => {
        if (expert.val().name == userName) {
            console.log("Found expert:" + expert.val().name);
            console.log("Load chat for userUuid:" + expert.val().selectedUuid);
            this.loadMessages(expert.val().selectedUuid);
            this.selectedUuid = expert.val().selectedUuid;
            this.expertAvatarName = expert.val().avatar;
            this.welcomeTitle.textContent = "Welcome " + expert.val().avatar;
            this.supportingTextbar.textContent = "You are chatting with USER: " + this.selectedUuid;
        }
        console.log("gotExpert.name:" + expert.val().name);
        console.log("gotExpert.avatar:" + expert.val().avatar);
        console.log("looking for userName:" + userName);
    });
}

// Sets up shortcuts to Firebase features and initiate firebase auth.
ExpertChat.prototype.initFirebase = function() {
  // Shortcuts to Firebase SDK features.
  this.auth = firebase.auth();
  this.database = firebase.database();
  this.storage = firebase.storage();
  // Initiates Firebase auth and listen to auth state changes.
  this.auth.onAuthStateChanged(this.onAuthStateChanged.bind(this));
};

// Loads chat messages history and listens for upcoming ones.
ExpertChat.prototype.loadMessages = function(userUuid) {
  // Reference to the /messages/ database path.
  var messagePath = '/wifidoc_chat_rooms/release/' + userUuid +'/';
  var fcmIdPath = '/wifidoc/release/users/' + userUuid + '/' + 'fcmToken';
  if (window.debugMode == true) {
	  if (window.isWifiExpert == true) {
		  messagePath = '/dobby_chat_rooms/debug/' + userUuid +'/';
          fcmIdPath = '/dobby/debug/users/' + userUuid + '/' + 'fcmToken';
	  } else {
		  messagePath = '/wifidoc_chat_rooms/debug/' + userUuid +'/';
          fcmIdPath = '/wifidoc/debug/users/' + userUuid + '/' + 'fcmToken';
	  }
  } else if (window.isWifiExpert == true) {
	  messagePath = '/dobby_chat_rooms/release/' + userUuid +'/';
      fcmIdPath = '/dobby/release/users/' + userUuid + '/' + 'fcmToken';
  }
  console.log("Message path: " + messagePath);
  this.messagesRef = this.database.ref(messagePath);
  this.fcmIdPath = fcmIdPath;
  this.notifRef = this.database.ref('/notifications/messages/');

  // Make sure we remove all previous listeners.
  this.messagesRef.off();

  // Loads the last 12 messages and listen for new ones.
  var setMessage = function(data) {
    var val = data.val();
    if (val.messageType == 5001) {
	    val.text = "META: User left chat.";
	    val.name = val.timestamp;
    } else if (val.messageType == 5002) {
	    val.text = "META: User entered chat.";
	    val.name = val.timestamp;
    }  else if (val.messageType == 5003) {
	    val.text = "META: Action started.";
	    val.name = val.timestamp;
    }  else if (val.messageType == 5004)  {
	    val.text = "META: Action completed.";
	    val.name = val.timestamp;
    }  else if (val.messageType == 6001)  {
	    val.text = "META: Neo Service Ready.";
	    val.name = val.timestamp;
    }  else if (val.messageType == 6002)  {
	    val.text = "META: Neo Event Streaming Stopped by User.";
	    val.name = val.timestamp;
    }  else if (val.messageType == 6003)  {
	    val.text = "META: Neo Event Streaming Stopped by Expert.";
	    val.name = val.timestamp;
    }  else if (val.messageType == 6005)  {
	    val.text = "META: Neo Service Overlay Permission Denied.";
	    val.name = val.timestamp;
    }  else if (val.messageType == 6006)  {
	    val.text = "META: Neo Service Overlay Permission Granted.";
	    val.name = val.timestamp;
    }  else if (val.messageType == 6007)  {
	    val.text = "META: Neo Accessibility Permission Not Granted within Timeout.";
	    val.name = val.timestamp;
    }  
    this.displayMessage(data.key, val.name, val.text, val.photoUrl, val.imageUrl);
  }.bind(this);
  this.messagesRef.limitToLast(12).on('child_added', setMessage);
  this.messagesRef.limitToLast(12).on('child_changed', setMessage);
};

// Saves a new message on the Firebase DB.
ExpertChat.prototype.saveMessage = function(e) {
  e.preventDefault();
  // Check that the user entered a message and is signed in.
  if (this.messageInput.value && this.checkSignedInWithMessage()) {
    var currentUser = this.auth.currentUser;
    var messageText = this.messageInput.value;
    // Add a new message entry to the Firebase Database.
    this.messagesRef.push({
      name: currentUser.displayName,
      messageType: 1001,
      text: this.messageInput.value,
      utcTimestampMs: new Date().getTime() + 1728000000,
      photoUrl: currentUser.photoURL || '/images/profile_placeholder.png'
    }).then(function() {
      // Clear message text field and SEND button state.
      ExpertChat.resetMaterialTextfield(this.messageInput);
      this.toggleButton();
	  if (!(messageText.substring(0,1) == "#")) {
      	this.notifRef.push({
          	from: this.expertAvatarName,
          	to: this.selectedUuid,
          	title: 'You have a new message from a Wifi Expert',
          	body: messageText, 
          	fcmIdPath: this.fcmIdPath
      	});
	  } else {
      	console.log('Dropping notifications for command text ', messageText);
	  }
    }.bind(this)).catch(function(error) {
      console.error('Error writing new message to Firebase Database', error);
    });
  }
};

// Sets the URL of the given img element with the URL of the image stored in Cloud Storage.
ExpertChat.prototype.setImageUrl = function(imageUri, imgElement) {
  // If the image is a Cloud Storage URI we fetch the URL.
  if (imageUri.startsWith('gs://')) {
    imgElement.src = ExpertChat.LOADING_IMAGE_URL; // Display a loading image first.
    this.storage.refFromURL(imageUri).getMetadata().then(function(metadata) {
      imgElement.src = metadata.downloadURLs[0];
    });
  } else {
    imgElement.src = imageUri;
  }
};

// Saves a new message containing an image URI in Firebase.
// This first saves the image in Firebase storage.
ExpertChat.prototype.saveImageMessage = function(event) {
  event.preventDefault();
  var file = event.target.files[0];

  // Clear the selection in the file picker input.
  this.imageForm.reset();

  // Check if the file is an image.
  if (!file.type.match('image.*')) {
    var data = {
      message: 'You can only share images',
      timeout: 2000
    };
    this.signInSnackbar.MaterialSnackbar.showSnackbar(data);
    return;
  }

  // Check if the user is signed-in
  if (this.checkSignedInWithMessage()) {

    // We add a message with a loading icon that will get updated with the shared image.
    var currentUser = this.auth.currentUser;
    this.messagesRef.push({
      name: currentUser.displayName,
      imageUrl: ExpertChat.LOADING_IMAGE_URL,
      photoUrl: currentUser.photoURL || '/images/profile_placeholder.png'
    }).then(function(data) {

      // Upload the image to Cloud Storage.
      var filePath = currentUser.uid + '/' + data.key + '/' + file.name;
      return this.storage.ref(filePath).put(file).then(function(snapshot) {

        // Get the file's Storage URI and update the chat message placeholder.
        var fullPath = snapshot.metadata.fullPath;
        return data.update({imageUrl: this.storage.ref(fullPath).toString()});
      }.bind(this));
    }.bind(this)).catch(function(error) {
      console.error('There was an error uploading a file to Cloud Storage:', error);
    });
  }
};

// Signs-in Expert Chat.
ExpertChat.prototype.signIn = function() {
  // Sign in Firebase using popup auth and Google as the identity provider.
  var provider = new firebase.auth.GoogleAuthProvider();
  this.auth.signInWithPopup(provider);
};

// Signs-out of Expert Chat.
ExpertChat.prototype.signOut = function() {
  // Sign out of Firebase.
  this.auth.signOut();
};

// Triggers when the auth state change for instance when the user signs-in or signs-out.
ExpertChat.prototype.onAuthStateChanged = function(user) {
  if (user) { // User is signed in!
    // Get profile pic and user's name from the Firebase user object.
    var profilePicUrl = user.photoURL;
    var userName = user.displayName;

    // Set the user's profile pic and name.
    this.userPic.style.backgroundImage = 'url(' + (profilePicUrl || '/images/profile_placeholder.png') + ')';
    this.userName.textContent = userName;

    // Show user's profile and sign-out button.
    this.userName.removeAttribute('hidden');
    this.userPic.removeAttribute('hidden');
    this.signOutButton.removeAttribute('hidden');

    // Hide sign-in button.
    this.signInButton.setAttribute('hidden', 'true');

    this.loadExpertList(userName);

    // We save the Firebase Messaging Device token and enable notifications.
    this.saveMessagingDeviceToken();
  } else { // User is signed out!
    // Hide user's profile and sign-out button.
    this.userName.setAttribute('hidden', 'true');
    this.userPic.setAttribute('hidden', 'true');
    this.signOutButton.setAttribute('hidden', 'true');

    // Show sign-in button.
    this.signInButton.removeAttribute('hidden');
  }
};

// Returns true if user is signed-in. Otherwise false and displays a message.
ExpertChat.prototype.checkSignedInWithMessage = function() {
  // Return true if the user is signed in Firebase
  if (this.auth.currentUser) {
    return true;
  }

  // Display a message to the user using a Toast.
  var data = {
    message: 'You must sign-in first',
    timeout: 2000
  };
  this.signInSnackbar.MaterialSnackbar.showSnackbar(data);
  return false;
};

// Saves the messaging device token to the datastore.
ExpertChat.prototype.saveMessagingDeviceToken = function() {
  firebase.messaging().getToken().then(function(currentToken) {
    if (currentToken) {
      console.log('Got FCM device token:', currentToken);
      // Saving the Device Token to the datastore.
      firebase.database().ref('/fcmTokens').child(currentToken)
          .set(firebase.auth().currentUser.uid);
    } else {
      // Need to request permissions to show notifications.
      this.requestNotificationsPermissions();
    }
  }.bind(this)).catch(function(error){
    console.error('Unable to get messaging token.', error);
  });
};

// Requests permissions to show notifications.
ExpertChat.prototype.requestNotificationsPermissions = function() {
  console.log('Requesting notifications permission...');
  firebase.messaging().requestPermission().then(function() {
    // Notification permission granted.
    this.saveMessagingDeviceToken();
  }.bind(this)).catch(function(error) {
    console.error('Unable to get permission to notify.', error);
  });
};

// Resets the given MaterialTextField.
ExpertChat.resetMaterialTextfield = function(element) {
  element.value = '';
  element.parentNode.MaterialTextfield.boundUpdateClassesHandler();
};

// Template for messages.
ExpertChat.MESSAGE_TEMPLATE =
    '<div class="message-container">' +
      '<div class="spacing"><div class="pic"></div></div>' +
      '<div class="message"></div>' +
      '<div class="name"></div>' +
    '</div>';

// A loading image URL.
ExpertChat.LOADING_IMAGE_URL = 'https://www.google.com/images/spin-32.gif';

// Displays a Message in the UI.
ExpertChat.prototype.displayMessage = function(key, name, text, picUrl, imageUri) {
  var div = document.getElementById(key);
  // If an element for that message does not exists yet we create it.
  if (!div) {
    var container = document.createElement('div');
    container.innerHTML = ExpertChat.MESSAGE_TEMPLATE;
    div = container.firstChild;
    div.setAttribute('id', key);
    this.messageList.appendChild(div);
  }
  if (picUrl) {
    div.querySelector('.pic').style.backgroundImage = 'url(' + picUrl + ')';
  }
  div.querySelector('.name').textContent = name;
  var messageElement = div.querySelector('.message');
  if (text) { // If the message is text.
    messageElement.textContent = text;
    // Replace all line breaks by <br>.
    messageElement.innerHTML = messageElement.innerHTML.replace(/\n/g, '<br>');
  } else if (imageUri) { // If the message is an image.
    var image = document.createElement('img');
    image.addEventListener('load', function() {
      this.messageList.scrollTop = this.messageList.scrollHeight;
    }.bind(this));
    this.setImageUrl(imageUri, image);
    messageElement.innerHTML = '';
    messageElement.appendChild(image);
  }
  // Show the card fading-in and scroll to view the new message.
  setTimeout(function() {div.classList.add('visible')}, 1);
  this.messageList.scrollTop = this.messageList.scrollHeight;
  this.messageInput.focus();
};

// Enables or disables the submit button depending on the values of the input
// fields.
ExpertChat.prototype.toggleButton = function() {
  if (this.messageInput.value) {
    this.submitButton.removeAttribute('disabled');
  } else {
    this.submitButton.setAttribute('disabled', 'true');
  }
};

// Checks that the Firebase SDK has been correctly setup and configured.
ExpertChat.prototype.checkSetup = function() {
  if (!window.firebase || !(firebase.app instanceof Function) || !firebase.app().options) {
    window.alert('You have not configured and imported the Firebase SDK. ' +
        'Make sure you go through the codelab setup instructions and make ' +
        'sure you are running the codelab using `firebase serve`');
  }
};

window.onload = function() {
  window.expertChat = new ExpertChat();
};

# EmotionalControl is a parental control system that  
1. captures Android device screen periodically  
2. sends screenshots to a server for analysis  
3. detects inappropriate content (text/images)  
4. sends notifications to parents via push notifications

# Android App  
1. Background screen monitoring every 3 seconds
2. Screenshot upload to a remote server
3. All required permissions requested on first launch
4. Runs as a background service
5. Option to start/stop monitoring manually

# Requirements  
1. Android 10+ (API 29+)
2. Permissions: FOREGROUND_SERVICE, SYSTEM_ALERT_WINDOW, INTERNET, WRITE_EXTERNAL_STORAGE (if needed), MEDIA_PROJECTION (requested via intent)  
3. Fully supports Android 10–15

# Server Side  
The server can be placed anywhere you want – VPS, local machine, or cloud – as long as Python 3.10+ is available.  

## Setup & Run  
1. [Download](https://www.python.org/downloads/) Python 3.11+  
2. Navigate to the server directory: `cd server`  
3. Install dependencies manually:  `pip install flask flask_cors requests numpy tensorflow sentence-transformers easyocr google-auth`  
4. [Download text model](https://drive.google.com/file/d/1vAOGjnRNflSSvwNEAD-oYNpOhQBO_IgR/view?usp=drive_link) and upload it to the server folder.  
5. Start the Flask server: `python server.py`  


## Features  
1. Accepts screenshots from Android app
2. Uses EasyOCR to extract text
3. Classifies text using `distiluse-base-multilingual-cased-v1` model
4. Sends push notifications via Firebase Cloud Messaging (FCM)

### Firebase Integration  
Push notifications are sent using Firebase Cloud Messaging (FCM):
1. Create a project at [Firebase Console](https://console.firebase.google.com)  
2. Generate and download `serviceAccountKey.json`
3. Rename it to `firebase_key.json`
4. Place it into the `server/` folder

# Android Client Setup  
The Android client is responsible for capturing the screen, sending screenshots to the backend, and displaying notifications when inappropriate content is detected.  

## Setup Instructions  
1. Clone the repository and open it in Android Studio: `git clone https://github.com/MKroppp/Control.git`
2. Open the project directory: `File` → `Open` → Navigate to the root of the project
3. Important:
   You may need to update the backend server URL in the `MainActivity.java` file to point to your running Flask server address.
   Typically, look for a variable like:
   `private static final String SERVER_URL = "http://<your-server-ip>:5000/upload";`
   Replace `<your-server-ip>` with the actual IP or hostname where the backend is running.
5. Connect your device or start an emulator.
6. Build & run the app (`Shift + F10` or green "Play" button).

## How It Works  
1. On app start, it asks for permission to capture the screen.
2. Once granted, a foreground service starts and captures the screen every 3 seconds.
3. Screenshots are sent via HTTP POST to the backend Flask server (`http://<your-ip>:5000/upload`).
4. The server processes the screenshot (OCR + ML) and sends a Firebase push notification if inappropriate content is detected.
5. The app receives and shows the notification.




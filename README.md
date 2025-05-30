# EmotionalControl is a parental control system that:  
1. captures Android device screen periodically  
2. sends screenshots to a server for analysis  
3. detects inappropriate content (text/images)  
4. sends notifications to parents via push notifications

# Android App:  
1. Background screen monitoring every 3 seconds
2. Screenshot upload to a remote server
3. All required permissions requested on first launch
4. Runs as a background service
5. Option to start/stop monitoring manually

# Requirements:  
1. Android 10+ (API 29+)
2. Permissions: FOREGROUND_SERVICE, SYSTEM_ALERT_WINDOW, INTERNET, WRITE_EXTERNAL_STORAGE (if needed), MEDIA_PROJECTION (requested via intent)
3. Fully supports Android 10–15

# Server Side  
The server can be placed anywhere you want – VPS, local machine, or cloud – as long as Python 3.10+ is available.  

## Features:  
1. Accepts screenshots from Android app
2. Uses EasyOCR to extract text
3. Classifies text using `distiluse-base-multilingual-cased-v1` model
4. Sends push notifications via Firebase Cloud Messaging (FCM)

[Download text model](https://drive.google.com/file/d/1vAOGjnRNflSSvwNEAD-oYNpOhQBO_IgR/view?usp=drive_link) and upload it to the server folder.  


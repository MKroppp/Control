from google.oauth2 import service_account
from google.auth.transport.requests import AuthorizedSession
import json

SERVICE_ACCOUNT_FILE = r'C:\Users\fuuup\OneDrive\Рабочий стол\Diplom\server\emotionalcontrol-36961d7b708d.json'

PROJECT_ID = 'emotionalcontrol'

# Создаем авторизованную сессию
SCOPES = ['https://www.googleapis.com/auth/firebase.messaging']

credentials = service_account.Credentials.from_service_account_file(
    SERVICE_ACCOUNT_FILE, scopes=SCOPES)

authed_session = AuthorizedSession(credentials)

def send_fcm_notification(token, title, body):
    url = f'https://fcm.googleapis.com/v1/projects/{PROJECT_ID}/messages:send'

    message = {
        "message": {
            "token": token,
            "notification": {
                "title": title,
                "body": body
            }
        }
    }

    response = authed_session.post(url, json=message)

    if response.status_code == 200:
        print(f"[FCM] Уведомление отправлено успешно: {response.text}")
    else:
        print(f"[FCM] Ошибка отправки: {response.status_code} {response.text}")

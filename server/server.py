from flask import Flask, request, jsonify
from PIL import Image, ImageEnhance, ImageOps
import os
import re
import numpy as np
import tensorflow as tf
from sentence_transformers import SentenceTransformer, util
import easyocr
from send_fcm import send_fcm_notification
import signal
import sys

# Flask app
app = Flask(__name__)

# Глобальные счётчики
total_screenshots = 0
unsafe_screenshots = 0
safe_screenshots = 0

# Пути
UPLOAD_FOLDER = 'uploads'
TFLITE_MODEL_PATH = 'model/2.tflite'
TEXT_MODEL_PATH = 'text_model'  # distiluse-base-multilingual-cased-v1

# Папка для скринов
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# TensorFlow Lite модель
interpreter = tf.lite.Interpreter(model_path=TFLITE_MODEL_PATH)
interpreter.allocate_tensors()
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()
input_shape = input_details[0]['shape']

# OCR
reader = easyocr.Reader(['en', 'ru'], gpu=False)

# Модель текста
text_model = SentenceTransformer(TEXT_MODEL_PATH)

# Примеры для семантической классификации
CATEGORIES = [
    ("оскорбление", "Ты отвратителен, и с тобой никто не хочет общаться."),
    ("нормально", "Сегодня хорошая погода, и я собираюсь погулять."),
    ("спам", "Позвони по номеру и получи приз прямо сейчас!"),
]
category_embeddings = [(label, text_model.encode(example, convert_to_tensor=True)) for label, example in CATEGORIES]

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'image' not in request.files:
        return 'No image file in request', 400

    file = request.files['image']
    if file.filename == '':
        return 'No selected file', 400

    image = Image.open(file.stream).convert('RGB')
    save_path = os.path.join(UPLOAD_FOLDER, file.filename)
    image.save(save_path)

    # Обработка
    tflite_result = process_image(image)
    extracted_text = extract_text(image)
    text_result = classify_text_semantic(extracted_text) if extracted_text else "Нет текста на изображении"

    alert = False
    if tflite_result['class'] not in [916, 527]: # Безопасный класс
        alert = True
    if isinstance(text_result, dict) and text_result.get("label") in ["оскорбление", "спам"]:
        alert = True
    if alert:
        try:
            with open("tokens.txt", "r") as f:
                tokens = [line.strip() for line in f if line.strip()]
            for token in tokens:
                send_fcm_notification(token, "Внимание!", "Обнаружен нежелательный контент")
        except FileNotFoundError:
            print("tokens.txt не найден.")

    print(f"[INFO] Сохранено изображение: {save_path}")
    print(f"[INFO] Результат TFLite модели: {tflite_result}")
    print(f"[INFO] Извлечённый текст: {extracted_text}")
    print(f"[INFO] Результат классификации текста: {text_result}")

    global total_screenshots, unsafe_screenshots, safe_screenshots
    total_screenshots += 1
    if alert:
        unsafe_screenshots += 1
    else:
        safe_screenshots += 1

    return {
        'tflite_result': tflite_result,
        'extracted_text': extracted_text,
        'text_result': text_result
    }, 200

@app.route('/register_token', methods=['POST'])
def register_token():
    data = request.get_json()
    print(f"[DEBUG] Получен запрос: {data}")
    token = data.get('token')
    
    if not token:
        return jsonify({'error': 'No token provided'}), 400

    print(f"Received FCM token: {token}")
    
    with open("tokens.txt", "a") as f:
        f.write(token + "\n")

    return jsonify({'status': 'success'}), 200

def preprocess_image(image):
    image = ImageOps.autocontrast(image)
    enhancer = ImageEnhance.Sharpness(image)
    image = enhancer.enhance(2.0)
    return image

def process_image(image):
    img_resized = image.resize((input_shape[1], input_shape[2]))
    img_array = np.array(img_resized, dtype=np.float32) / 255.0
    img_array = np.expand_dims(img_array, axis=0)

    interpreter.set_tensor(input_details[0]['index'], img_array)
    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details[0]['index'])

    predicted_class = int(np.argmax(output_data))
    confidence = float(np.max(output_data))

    return {'class': predicted_class, 'confidence': confidence}

def extract_text(image):
    preprocessed = preprocess_image(image)
    results = reader.readtext(np.array(preprocessed), detail=0)
    full_text = ' '.join(results).strip()
    return clean_ocr_text(full_text)

def classify_text_semantic(text, threshold=0.2):
    if len(text.split()) < 2:
        return {'label': 'неопределено', 'similarity': 0.0}

    text_embedding = text_model.encode(text, convert_to_tensor=True)
    scores = [(label, float(util.pytorch_cos_sim(text_embedding, emb)[0][0])) for label, emb in category_embeddings]
    best_label, best_score = max(scores, key=lambda x: x[1])

    if best_score < threshold:
        return {'label': 'неопределено', 'similarity': best_score}

    return {'label': best_label, 'similarity': best_score}

def clean_ocr_text(text):
    text = re.sub(r'[^\w\s.,!?А-Яа-яЁёA-Za-z]', '', text)
    return re.sub(r'\s+', ' ', text).strip()

def shutdown_handler(signal_received, frame):
    print("\n[STATS]")
    print(f"Screenshots: {total_screenshots}")
    print(f"Unsafe: {unsafe_screenshots}")
    print(f"Safe: {safe_screenshots}")
    sys.exit(0)

signal.signal(signal.SIGINT, shutdown_handler)   # Обработка Ctrl+C
signal.signal(signal.SIGTERM, shutdown_handler)  # Обработка kill

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)


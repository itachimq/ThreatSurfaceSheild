# 🛡️ AI-Powered Secure Browser for Real-Time Phishing Detection

An Android-based secure browser that leverages **Deep Learning** to detect phishing websites in real time. The application integrates a **Convolutional Neural Network (CNN)** model trained on phishing and legitimate URL datasets and deployed using **TensorFlow Lite** for fast, offline inference.

---

## 📖 Overview

Phishing attacks are among the most common cybersecurity threats, where attackers create fake websites to steal sensitive user information. This project provides a secure browsing solution by analyzing URLs before loading web pages and alerting users if a phishing attempt is detected.

The system performs on-device inference using TensorFlow Lite, ensuring low latency, improved privacy, and offline functionality.

---

## ✨ Features

- 🔍 Real-time phishing URL detection
- 🤖 CNN-based deep learning model
- 📱 TensorFlow Lite integration for Android
- ⚡ Fast on-device inference
- 🚫 Warning and blocking mechanism for phishing websites
- 🔒 Offline and privacy-focused detection
- 🌐 Secure browsing using Android WebView

---

## 🏗️ System Architecture

```text
User Input URL
      │
      ▼
URL Preprocessing
      │
      ▼
Character Tokenization
      │
      ▼
CNN Model (TensorFlow Lite)
      │
      ▼
Risk Score Generation
      │
      ▼
Allow / Warn / Block
      │
      ▼
Secure Browser Response
```

---

## 🛠️ Tech Stack

| Category | Technologies |
|----------|--------------|
| Programming Language | Python, Kotlin |
| Machine Learning | TensorFlow, Keras |
| Mobile AI | TensorFlow Lite |
| IDE | Android Studio |
| Deep Learning | Convolutional Neural Network (CNN) |
| Platform | Android |

---

## 📂 Project Structure

```text
AI-Phishing-Browser/
│
├── app/
├── model/
├── dataset/
├── screenshots/
├── README.md
└── requirements.txt
```

---

## 🚀 Installation

### Clone the repository

```bash
git clone https://github.com/<your-username>/AI-Phishing-Browser.git
```

### Open in Android Studio

1. Open Android Studio.
2. Select **Open an Existing Project**.
3. Choose the cloned project.
4. Allow Gradle to sync.

### Run

Connect an Android device or start an emulator and click **Run**.

---

## 🧠 Model Information

- **Model:** Convolutional Neural Network (CNN)
- **Framework:** TensorFlow/Keras
- **Deployment:** TensorFlow Lite (.tflite)
- **Input:** Character-level tokenized URL
- **Output:** Phishing probability score

---

## 📊 Dataset

The model was trained using a balanced dataset containing phishing and legitimate URLs.

| Dataset | URLs |
|---------|-----:|
| Phishing URLs | 5,500 |
| Legitimate URLs | 5,500 |
| **Total** | **11,000** |

### Dataset Split

| Type | Percentage |
|------|-----------:|
| Training | 80% |
| Testing | 20% |

---

## 📈 Results

- Approximately **90% detection accuracy**
- Real-time URL analysis
- Low-latency inference
- Offline phishing detection
- Enhanced browsing security

---

## 🔮 Future Enhancements

- Website content analysis
- Transformer-based phishing detection
- Cloud threat intelligence
- Browser extension support
- VPN integration
- Ad blocker
- URL reputation checking

---

## 📄 License

This project was developed for academic and educational purposes.

---

## ⭐ Support

If you found this project useful, consider giving it a ⭐ on GitHub!

# OmniCheck SADL Android SDK

### South African Driver’s Licence (SADL) Decoder — with Secure Activation & Native Portrait Extraction

The **OmniCheck SADL SDK** is a production-ready Android library that enables fast, offline decoding of South African driver’s licence (PDF417) barcodes, complete with text data extraction, RSA decryption, and optional portrait reconstruction — secured by device-bound activation tokens.

---

## 🧩 Features

- ✅ Decode South African Driver’s Licence (DL) barcodes (V1/V2)
- 🔐 Device-bound activation via online or offline token
- 📸 Extract embedded portrait via native C++ decoder
- 🧠 Smart RSA key handling (auto-detect version and block size)
- 📅 Parses ID number, names, issue/expiry dates, PRDP, vehicle codes, and more
- 🧾 Supports **Java 8+**, **Kotlin**, and **minSdk 23**
- ⚙️ Self-contained AAR — works offline after activation

---

## 📦 Installation

### Manual AAR

1. Copy `sadl-sdk-release.aar` into your app’s `libs/` folder.
2. In your app-level `build.gradle`:

   ```kotlin
   dependencies {
       implementation(files("libs/sadl-sdk-release.aar"))
   }
   ```
---

## 🚀 Quick Start

### 1️⃣ Initialize the SDK

```kotlin
Sadl.init(applicationContext)
```

### 2️⃣ Activate Online

```kotlin
val result = SadlActivator.activateOnlineAsync(
    ctx = applicationContext,
    baseUrl = "https://www.omnicheck.co.za",
    apiKey = "YOUR_API_KEY",
    product = "SADL-PRO",
    years = 1
)
```

Or **import an offline token**:

```kotlin
SadlActivator.importOfflineToken(applicationContext, "<SIGNED_TOKEN>")
```

### 3️⃣ Decode a Driver’s Licence

```kotlin
val dl = Sadl.decodeDriversLicenseBase64(base64String, includePhoto = true)

Log.d("DL", "Name: ${dl.firstNames} ${dl.surname}")
Log.d("DL", "ID: ${dl.idNumber}")
Log.d("DL", "Valid: ${dl.validFrom} → ${dl.validTo}")
```

If `includePhoto = true`, a portrait (JPEG bytes) will be available as `dl.photoJpeg`.

---

## 🧠 API Reference

| Component | Description |
|------------|--------------|
| `Sadl` | Public facade: init, decode, decrypt |
| `SadlActivator` | Handles online and offline activation |
| `LicenseManager` | Verifies, stores, and enforces activation |
| `RsaDriversLicense` | Data model for decoded licence |
| `NativeCore` | JNI bridge for native portrait decoding |

---

## 🔒 Activation Logic

- Each activation token is **device-specific** (based on SHA-256 fingerprint).
- Tokens are **Ed25519-signed** by the server.
- Expired, invalid, or mismatched tokens block decoding.
- Token persistence uses Android `SharedPreferences`.

### Laravel Backend Endpoint

The SDK expects a POST endpoint:
`/webservice/sadl_device_activate`

It should return:
```json
{ "token": "<base64url(payload)>.<base64url(signature)>" }
```

---

## 🧩 Example App

The `testapp2` module demonstrates:
- SDK initialization
- Online activation
- Licence decode with portrait rendering
- Activation state management

---

## 🛠 Build Requirements

| Tool | Version |
|------|----------|
| Android Gradle Plugin | 8.5+ |
| Kotlin | 1.9+ |
| Compile SDK | 34 |
| Min SDK | 23 |
| NDK | 26.1.10909125 (for libsadl.so) |

## 📜 License Enforcement

All decode calls route through `LicenseManager.assertValidOrStatus()` —  
unactivated or expired devices will throw:

```
IllegalStateException: SADL SDK not activated on this device.
```

---

## ⚖️ Security & Compliance

- Signed tokens (Ed25519)
- Device fingerprinting (SHA-256 of ANDROID_ID)
- POPIA-compliant (no data leaves the device)
- Uses `BouncyCastle` for cryptography
- Optional HTTPS-only activation endpoint

---

## 🧾 Changelog

| Version | Date | Changes |
|----------|------|----------|
| 1.0.0 | Oct 2025 | Initial production release (activation + portrait decode) |

---

## 💬 Support

OmniCheck / VerifyID.io  
📧 support@verifyid.co.za  
🌐 [https://www.omnicheck.co.za](https://www.omnicheck.co.za)  
📍 South Africa  

---

## ⚙️ Copyright

© 2025 OmniSol Information Technology (Pty) Ltd.  
All Rights Reserved.

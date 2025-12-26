# ESP32 Rescue Robot Firmware

Bu klasör, IoT Kurtarma Robotu için ESP32 firmware dosyalarını içerir.

## 📁 Dosyalar

### 1. `robot_esp32_ap.ino` (Basit Mod - ÖNERİLİR)
**Access Point modu** - En kolay kurulum!

**Özellikler:**
- ✅ WiFi Access Point (SSID: "RescueRobot", Şifre: "12345678")
- ✅ IP: 192.168.4.1 (sabit)
- ✅ TCP Server: Port 8080
- ✅ Manuel/Otomatik mod
- ✅ Motor kontrol
- ✅ BLE tarama
- ✅ Ultrasonik sensör

**Kullanım:**
1. ESP32'ye yükle
2. Robot'un WiFi ağına bağlan (RescueRobot)
3. Controller app'te IP: 192.168.4.1

---

### 2. `robot_esp32_mdns.ino` (Gelişmiş Mod)
**WiFi Station + mDNS modu** - Varolan WiFi'ye bağlanır

**Özellikler:**
- ✅ Varolan WiFi ağına bağlanır
- ✅ mDNS desteği (rescuerobot.local)
- ✅ Web dashboard (http://rescuerobot.local)
- ✅ TCP Server: Port 8080
- ✅ İstatistikler ve sensör verileri
- ✅ REST API endpoint'leri

**Kullanım:**
1. Koddaki WiFi SSID ve şifresini değiştir
2. ESP32'ye yükle
3. Serial Monitor'den IP adresini öğren
4. Controller app veya web tarayıcıdan bağlan

---

## 🔌 Pin Bağlantıları

### Motor Sürücü (L298N/L293D)
```
LEFT MOTOR:
  - Forward:  GPIO 25
  - Backward: GPIO 26
  
RIGHT MOTOR:
  - Forward:  GPIO 27
  - Backward: GPIO 14
```

### Sensörler
```
ULTRASONIK (HC-SR04):
  - TRIG: GPIO 5
  - ECHO: GPIO 18
  
VICTIM DETECTION:
  - Pin: GPIO 19 (Active LOW)
```

---

## 📡 Protokol

### Komutlar (Controller → Robot)
| Komut | Açıklama |
|-------|----------|
| `F` | İleri git |
| `B` | Geri git |
| `L` | Sola dön |
| `R` | Sağa dön |
| `S` | Dur |
| `A` | Otomatik mod |
| `M` | Manuel mod |

### Mesajlar (Robot → Controller)
| Mesaj | Açıklama |
|-------|----------|
| `MOVE:F` | İleri gidiliyor |
| `MOVE:B` | Geri gidiliyor |
| `MOVE:L` | Sola dönülüyor |
| `MOVE:R` | Sağa dönülüyor |
| `MOVE:S` | Durdu |
| `OBSTACLE` | Engel tespit edildi |
| `VICTIM` | Kazazede bulundu |
| `RADAR:XX` | BLE RSSI değeri (örn: RADAR:-65) |
| `MODE:AUTO` | Otomatik moda geçildi |
| `MODE:MANUAL` | Manuel moda geçildi |

---

## 🚀 Yükleme Adımları

1. **Arduino IDE Kurulumu:**
   - Arduino IDE'yi aç
   - File → Preferences → Additional Boards Manager URLs'e ekle:
     ```
     https://dl.espressif.com/dl/package_esp32_index.json
     ```

2. **ESP32 Board Kurulumu:**
   - Tools → Board → Boards Manager
   - "esp32" ara ve yükle

3. **Gerekli Kütüphaneler:**
   - WiFi (ESP32 ile birlikte gelir)
   - BLE (ESP32 ile birlikte gelir)
   - ESPmDNS (ESP32 ile birlikte gelir)
   - WebServer (ESP32 ile birlikte gelir)

4. **Board Ayarları:**
   - Board: "ESP32 Dev Module"
   - Upload Speed: 115200
   - Flash Frequency: 80MHz

5. **Yükle:**
   - Dosyayı aç (.ino)
   - Upload butonuna bas
   - Serial Monitor'ü aç (115200 baud)

---

## 🔧 Test

### Serial Monitor Çıktısı (Başarılı):
```
🚀 Starting Rescue Robot...
📡 AP IP: 192.168.4.1
🌐 TCP Server started on port 8080
📶 BLE initialized
✅ Robot ready!
📱 Client connected!
📨 Command: F
⬆️ Moving Forward
📤 Sent: MOVE:F
```

---

## 💡 İpuçları

1. **AP Modu** küçük projeler için idealdir (WiFi router'a ihtiyaç yok)
2. **mDNS Modu** birden fazla robot kullanıyorsanız daha pratik
3. BLE tarama otomatik modda aktif olur
4. Ultrasonik sensör <20cm mesafede engel algılar
5. Victim detection pini LOW olduğunda alarm verir

---

## 📊 Performans

- TCP Bağlantı: <500ms
- Komut İşleme: <50ms
- BLE Tarama: 1 saniye (her 3 saniyede)
- Sensör Okuma: 500ms (ultrasonik), 1000ms (victim)

---

## ⚠️ Notlar

- Motor pinlerini donanımınıza göre değiştirin
- BLE tarama pil tüketimini artırır
- Otomatik modda basit engel önleme algoritması vardır
- Web dashboard sadece mDNS versiyonunda mevcuttur

---

## 📞 Destek

Sorun yaşarsanız Serial Monitor çıktısını kontrol edin.
Detaylı log mesajları her işlem için gösterilir.

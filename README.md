# Filey - Akıllı & Güvenli Android Dosya Yöneticisi

**Filey**, Jetpack Compose ve Material 3 (Material You) ile geliştirilmiş, sıradan bir dosya yöneticisinden çok daha fazlasını sunan bir **akıllı asistan**dır. Dosyalarınızı sadece listelemez, onları anlar, düzenler ve en üst düzeyde korur.

![Build Status](https://github.com/omersusin/Filey/actions/workflows/build.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)
![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-green.svg)
![Security](https://img.shields.io/badge/Security-AES--256-red.svg)

## 🌟 Öne Çıkan Özellikler

### 🛡️ Güvenli Kasa (Secure Vault)
- **AES-256 Şifreleme:** Özel dosyalarınızı askeri düzeyde şifreleme ile koruyun.
- **PIN & Biyometrik Koruma:** Kasanıza sadece siz erişebilirsiniz.
- **Gizli Alan:** Şifrelenen dosyalar sistem galerisinden ve diğer uygulamalardan tamamen gizlenir.

### 🧠 Akıllı Düzenleyici (Smart Organizer)
- **Kural Tabanlı Organizasyon:** Uzantılara göre dosyaları otomatik olarak ilgili klasörlere (PDF -> Belgeler, JPG -> Resimler vb.) taşır.
- **İsim Normalizer:** Karmaşık, URL-encoded veya anlamsız dosya isimlerini tek tıkla profesyonel ve okunaklı hale getirir.
- **Otomatik Versiyonlama:** Metin dosyalarını düzenlerken orijinal halini yedekleyerek veri kaybını önler.

### 📂 Gelişmiş Dosya Yönetimi
- **Material 3 UI Overhaul:** Dinamik renkler, akıcı animasyonlar ve modern bir "Dashboard" deneyimi.
- **Dahili Görüntüleyiciler:**
  - 📄 **PDF Okuyucu:** Harici uygulamaya ihtiyaç duymadan pürüzsüz PDF deneyimi.
  - 🖼️ **Medya Galerisi:** Resim ve videolar için hızlı önizleme ve oynatma.
  - 📝 **Kod Editörü:** Sözdizimi vurgulama desteği ile metin dosyalarını düzenleme.
- **Arşiv Uzmanı:** Zip, Rar, 7z ve daha fazlasını görüntüleyin veya çıkartın.

### ⚡ İleri Düzey Yetenekler
- **Root & Shizuku Desteği:** Sistem dosyalarına tam erişim ve güvenli işlem yapabilme.
- **Depolama Analizi:** Büyük dosyaları ve kopyaları bularak yer açın.
- **Kablosuz Paylaşım:** Dahili sunucu ile dosyalarınızı tarayıcı üzerinden kolayca paylaşın.

## 🛠️ Teknik Altyapı

- **UI Framework:** Jetpack Compose (Material You)
- **Mimari:** MVVM + Clean Architecture + Modular Design
- **Güvenlik:** AES-256-CBC, PBKDF2
- **Kütüphaneler:**
  - `androidx.media3`: Medya oynatma
  - `io.coil-kt`: Resim ve Video Thumbnail yükleme
  - `com.github.topjohnwu.libsu`: Root erişimi
  - `org.apache.commons:commons-compress`: Arşiv işlemleri
  - `androidx.navigation:navigation-compose`: Modüler Navigasyon

## 📦 Kurulum

1. Depoyu klonlayın:
   ```bash
   git clone https://github.com/omersusin/Filey.git
   ```
2. Android Studio (Hedgehog veya daha yeni bir sürüm) ile projeyi açın.
3. Gradle senkronizasyonunun tamamlanmasını bekleyin.
4. `Run` butonuna basarak modern dosya yönetimi deneyimine başlayın.

## 🤝 Katkıda Bulunma

Filey'i daha akıllı hale getirmemize yardımcı olun! Her türlü hata bildirimi, özellik önerisi veya `Pull Request` için kapımız açık.

## 📜 Lisans

Bu proje MIT Lisansı altında lisanslanmıştır.

---
**Geliştirici:** [Ömer Süsin](https://github.com/omersusin)
**Vizyon:** Dosya yönetimini bir angarya olmaktan çıkarıp, akıllı bir deneyime dönüştürmek.

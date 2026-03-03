# Filey - Modern Android Dosya Yöneticisi

**Filey**, Jetpack Compose ve Material 3 ile geliştirilmiş, hızlı, güvenli ve kullanıcı dostu bir Android dosya yöneticisidir. Hem standart kullanıcılar hem de ileri düzey (Root) kullanıcılar için optimize edilmiştir.

![Build Status](https://github.com/omersusin/Filey/actions/workflows/build.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)
![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-green.svg)

## 🚀 Özellikler

- **Modern Tasarım:** Jetpack Compose ve Material 3 ile akıcı ve estetik bir kullanıcı deneyimi.
- **Kapsamlı Dosya Yönetimi:** Kopyalama, taşıma, silme, yeniden adlandırma ve yeni dosya/klasör oluşturma.
- **Root Desteği:** `libsu` kütüphanesi ile sistem dosyalarına erişim ve işlem yapabilme yeteneği.
- **Dahili Görüntüleyici ve Oynatıcılar:**
  - 🖼️ **Resim Görüntüleyici:** Coil ile hızlı resim yükleme ve görüntüleme.
  - 🎬 **Video & Ses Oynatıcı:** Media3 (ExoPlayer) altyapısı ile gelişmiş medya oynatma deneyimi.
  - 📄 **Metin Düzenleyici:** Basit metin dosyalarını doğrudan uygulama içinden düzenleyin.
- **Arşiv Yönetimi:** Zip ve Rar dosyalarını görüntüleme ve çıkartma (Apache Commons Compress & Junrar).
- **Hızlı Erişim:** Depolama bilgilerini izleme ve kategori bazlı (Resim, Video, Belge vb.) dosya filtreleme.

## 🛠️ Teknik Altyapı

- **Dil:** %100 Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Mimari:** MVVM (Model-View-ViewModel) + Clean Architecture prensipleri
- **Önemli Kütüphaneler:**
  - `androidx.media3`: Medya oynatma
  - `io.coil-kt`: Resim yükleme
  - `com.github.topjohnwu.libsu`: Root erişimi
  - `org.apache.commons:commons-compress`: Arşiv işlemleri
  - `androidx.navigation:navigation-compose`: Uygulama içi navigasyon

## 📦 Kurulum

Projenizi yerel ortamda çalıştırmak için şu adımları izleyin:

1. Depoyu klonlayın:
   ```bash
   git clone https://github.com/omersusin/Filey.git
   ```
2. Android Studio (Hedgehog veya daha yeni bir sürüm) ile projeyi açın.
3. Gradle senkronizasyonunun tamamlanmasını bekleyin.
4. Cihazınıza veya emülatörünüze yüklemek için `Run` butonuna basın.

## 🤝 Katkıda Bulunma

Hata bildirimleri veya özellik önerileri için lütfen bir `Issue` açın veya bir `Pull Request` gönderin. Katkılarınızdan memnuniyet duyarız!

## 📜 Lisans

Bu proje MIT Lisansı altında lisanslanmıştır. Daha fazla bilgi için `LICENSE` dosyasına göz atabilirsiniz.

---
**Geliştirici:** [Ömer Susin](https://github.com/omersusin)

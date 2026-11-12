# Yoldaş Offline Demo 0.1

Android 12+ için gizlilik odaklı sesli avatar prototipi.

## Bu sürümde çalışanlar
- Standart sarı 2D avatar: göz kırpma + konuşurken ağız animasyonu.
- Android'in **on-device** konuşma tanıması mevcutsa Türkçe dinleme.
- Yalnızca ağ gerektirmediğini bildiren kurulu Türkçe TTS sesini seçme.
- Çocuksu/enerjik etki için TTS pitch/rate ayarı.
- Basit yerel hafıza (isim) ve kural tabanlı yanıtlar.
- Manifestte `INTERNET` izni YOK; reklam/analitik SDK YOK.

## Bilinçli sınır
Bu 0.1 paketi henüz gerçek 1–1.5B yerel LLM veya kendi STT/TTS modellerini içermez. Android'in cihaz-üstü STT/TTS hizmetlerini kullanır ve bulut fallback yapmaz. Cihazda on-device STT yoksa sesli giriş kapatılır. Bu, gizlilik ilkesini ihlal etmemek içindir.

## Derleme
Android Studio'da klasörü açın. JDK 17 ve Android SDK 35 ile `app` modülünü çalıştırın veya Build > Build APK(s) kullanın.

İlk Gradle/SDK bileşenlerinin kurulması internet gerektirebilir; uygulamanın kendisi INTERNET izni istemez.

package com.yoldas.offline

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.*
import android.graphics.Color
import android.view.Gravity
import java.util.Locale

class MainActivity : android.app.Activity(), RecognitionListener, TextToSpeech.OnInitListener {
    private lateinit var avatar: EmojiAvatarView
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var button: Button
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val prefs by lazy { getSharedPreferences("private_memory", MODE_PRIVATE) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_HORIZONTAL; setPadding(28,48,28,32); setBackgroundColor(Color.rgb(17,19,26)) }
        avatar=EmojiAvatarView(this).apply { layoutParams=LinearLayout.LayoutParams(-1,0,1f) }
        status=TextView(this).apply { text="Hazırlanıyor…"; textSize=16f; setTextColor(Color.WHITE); gravity=Gravity.CENTER }
        transcript=TextView(this).apply { text="Mikrofona dokun ve Türkçe konuş."; textSize=18f; setTextColor(Color.rgb(220,220,230)); setPadding(0,22,0,22); gravity=Gravity.CENTER }
        button=Button(this).apply { text="🎙 Konuş"; textSize=18f; setOnClickListener { startListening() } }
        root.addView(avatar);root.addView(status);root.addView(transcript);root.addView(button);setContentView(root)
        tts=TextToSpeech(this,this)
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),7)
        setupRecognizer()
    }

    private fun setupRecognizer(){
        if(android.os.Build.VERSION.SDK_INT>=31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this).also{it.setRecognitionListener(this)}
            status.text="Çevrimdışı konuşma tanıma hazır"
        } else {
            status.text="Bu cihazda Android'in yerel konuşma tanıması yok. Gizlilik gereği bulut tanıma kullanılmayacak."
            button.isEnabled=false
        }
    }
    private fun startListening(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) return
        avatar.setTalking(false); tts?.stop(); status.text="Dinliyorum…"
        val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
        }
        recognizer?.startListening(i)
    }
    private fun answer(input:String):String{
        val t=input.lowercase(Locale("tr","TR"))
        val nameRegex=Regex("(?:benim adım|bana)\\s+([A-Za-zÇĞİÖŞÜçğıöşü]{2,24})(?:\\s+de|\\s+diye)?")
        nameRegex.find(input)?.groupValues?.getOrNull(1)?.let { prefs.edit().putString("name",it).apply(); return "Memnun oldum $it. İsmini bu telefondaki hafızamda tutacağım." }
        val name=prefs.getString("name",null)
        return when {
            "merhaba" in t || "selam" in t -> "Merhaba${if(name!=null) " $name" else ""}! Bugün ne konuşmak istersin?"
            "nasılsın" in t -> "Gayet iyiyim. Seninle konuşmaya hazırım. Sen nasılsın?"
            "adım ne" in t -> if(name!=null) "Bana adının $name olduğunu söylemiştin." else "Henüz adını bana öğretmedin."
            "internet" in t || "gizli" in t -> "Bu denemede internet iznim yok. Konuşma tanıma yalnızca cihazındaki çevrimdışı hizmet varsa çalışıyor."
            "saat" in t -> "Saat sorusunu anladım ama bu ilk denemede saat bilgisini konuşma motoruna henüz bağlamadık."
            else -> "Seni şöyle anladım: $input. Bu ilk sürümde cevap motorum sınırlı. Sonraki aşamada tamamen yerel dil modelini bağlayınca çok daha doğal cevap vereceğim."
        }
    }
    private fun speak(s:String){transcript.text=s;status.text="Konuşuyorum…";avatar.setTalking(true);tts?.speak(s,TextToSpeech.QUEUE_FLUSH,null,"reply")}
    override fun onInit(x:Int){
        if(x!=TextToSpeech.SUCCESS){status.text="Ses motoru başlatılamadı";return}
        tts?.language=Locale("tr","TR")
        // Privacy gate: select only an installed voice that says it does not require network.
        val offline=tts?.voices?.filter{!it.isNetworkConnectionRequired}?.filter{it.locale.language=="tr"}?.maxByOrNull{it.quality}
        if(offline!=null) tts?.voice=offline else status.text="Çevrimdışı Türkçe TTS sesi bulunamadı"
        tts?.setPitch(1.25f);tts?.setSpeechRate(1.08f)
        tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
            override fun onStart(id:String?){runOnUiThread{avatar.setTalking(true)}}
            override fun onDone(id:String?){runOnUiThread{avatar.setTalking(false);status.text="Hazırım"}}
            @Deprecated("Deprecated") override fun onError(id:String?){runOnUiThread{avatar.setTalking(false);status.text="Ses hatası"}}
        })
    }
    override fun onResults(r:Bundle){val s=r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?:return;transcript.text="Sen: $s";speak(answer(s))}
    override fun onPartialResults(r:Bundle){r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let{transcript.text="Sen: $it"}}
    override fun onReadyForSpeech(p:Bundle?){status.text="Dinliyorum…"}
    override fun onBeginningOfSpeech(){};override fun onRmsChanged(v:Float){};override fun onBufferReceived(b:ByteArray?){};override fun onEndOfSpeech(){status.text="Düşünüyorum…"}
    override fun onError(e:Int){status.text="Dinleme hatası ($e). Tekrar deneyebilirsin."}
    override fun onEvent(t:Int,p:Bundle?){}
    override fun onDestroy(){recognizer?.destroy();tts?.shutdown();super.onDestroy()}
}

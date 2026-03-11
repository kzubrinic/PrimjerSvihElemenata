package hr.unidu.kz.primjersvihelemenata

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private val mojReceiver = TimerReceiver()

    // Launcher koji čeka odgovor korisnika za dozvolu obavijesti
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Dozvola za obavijesti odobrena!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bez dozvole neću moći prikazati završetak mjerenja.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Traži dozvolu za notifikacije (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val btn = findViewById<Button>(R.id.btnPokreni)
        btn.setOnClickListener {
            // Nema više provjere kalendara, samo pokrećemo servis
            startService(Intent(this, TimerService::class.java))
            Toast.makeText(this, "Mjerenje počinje...", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        // Registracija receivera uz sigurnosnu zastavicu
        val filter = IntentFilter("TIMER_FINISHED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mojReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(mojReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mojReceiver)
    }
}
class TimerService : Service() {
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Thread {
            Thread.sleep(5000) // Simulacija rada
            // Šalje broadcast nakon rada
            val intent = Intent("TIMER_FINISHED")
            intent.setPackage(packageName) // 'packageName' automatski dohvaća ID tvoje aplikacije
            intent.putExtra("vrijeme", "5 sekundi")
            sendBroadcast(intent)

            stopSelf()
        }.start()
        return START_STICKY
    }
    override fun onBind(i: Intent?) = null
}

class TimerReceiver : BroadcastReceiver() {
    @SuppressLint("ServiceCast")
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "mjerenje_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Kreiraj kanal (potrebno za Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Obavijesti mjerenja",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Napravi samu obavijest
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Ikona
            .setContentTitle("Mjerenje završeno")
            .setContentText("Tvoje mjerenje od 5 sekundi je uspješno gotovo!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Obavijest nestaje kad se klikne na nju

        // 3. Pokaži obavijest
        notificationManager.notify(1, builder.build())

        Toast.makeText(context, "Sustav primio signal!", Toast.LENGTH_SHORT).show()
    }
}
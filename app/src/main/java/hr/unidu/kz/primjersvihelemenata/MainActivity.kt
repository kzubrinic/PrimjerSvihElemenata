package hr.unidu.kz.primjersvihelemenata

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // Launcher koji čeka odgovor korisnika na zahtjev za dozvolu
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Dozvola odobrena!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bez dozvole ne mogu pisati u kalendar.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Povezuješ se s XML-om

        val btn = findViewById<Button>(R.id.btnPokreni)

        btn.setOnClickListener {
            // Provjera dozvole
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                startService(Intent(this, TimerService::class.java))
            } else {
                // Traženje dozvole
                requestPermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
            }
        }
    }
}

class TimerService : Service() {
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Thread {
            Thread.sleep(5000) // Simulacija rada
            // Šalje broadcast nakon rada
            sendBroadcast(Intent("TIMER_FINISHED").putExtra("vrijeme", "5 sekundi"))
            stopSelf()
        }.start()
        return START_STICKY
    }
    override fun onBind(i: Intent?) = null
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val vrijeme = intent.getStringExtra("vrijeme")

        // ContentResolver: Upis u kalendar
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, System.currentTimeMillis())
            put(CalendarContract.Events.DTEND, System.currentTimeMillis() + 60000)
            put(CalendarContract.Events.TITLE, "Završeno mjerenje: $vrijeme")
            put(CalendarContract.Events.CALENDAR_ID, 1) // ID primarnog kalendara
            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
        }

        context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        Toast.makeText(context, "Spremljeno u kalendar!", Toast.LENGTH_LONG).show()
    }
}
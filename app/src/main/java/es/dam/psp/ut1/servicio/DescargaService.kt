package es.dam.psp.ut1.servicio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlin.concurrent.thread

/**
 * DescargaService.kt · Práctica 5 ·  .
 * Completa los TODO 5.b (1 a 4); los mensajes de log que piden sirven para las evidencias de 5.d.
 *
 * Apuntes UT-1, apartados 7.2 (servicios) y 7.3 (Logcat, adb).
 * Dónde va: en el proyecto Android ServicioPSP, paquete es.dam.psp.ut1.servicio
 * (sigue android_servicio/LEEME.txt). No tiene main: lo inicia MainActivity con
 * ContextCompat.startForegroundService(...) y lo para con stopService(...).
 * Resultado: notificación con barra de progreso y líneas en Logcat (filtro tag:PSP-UT1).
 *
 * Recuerda: un Service se ejecuta en el HILO PRINCIPAL de la app. El trabajo largo
 * (el bucle de la descarga) debe ir en un hilo aparte; para eso usamos thread { }.
 */
class DescargaService : Service() {

    private var trabajador: Thread? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {       // los canales existen desde Android 8
            val canal = NotificationChannel(CANAL, "Descargas", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
        // TODO 5.b (1/4): escribe en el log (Log.d con TAG) el PID del proceso y el nombre del hilo actual

        val PID = android.os.Process.myPid()
        val nombreHilo = Thread.currentThread().name

        Log.d (TAG, "El PID es: $PID y el nomre del hilo es: $nombreHilo")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO 5.b (2/4): pasa el servicio a primer plano con ServiceCompat.startForeground(...)
        //         usando notificacion(0) y el tipo ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        //         (en versiones anteriores a Android 10 / API 29 el tipo es 0).

        val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, ID_NOTIFICACION, notificacion(0), tipo)

        // TODO 5.b (3/4): si trabajador es null, crea con thread(name = "descarga") { ... } un hilo que:
        //         - recorra el progreso de 0 a 100 de 5 en 5,
        //         - actualice la notificación con notify(ID_NOTIFICACION, notificacion(progreso)),
        //         - escriba en el log el progreso, el PID y el nombre del hilo,
        //         - duerma 1 segundo entre pasos (Thread.sleep),
        //         - capture InterruptedException (descarga cancelada),
        //         - y llame a stopSelf() al terminar (en un finally).

        if (trabajador == null) {
            trabajador = thread(name = "descarga") {
                try {
                    val notificationManager = getSystemService(NotificationManager::class.java)

                    for (progreso in 0..100 step 5) {
                        notificationManager.notify(ID_NOTIFICACION, notificacion(progreso))
                        Log.d(TAG, "Progreso: $progreso%, PID: ${android.os.Process.myPid()}, Hilo: ${Thread.currentThread().name}")
                        Thread.sleep(1000)
                    }
                } catch (e: InterruptedException) {
                    Log.d(TAG, "Descarga cancelada (hilo interrumpido)")
                } finally {
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY   // si el sistema mata el proceso, no se recrea el servicio
    }

    override fun onDestroy() {
        // TODO 5.b (4/4): interrumpe el hilo trabajador y escribe "onDestroy" en el log
        trabajador?.interrupt()
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    /** Servicio iniciado (started), no ligado: no ofrece interfaz de enlace. */
    override fun onBind(intent: Intent?): IBinder? = null

    private fun notificacion(progreso: Int): Notification =
        NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargando datos")
            .setContentText("$progreso %")
            .setProgress(100, progreso, false)
            .setOngoing(true)            // el usuario no puede descartarla mientras dura
            .setOnlyAlertOnce(true)      // no suena en cada actualización del progreso
            .build()

    companion object {
        const val TAG = "PSP-UT1"
        const val CANAL = "descargas"
        const val ID_NOTIFICACION = 1
    }
}

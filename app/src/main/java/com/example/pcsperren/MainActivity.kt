package com.example.pcsperren

import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.pcsperren.databinding.ActivityMainBinding
import java.io.IOException
import java.net.Socket
import java.net.InetSocketAddress

class MainActivity : AppCompatActivity() {
    private var currentLockTask: LockTask? = null
    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())

    private val getStatusRunnable = object : Runnable {
        override fun run() {
            GetStatusTask().execute()
            handler.postDelayed(this, 5000)
        }
    }

    private val sendCommandRunnable = object : Runnable {
        override fun run() {
            currentLockTask?.cancel(true)
            currentLockTask = LockTask()
            currentLockTask?.execute()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handler.post(getStatusRunnable)

        binding.lockButton.setOnClickListener {
            Log.d("MainActivity", "Lock Button clicked")
            handler.removeCallbacks(sendCommandRunnable)
            handler.post(sendCommandRunnable)
        }

        binding.unlockButton.setOnClickListener {
            Log.d("MainActivity", "Unlock Button clicked")
            handler.removeCallbacks(sendCommandRunnable)
            currentLockTask?.cancel(true)
            UnlockTask().execute()
        }
    }
    override fun onResume() {
        super.onResume()
        handler.post(getStatusRunnable)
    }
    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(getStatusRunnable)
    }

    private inner class LockTask : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void?): Boolean {
            Log.d("MainActivity", "Sending lock command")
            return sendCommand("lock")
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                binding.statusView.text = "PC ist gesperrt."
                binding.statusView.setTextColor(Color.YELLOW)
                binding.lockButton.setBackgroundColor(Color.YELLOW)
                binding.unlockButton.setBackgroundColor(Color.GRAY)
            } else {
                binding.statusView.text = "Das Ziel ist nicht eingeschalten, oder Sie sind nicht mit dem lokalen Netzwerk verbunden. Der Befehl konnte nicht gesendet werden."
                binding.statusView.setTextColor(Color.RED)
            }
        }
    }

    private inner class UnlockTask : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void?): Boolean {
            Log.d("MainActivity", "Sending unlock command")
            return sendCommand("unlock")
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                binding.statusView.text = "PC ist entsperrt."
                binding.statusView.setTextColor(Color.GREEN)
                binding.lockButton.setBackgroundColor(Color.GRAY)
                binding.unlockButton.setBackgroundColor(Color.GREEN)
            } else {
                binding.statusView.text = "Das Ziel ist nicht eingeschalten, oder Sie sind nicht mit dem lokalen Netzwerk verbunden. Der Befehl konnte nicht gesendet werden."
                binding.statusView.setTextColor(Color.RED)
            }
        }
    }

    private inner class GetStatusTask : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String {
            return sendStatusCommand("get_status")
        }

        override fun onPostExecute(result: String) {
            when (result.trim()) {
                "locked" -> {
                    binding.statusView.text = "PC ist gesperrt."
                    binding.statusView.setTextColor(Color.YELLOW)
                    binding.lockButton.setBackgroundColor(Color.YELLOW)
                    binding.unlockButton.setBackgroundColor(Color.GRAY)
                }
                "unlocked" -> {
                    binding.statusView.text = "PC ist entsperrt."
                    binding.statusView.setTextColor(Color.GREEN)
                    binding.lockButton.setBackgroundColor(Color.GRAY)
                    binding.unlockButton.setBackgroundColor(Color.GREEN)
                }
                else -> {
                    binding.statusView.text = "PC nicht erreichbar. Prüfe ob PC eingeschalten ist und du im WLAN verbunden bist"
                    binding.statusView.setTextColor(Color.RED)
                }
            }
        }
    }

    private fun sendCommand(command: String): Boolean {
        try {
            val socketAddress = InetSocketAddress("192.168.0.17", 12345)
            val socket = Socket()

            socket.connect(socketAddress, 3000)

            val out = socket.getOutputStream()
            out.write(command.toByteArray())
            out.flush()
            out.close()
            socket.close()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    private fun sendStatusCommand(command: String): String {
        try {
            val socketAddress = InetSocketAddress("192.168.0.17", 12345)
            val socket = Socket()

            socket.connect(socketAddress, 1000)

            val out = socket.getOutputStream()
            val input = socket.getInputStream()

            out.write(command.toByteArray())
            out.flush()

            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)
            out.close()
            socket.close()

            return String(buffer, 0, bytesRead)
        } catch (e: IOException) {
            e.printStackTrace()
            return "error"
        }
    }
}

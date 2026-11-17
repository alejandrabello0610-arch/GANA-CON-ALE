
package com.ganaconale.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import android.graphics.Typeface
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val requestCode = 100
    private val sppUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var printer: BluetoothSocket? = null
    private lateinit var status: TextView
    private lateinit var adapter: BluetoothAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager.adapter

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
        }

        layout.addView(TextView(this).apply {
            text = "GANA CON ALE"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
        })

        layout.addView(TextView(this).apply {
            text = "Sistema de rifas"
            textSize = 18f
        })

        status = TextView(this).apply {
            text = "Impresora: desconectada"
            textSize = 16f
            setPadding(0, 35, 0, 25)
        }
        layout.addView(status)

        val devices = Button(this).apply {
            text = "BUSCAR IMPRESORAS VINCULADAS"
            setOnClickListener { showPrinters() }
        }
        layout.addView(devices)

        val test = Button(this).apply {
            text = "IMPRIMIR PRUEBA"
            setOnClickListener { printTest() }
        }
        layout.addView(test)

        setContentView(layout)

        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                requestCode
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun showPrinters() {
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                requestCode
            )
            return
        }

        if (!adapter.isEnabled) {
            status.text = "Activa el Bluetooth del teléfono"
            return
        }

        val paired = adapter.bondedDevices.toList()

        if (paired.isEmpty()) {
            status.text = "Primero vincula tu impresora en Bluetooth"
            return
        }

        val names = paired.map { it.name ?: it.address }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecciona tu impresora")
            .setItems(names) { _, position ->
                connect(paired[position])
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        status.text = "Conectando..."

        thread {
            try {
                printer?.close()
                val socket = device.createRfcommSocketToServiceRecord(sppUUID)
                socket.connect()
                printer = socket

                runOnUiThread {
                    status.text = "Conectada: ${device.name}"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Error de conexión: ${e.message}"
                }
            }
        }
    }

    private fun printTest() {
        val socket = printer

        if (socket == null || !socket.isConnected) {
            status.text = "Primero conecta la impresora"
            return
        }

        thread {
            try {
                val output = socket.outputStream
                output.write(byteArrayOf(0x1B, 0x40))
                output.write(
                    """
                    GANA CON ALE
                    ------------------------------
                    PRUEBA DE IMPRESION
                    Impresora POS-58H
                    Conexion Bluetooth correcta
                    ------------------------------


                    """.toByteArray(Charsets.UTF_8)
                )
                output.write(byteArrayOf(0x1D, 0x56, 0x00))
                output.flush()

                runOnUiThread {
                    status.text = "Prueba enviada correctamente"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Error al imprimir: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            printer?.close()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}

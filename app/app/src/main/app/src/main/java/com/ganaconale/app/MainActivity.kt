
package com.ganaconale.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var status: TextView
    private var printer: BluetoothSocket? = null

    private val sppUUID = UUID.fromString(
        "00001101-0000-1000-8000-00805F9B34FB"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager = getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

        bluetoothAdapter = manager.adapter

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 40, 28, 28)
        }

        layout.addView(TextView(this).apply {
            text = "GANA CON ALE"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        })

        layout.addView(TextView(this).apply {
            text = "Sistema de rifas"
            textSize = 18f
            gravity = Gravity.CENTER
        })

        status = TextView(this).apply {
            text = "Impresora desconectada"
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }

        layout.addView(status)

        layout.addView(Button(this).apply {
            text = "CONECTAR IMPRESORA"
            setOnClickListener { showPrinters() }
        })

        layout.addView(Button(this).apply {
            text = "IMPRIMIR PRUEBA"
            setOnClickListener { printTest() }
        })

        setContentView(layout)

        requestBluetoothPermission()
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                100
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun showPrinters() {
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetoothPermission()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            status.text = "Activa el Bluetooth del teléfono"
            return
        }

        val devices = bluetoothAdapter.bondedDevices.toList()

        if (devices.isEmpty()) {
            status.text = "Vincula primero la impresora en Android"
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Selecciona tu impresora")
            .setItems(devices.map {
                it.name ?: it.address
            }.toTypedArray()) { _, position ->
                connectPrinter(devices[position])
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun connectPrinter(
        device: android.bluetooth.BluetoothDevice
    ) {
        status.text = "Conectando..."

        thread {
            try {
                printer?.close()

                val socket =
                    device.createRfcommSocketToServiceRecord(sppUUID)

                socket.connect()
                printer = socket

                runOnUiThread {
                    status.text = "Impresora conectada: ${device.name}"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Error: ${e.message}"
                }
            }
        }
    }

    private fun printTest() {
        val socket = printer

        if (socket == null || !socket.isConnected) {
            status.text = "Conecta primero la impresora"
            return
        }

        thread {
            try {
                val output = socket.outputStream

                output.write(byteArrayOf(0x1B, 0x40))

                output.write(
                    """
                    GANA CON ALE
                    ------------------------
                    PRUEBA DE IMPRESION
                    POS-58H
                    Bluetooth conectado
                    ------------------------


                    """.toByteArray(Charsets.UTF_8)
                )

                output.flush()

                runOnUiThread {
                    status.text = "Prueba enviada a la impresora"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Error de impresión: ${e.message}"
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

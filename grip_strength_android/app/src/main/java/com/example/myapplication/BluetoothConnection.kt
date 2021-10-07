package com.example.myapplication

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import java.io.IOException
import java.util.*

class BluetoothConnection : AppCompatActivity() {

    private val btnRefreshBTList: Button by lazy { findViewById<Button>(R.id.btn_refresh_bt_list) }
    private val btListView: ListView by lazy { findViewById<ListView>(R.id.btList) }
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val REQUEST_ENABLE_BT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_connection)


        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show()
        }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        btnRefreshBTList.setOnClickListener {
            val list = ArrayList<Map<String, String>>()
            pairedDevices?.forEach { device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                list.add(mapOf("name" to deviceName, "address" to deviceHardwareAddress))
            }

            val listAdapter = SimpleAdapter(
                this,
                list,
                android.R.layout.simple_list_item_2,
                arrayOf("name", "address"),
                intArrayOf(android.R.id.text1, android.R.id.text2)
            )

            btListView.adapter = listAdapter
            btListView.setOnItemClickListener { parent, view, position, id ->

                val selectedItem = pairedDevices?.elementAt(position)
                //Toast.makeText(this, "You Clicked: " + selectedItem?.name, Toast.LENGTH_SHORT).show()
                try {
                    ConnectThread(selectedItem).run()
                    Toast.makeText(this, "Connecting to " + selectedItem?.name, Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, GripStrength::class.java).apply {

                    }
                    startActivity(intent)
                } catch (ex: IOException) {

                    Toast.makeText(this, "Connection failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private inner class ConnectThread(device: BluetoothDevice?) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device?.createRfcommSocketToServiceRecord(MY_UUID)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.

                //Toast.makeText(parent, "Connection succeeded!", Toast.LENGTH_SHORT).show()
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
}



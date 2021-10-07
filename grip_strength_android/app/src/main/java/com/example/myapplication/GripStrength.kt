package com.example.myapplication

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.ekn.gruzer.gaugelibrary.ArcGauge
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.io.IOException
import java.io.InputStream
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext


class GripStrength : AppCompatActivity(), CoroutineScope by MainScope() {
    private val btnConnectToBT: Button by lazy { findViewById(R.id.btn_bt_connect) }
    private val btnGripTest: Button by lazy { findViewById(R.id.btn_grip_test) }
    private val btnPressMe: Button by lazy { findViewById(R.id.btn_press_me) }
    private val arcGauge: ArcGauge by lazy { findViewById(R.id.arcGauge) }
    private val chart: LineChart by lazy { findViewById(R.id.chart) }

    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val REQUEST_ENABLE_BT = 1
    private val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
    private var btConnection: ConnectThread? = null
    private var startCheck: Boolean = false;
    var db = FirebaseFirestore.getInstance()
    var suckmydick = db.collection("suckmydick")
    var currentValue = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grip_strength)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        //val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show()
        }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtInnt = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        }
        //val message = intent.getIntExtra(EXTRA_MESSAGE)

        btnConnectToBT.setOnClickListener {
            if (btConnection?.checkConnection() == true) {
                btConnection?.cancel()
                btnConnectToBT.setText("Connect")
            } else {
                //Toast.makeText(this, "You Clicked:dsfsd ", Toast.LENGTH_SHORT).show()
                val list = ArrayList<Map<String, String>>()
                pairedDevices?.forEach { device ->
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    list.add(mapOf("title" to deviceName, "content" to deviceHardwareAddress))
                }

                val intent = Intent(this, BluetoothList::class.java)
                intent.putExtra("extra_list", list)
                launchBluetoothList.launch(intent)

            }

        }
        btnGripTest.setOnClickListener {
            arcGauge.maxValue = 3.3
            arcGauge.minValue = 0.0
            startCheck = true
            btnGripTest.setText("testing...")
            launch{
                timer1()
                startCheck = false
                btnGripTest.setText("start")
            }

        }
        btnPressMe.setOnClickListener {

            suckmydick.get().addOnSuccessListener {
                val list = ArrayList<Map<String, String>>()
                strengthData.clear()
                it.documents.forEach {
                    val data = it.toObject(Strength::class.java) ?: return@forEach
                    strengthData.add(data)
                    list.add(mapOf("title" to data.date.toString(), "content" to data.username))

                }
                val intent = Intent(this, BluetoothList::class.java)
                intent.putExtra("extra_list", list)
                launchStrengthList.launch(intent)

            }
        }

    }

    private suspend fun timer1() {
        val list = ArrayList<Double>()
        val startTime = Date().time
        while (Date().time-startTime <= 1600) {
            delay(100)
            list.add(currentValue)
        }
        suckmydick.document().set(Strength(username = "dk", strength_data = list))
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }

    private val strengthData = ArrayList<Strength>()

    private val launchStrengthList =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val position = result.data?.extras?.get("position") as Int
                val selectedItem = strengthData.elementAt(position)
                val entryList = ArrayList<Entry>()
                selectedItem.strength_data.forEachIndexed { index, d ->
                    entryList.add(Entry(index.toFloat(), d.toFloat()))
                }

                val dataset = LineDataSet(entryList, "chart")
                chart.data = LineData(dataset)
                chart.invalidate()
            }
        }

    private var launchBluetoothList =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val position = data?.extras?.get("position") as Int
                //Toast.makeText(this, "You Clicked: " + position, Toast.LENGTH_SHORT).show()
                val selectedItem = pairedDevices?.elementAt(position)
                //Toast.makeText(this, "You Clicked: " + selectedItem?.name, Toast.LENGTH_SHORT).show()
                btConnection = ConnectThread(selectedItem)
                btConnection?.start()

            }
        }

    private inner class ConnectThread(device: BluetoothDevice?) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device?.createRfcommSocketToServiceRecord(MY_UUID)
        }
        private val mmInStream: InputStream? = mmSocket?.inputStream
        private val mmBuffer: ByteArray = ByteArray(1024)
        private val device = device

        override fun run() {
            Looper.prepare()
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.

                try {
                    socket.connect()
                    Toast.makeText(
                        this@GripStrength,
                        "Connecting to " + device?.name,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    btnConnectToBT.setText("Disconnect")
                    val sca = Scanner(socket.inputStream)
                    while (true) {
                        val inStr = if (sca.hasNextLine()) sca.nextLine() else null
                        if (startCheck) {
                            if (inStr != null) {
                                val value = inStr.toDouble()
                                arcGauge.value = value
                                currentValue = value
//                                var smd = suckmydick.document()
//                                val strength = Strength(inStr)
//                                smd.set(strength)
                            }
                        }
                    }
                    //btnConnectToBT.setText("Disconnect")
                } catch (ex: IOException) {

                    Toast.makeText(this@GripStrength, "Connection failed!", Toast.LENGTH_SHORT)
                        .show()
                }


            }
            Looper.loop()
        }

        fun checkConnection(): Boolean {
            return mmSocket?.isConnected == true
        }

        // Closes the client socketnd causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Could not close the client socket", e)
            }
        }
    }
}



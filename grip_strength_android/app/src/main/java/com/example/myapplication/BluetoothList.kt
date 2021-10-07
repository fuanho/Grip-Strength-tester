package com.example.myapplication

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.widget.SimpleAdapter
import android.content.Intent
import android.widget.ListView
import android.widget.Toast
import java.io.IOException

import java.util.ArrayList

class BluetoothList : AppCompatActivity() {
    private val btListView: ListView by lazy { findViewById<ListView>(R.id.list_bt_list) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_list)

        val message = intent.extras?.get("extra_list") as ArrayList<Map<String, String>>

        val listAdapter = SimpleAdapter(
            this,
            message,
            android.R.layout.simple_list_item_2,
            arrayOf("title", "content"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        btListView.adapter = listAdapter

        btListView.setOnItemClickListener { parent, view, position, id ->
            val resultIntent = Intent()
            //Toast.makeText(this, "You Clicked: " + position, Toast.LENGTH_SHORT).show()
            resultIntent.putExtra("position", position)
            setResult(Activity.RESULT_OK, resultIntent);
            finish()
        }
    }
}
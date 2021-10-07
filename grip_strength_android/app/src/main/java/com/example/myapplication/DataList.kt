package com.example.myapplication

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.widget.SimpleAdapter
import android.content.Intent
import android.widget.ListView

class DataList : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_list)
    }
}
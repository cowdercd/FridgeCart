package com.example.fridgecart

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.fridgecart.R
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.view.View
import com.example.fridgecart.DeviceInfoModel
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fridgecart.DeviceListAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.snackbar.Snackbar
import java.util.ArrayList

class SelectDeviceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device)

        //Bluetooth Setup
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        //Get List of Paired Bluetooth Devices
        val pairedDevices = bluetoothAdapter.bondedDevices
        val deviceList: MutableList<Any> = ArrayList()
        if(pairedDevices.size > 0) {
            // There are paired devices
            for (device in pairedDevices) {
                val deviceName = device.name
                val deviceHardwareAddress = device.address //mac
                val deviceInfoModel = DeviceInfoModel(deviceName, deviceHardwareAddress)
                deviceList.add(deviceInfoModel)
            }
            /* Display paired device using recyclerView*/
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewDevice)
            recyclerView.layoutManager = LinearLayoutManager(this)
            val deviceListAdapter = DeviceListAdapter(this, deviceList)
            recyclerView.setAdapter(deviceListAdapter)
            recyclerView.itemAnimator = DefaultItemAnimator()
        } else {
            val view = findViewById<View>(R.id.recyclerViewDevice)
            val snackBar = Snackbar.make(
                view,
                "Activate Bluetooth or pair a Bluetooth device",
                Snackbar.LENGTH_INDEFINITE
            )
            snackBar.setAction("Okay") { }
            snackBar.show()
        }
    }
}
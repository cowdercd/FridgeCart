package com.example.fridgecart

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.LinearLayout
import com.example.fridgecart.R
import android.view.ViewGroup
import android.view.LayoutInflater
import com.example.fridgecart.DeviceInfoModel
import android.content.Intent
import android.view.View

class DeviceListAdapter(private val context: Context, private val deviceList: List<Any>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            var textName: TextView
            var textAddress: TextView
            var linearLayout: LinearLayout

            init {
                textName = v.findViewById(R.id.textViewDeviceName)
                textAddress = v.findViewById(R.id.textViewDeviceAddress)
                linearLayout = v.findViewById(R.id.linearLayoutDeviceInfo)
            }
        }
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val v: View = LayoutInflater.from(parent.context).inflate(R.layout.device_info_layout, parent, false)
            return DeviceListAdapter.ViewHolder(v)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val itemHolder: DeviceListAdapter.ViewHolder = holder as ViewHolder
            val deviceInfoModel = deviceList[position] as DeviceInfoModel
            itemHolder.textName.setText(deviceInfoModel.getDeviceHardwareAddress())

            /* When a device is selected */
            itemHolder.linearLayout.setOnClickListener(View.OnClickListener {
                val intent = Intent(context, MainActivity::class.java)
                /* Send Device Details to Main Activity */
                intent.putExtra("deviceName", deviceInfoModel.getDeviceName())
                intent.putExtra("deviceAddress", deviceInfoModel.getDeviceHardwareAddress())
                /* Call Main Activity */
                context.startActivity(intent)
            })
        }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    var rawDeviceListAdapter: View? = null
}

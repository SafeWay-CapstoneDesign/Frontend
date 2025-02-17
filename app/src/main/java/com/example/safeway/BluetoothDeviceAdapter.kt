package com.example.safeway

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BluetoothDeviceAdapter(private val deviceList: List<BluetoothDevice>) :
    RecyclerView.Adapter<BluetoothDeviceAdapter.BluetoothViewHolder>() {

    private var onItemClickListener: ((BluetoothDevice) -> Unit)? = null

    fun setOnItemClickListener(listener: (BluetoothDevice) -> Unit) {
        onItemClickListener = listener
    }

    class BluetoothViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val deviceAddress: TextView = itemView.findViewById(R.id.deviceAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bluetooth_device, parent, false)
        return BluetoothViewHolder(view)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: BluetoothViewHolder, position: Int) {
        val device = deviceList[position]
        holder.deviceName.text = device.name ?: "알 수 없는 기기"
        holder.deviceAddress.text = device.address

        // 아이템 클릭 시 페어링 요청을 호출하도록 이벤트 추가
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(device)
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }
}

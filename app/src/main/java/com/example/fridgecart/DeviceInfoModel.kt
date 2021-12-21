package com.example.fridgecart

class DeviceInfoModel {
    private var deviceName: String? = null
    private var deviceHardwareAddress: String? = null

    constructor() {}
    constructor(deviceName: String?, deviceHardwareAddress: String?) {
        this.deviceName = deviceName
        this.deviceHardwareAddress = deviceHardwareAddress
    }
    fun getDeviceName(): String? {return deviceName}

    fun getDeviceHardwareAddress(): String? {return deviceHardwareAddress}
}
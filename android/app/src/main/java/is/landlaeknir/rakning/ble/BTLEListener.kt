/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package `is`.landlaeknir.rakning.ble


import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi

class BTLEListener(
    context: Context,
    private val bluetoothLeScanner: BluetoothLeScanner
) {
    private val filters = listOf(
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(COLOCATE_SERVICE_UUID))
            .build()
    )

    private val settings = ScanSettings.Builder()
        .setReportDelay(0)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()

    private val scanCallBack = ScanningCallback(context, GattClientCallback())

    fun start() {
        bluetoothLeScanner.startScan(
            filters,
            settings,
            scanCallBack
        )
    }

    fun stop() {
        bluetoothLeScanner.stopScan(scanCallBack)
    }
}


private class ScanningCallback(
    private val context: Context,
    private val gattClientCallBack: GattClientCallback
) : ScanCallback() {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        onResult(result)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBatchScanResults(results: List<ScanResult>) {
        results.forEach { onResult(it) }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun onResult(result: ScanResult) {
        Log.i(
            "Scanning",
            "Received $result"
        )

        Log.i(
            "Scanning",
            "Device Found\n Address: ${result.device.address} \n" +
                    " Name: ${result.device.name} \n" +
                    " Uuids: ${result.device.uuids} \n" +
                    " Type: ${result.device.type}"
        )

        result.device.connectGatt(context, false, gattClientCallBack, TRANSPORT_LE)
    }

    override fun onScanFailed(errorCode: Int) {
        Log.e(
            "Scanning",
            "Scan failed $errorCode"
        )
    }
}

private class GattClientCallback : BluetoothGattCallback() {

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Log.i("onServicesDiscovered", "status: $status")

        if (status == GATT_SUCCESS) {
            gatt.getService(COLOCATE_SERVICE_UUID).getCharacteristic(DEVICE_CHARACTERISTIC_UUID)
                .let {
                    gatt.readCharacteristic(it)
                }
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (characteristic.isDeviceIdentifier()) {
            Log.i(
                "onCharacteristicRead", "Device Identifier: ${String(characteristic.value)}"
            )
        }
    }

    override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int
    ) {
        Log.i(
            "onConnectionStateChange",
            "status: $status, state: $newState"
        )

        if (newState == STATE_CONNECTED) {
            gatt.discoverServices()
        }
    }
}

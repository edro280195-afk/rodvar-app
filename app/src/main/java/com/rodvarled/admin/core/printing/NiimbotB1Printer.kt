package com.rodvarled.admin.core.printing

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.util.UUID

/**
 * Transporte BLE nativo para NIIMBOT B1 y etiquetas cuadradas 50x50 mm.
 *
 * El protocolo usa paquetes 55 55 ... AA AA y el mismo servicio BLE que la
 * integración probada en nenis-bundle. La conexión se conserva durante todo
 * el lote para no perder etiquetas por reconexiones innecesarias.
 */
class NiimbotB1Printer {
    data class Device(val name: String, val address: String)

    companion object {
        private val SERVICE_UUID = UUID.fromString("e7810a71-73ae-499d-8c15-faa9aef0c3f2")
        private val DATA_CHARACTERISTIC_UUID = UUID.fromString("bef8d6c9-9c21-4c9e-b632-bd58c1009f9f")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val RESPONSE_TIMEOUT_MS = 5_000L
        private const val PACKET_INTERVAL_MS = 10L
        private const val STATUS_POLL_INTERVAL_MS = 100L
        private const val REQUESTED_MTU = 247

        fun hasPermission(context: Context): Boolean = if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

        suspend fun scan(context: Context, timeoutMs: Long = 3_500L): List<Device> = withContext(Dispatchers.IO) {
            check(hasPermission(context)) { "Permite Bluetooth para buscar la NIIMBOT." }
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: error("Este dispositivo no tiene Bluetooth.")
            check(adapter.isEnabled) { "Activa el Bluetooth del teléfono." }

            val bonded = adapter.bondedDevices
                .mapNotNull { device ->
                    val name = runCatching { device.name }.getOrNull().orEmpty()
                    if (isNiimbotName(name)) Device(name.ifBlank { "NIIMBOT B1" }, device.address) else null
                }
            if (bonded.isNotEmpty()) return@withContext bonded

            val scanner = adapter.bluetoothLeScanner ?: error("No se pudo iniciar el escaneo Bluetooth.")
            val found = linkedMapOf<String, Device>()
            val callback = object : android.bluetooth.le.ScanCallback() {
                override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
                    val device = result.device
                    val name = result.scanRecord?.deviceName ?: runCatching { device.name }.getOrNull().orEmpty()
                    val normalized = name.uppercase().replace(" ", "")
                    if (isNiimbotName(name)) {
                        found[device.address] = Device(name.ifBlank { "NIIMBOT B1" }, device.address)
                    }
                }
            }

            scanner.startScan(callback)
            delay(timeoutMs)
            scanner.stopScan(callback)
            found.values.toList()
        }

        private fun isNiimbotName(name: String): Boolean {
            val normalized = name.uppercase().replace(Regex("[^A-Z0-9]"), "")
            return normalized.startsWith("B1") || normalized.startsWith("NIIMBOTB1")
        }

        suspend fun print(
            context: Context,
            address: String,
            labels: List<Bitmap>,
            copies: Int = 1,
            onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
        ) {
            require(labels.isNotEmpty()) { "No hay etiquetas para imprimir." }
            check(hasPermission(context)) { "Permite Bluetooth para imprimir." }
            val session = Session(context, address)
            try {
                session.connect()
                session.printBatch(labels, copies, onProgress)
            } finally {
                session.close()
            }
        }
    }

    private class Session(private val context: Context, address: String) : Closeable {
        private val device: BluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
        private var gatt: BluetoothGatt? = null
        private var writeCharacteristic: BluetoothGattCharacteristic? = null
        private var notifyCharacteristic: BluetoothGattCharacteristic? = null
        private var connectWaiter: CompletableDeferred<Unit>? = null
        private var negotiatedMtu = 23
        private var writeWaiter: CompletableDeferred<Unit>? = null
        private var responseWaiter: CompletableDeferred<ByteArray>? = null
        private var expectedResponses: Set<Int>? = null
        private var receiveBuffer = ByteArray(0)
        private var printheadPixels = 384

        private val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    connectWaiter?.completeExceptionally(Exception("La NIIMBOT rechazó la conexión Bluetooth ($status)."))
                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connectWaiter?.completeExceptionally(Exception("La NIIMBOT se desconectó."))
                    responseWaiter?.completeExceptionally(Exception("La NIIMBOT se desconectó."))
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    connectWaiter?.completeExceptionally(Exception("La NIIMBOT no expuso sus servicios BLE."))
                    return
                }
                val service = gatt.getService(SERVICE_UUID)
                val dataCharacteristic = service?.getCharacteristic(DATA_CHARACTERISTIC_UUID)
                writeCharacteristic = dataCharacteristic?.takeIf {
                    it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
                        || it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
                } ?: service?.characteristics?.firstOrNull {
                    it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
                        || it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
                }
                notifyCharacteristic = dataCharacteristic?.takeIf {
                    it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                } ?: service?.characteristics?.firstOrNull {
                    it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                }
                if (writeCharacteristic == null || notifyCharacteristic == null) {
                    connectWaiter?.completeExceptionally(Exception("La impresora no es compatible con NIIMBOT B1."))
                    return
                }
                gatt.setCharacteristicNotification(notifyCharacteristic, true)
                val descriptor = notifyCharacteristic?.getDescriptor(CCCD_UUID)
                if (descriptor == null) {
                    requestMtu(gatt)
                } else {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) requestMtu(gatt)
                else connectWaiter?.completeExceptionally(Exception("No se pudieron activar las notificaciones de la NIIMBOT."))
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) negotiatedMtu = mtu
                connectWaiter?.complete(Unit)
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                consumeIncoming(characteristic.value)
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                consumeIncoming(value)
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) writeWaiter?.complete(Unit)
                else writeWaiter?.completeExceptionally(Exception("La NIIMBOT rechazó un paquete ($status)."))
            }
        }

        suspend fun connect() {
            connectWaiter = CompletableDeferred()
            negotiatedMtu = 23
            @Suppress("DEPRECATION")
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            withTimeout(CONNECT_TIMEOUT_MS) { connectWaiter!!.await() }
            sendAndWait(0xC1, byteArrayOf(1), 0xC2)
            runCatching {
                val model = sendAndWaitForResponses(
                    0x40,
                    byteArrayOf(8),
                    setOf(0x41, 0x42, 0x43, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4F)
                )
                if (model.size >= 2) {
                    val modelId = ((model[0].toInt() and 0xFF) shl 8) or (model[1].toInt() and 0xFF)
                    printheadPixels = if (modelId == 4096) 384 else printheadPixels
                }
            }
        }

        suspend fun printBatch(
            bitmaps: List<Bitmap>,
            copies: Int,
            onProgress: (completed: Int, total: Int) -> Unit
        ) {
            require(bitmaps.isNotEmpty()) { "No hay etiquetas para imprimir." }
            require(bitmaps.size <= 65_535) { "El lote de impresión es demasiado grande." }

            sendAndWait(0x21, byteArrayOf(5), 0x31)
            sendAndWait(0x23, byteArrayOf(1), 0x33)
            sendAndWait(0x01, u16(bitmaps.size) + byteArrayOf(0, 0, 0, 0, 0), 0x02)

            bitmaps.forEachIndexed { index, bitmap ->
                printPage(bitmap, copies)
                onProgress(index + 1, bitmaps.size)
            }

            waitForFinished(bitmaps.size)
            sendAndWait(0xF3, byteArrayOf(1), 0xF4)
        }

        private suspend fun printPage(bitmap: Bitmap, copies: Int) {
            val rows = bitmapToRows(bitmap)
            sendAndWait(0x03, byteArrayOf(1), 0x04)
            sendAndWait(0x13, u16(rows.height) + u16(rows.width) + u16(copies), 0x14)

            rows.rows.forEach { row ->
                if (row.data.isEmpty()) {
                    sendOneWay(0x84, u16(row.startIndex) + byteArrayOf(row.repeat.toByte()))
                } else {
                    val counts = countParts(row.data, printheadPixels)
                    val totalBlackPixels = countBits(row.data, 0, row.data.size)
                    if (totalBlackPixels <= 6) {
                        val indexes = pixelIndexes(row.data)
                        sendOneWay(0x83, u16(row.startIndex) + counts + byteArrayOf(row.repeat.toByte()) + indexes)
                    } else {
                        sendOneWay(0x85, u16(row.startIndex) + counts + byteArrayOf(row.repeat.toByte()) + row.data)
                    }
                }
            }

            sendAndWait(0xE3, byteArrayOf(1), 0xE4)
        }

        private suspend fun waitForFinished(totalPages: Int) {
            repeat(100) {
                delay(STATUS_POLL_INTERVAL_MS)
                val status = sendAndWait(0xA3, byteArrayOf(1), 0xB3)
                val page = if (status.size >= 2) {
                    ((status[0].toInt() and 0xFF) shl 8) or (status[1].toInt() and 0xFF)
                } else 0
                val printProgress = status.getOrNull(2)?.toInt()?.and(0xFF) ?: 0
                val feedProgress = status.getOrNull(3)?.toInt()?.and(0xFF) ?: 0
                if (page >= totalPages && printProgress >= 100 && feedProgress >= 100) return
            }
            error("La NIIMBOT tardó demasiado en confirmar la impresión.")
        }

        private suspend fun sendAndWait(command: Int, data: ByteArray, response: Int): ByteArray {
            return sendAndWaitForResponses(command, data, setOf(response))
        }

        private suspend fun sendAndWaitForResponses(command: Int, data: ByteArray, responses: Set<Int>): ByteArray {
            responseWaiter = CompletableDeferred()
            expectedResponses = responses
            return try {
                write(packet(command, data, command == 0xC1))
                withTimeout(RESPONSE_TIMEOUT_MS) { responseWaiter!!.await() }
            } finally {
                responseWaiter = null
                expectedResponses = null
            }
        }

        private suspend fun sendOneWay(command: Int, data: ByteArray) {
            write(packet(command, data, false))
            delay(10)
        }

        private suspend fun write(bytes: ByteArray) {
            val characteristic = writeCharacteristic ?: error("La impresora no está conectada.")
            check(bytes.size <= negotiatedMtu - 3) {
                "El enlace BLE no negoció un tamaño suficiente (${negotiatedMtu - 3} bytes disponibles)."
            }
            val supportsWriteResponse = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
            writeWaiter = if (supportsWriteResponse) CompletableDeferred() else null
            @Suppress("DEPRECATION")
            characteristic.writeType = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT else BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            characteristic.value = bytes
            @Suppress("DEPRECATION")
            if (gatt?.writeCharacteristic(characteristic) != true) error("No se pudo enviar el paquete a la NIIMBOT.")
            if (supportsWriteResponse) {
                withTimeout(RESPONSE_TIMEOUT_MS) { writeWaiter!!.await() }
            }
            delay(PACKET_INTERVAL_MS)
        }

        private fun consumeIncoming(bytes: ByteArray) {
            receiveBuffer += bytes
            while (receiveBuffer.size >= 7) {
                val start = receiveBuffer.indexOfHeader() ?: run { receiveBuffer = ByteArray(0); return }
                if (start > 0) receiveBuffer = receiveBuffer.copyOfRange(start, receiveBuffer.size)
                if (receiveBuffer.size < 6) return
                val length = receiveBuffer[3].toInt() and 0xFF
                val total = 2 + 1 + 1 + length + 1 + 2
                if (receiveBuffer.size < total) return
                val packet = receiveBuffer.copyOfRange(0, total)
                receiveBuffer = receiveBuffer.copyOfRange(total, receiveBuffer.size)
                val command = packet[2].toInt() and 0xFF
                val data = packet.copyOfRange(4, 4 + length)
                val expected = expectedResponses
                if (command == 0xDB) {
                    responseWaiter?.completeExceptionally(Exception("La NIIMBOT reportó error de impresión: 0x${data.firstOrNull()?.toInt()?.and(0xFF)?.toString(16) ?: "desconocido"}."))
                } else if (expected == null || command in expected) responseWaiter?.complete(data)
            }
        }

        override fun close() {
            runCatching { gatt?.disconnect() }
            runCatching { gatt?.close() }
            gatt = null
        }

        private data class EncodedRow(val startIndex: Int, val data: ByteArray, val repeat: Int)

        private data class EncodedRows(val width: Int, val height: Int, val rows: List<EncodedRow>)

        private fun bitmapToRows(bitmap: Bitmap): EncodedRows {
            val width = bitmap.width
            val height = bitmap.height
            val bytesPerRow = (width + 7) / 8
            val rows = ArrayList<EncodedRow>()
            for (y in 0 until height) {
                val row = ByteArray(bytesPerRow)
                var black = false
                for (x in 0 until width) {
                    val color = bitmap.getPixel(x, y)
                    val luminance = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3
                    if (luminance < 128) {
                        row[x / 8] = (row[x / 8].toInt() or (1 shl (7 - (x % 8)))).toByte()
                        black = true
                    }
                }
                val rowData = if (black) row else ByteArray(0)
                val previous = rows.lastOrNull()
                if (previous != null && previous.repeat < 255 && previous.data.contentEquals(rowData)) {
                    rows[rows.lastIndex] = previous.copy(repeat = previous.repeat + 1)
                } else {
                    rows += EncodedRow(y, rowData, 1)
                }
            }
            return EncodedRows(width, height, rows)
        }

        private fun countParts(data: ByteArray, printheadPixels: Int): ByteArray {
            val total = countBits(data, 0, data.size)
            if (printheadPixels < 384) return byteArrayOf(0, (total and 0xFF).toByte(), (total shr 8).toByte())
            val chunk = (data.size + 2) / 3
            return byteArrayOf(
                countBits(data, 0, minOf(chunk, data.size)).toByte(),
                countBits(data, minOf(chunk, data.size), minOf(chunk * 2, data.size)).toByte(),
                countBits(data, minOf(chunk * 2, data.size), data.size).toByte()
            )
        }

        private fun countBits(data: ByteArray, start: Int, end: Int): Int = (start until end).sumOf { Integer.bitCount(data[it].toInt() and 0xFF) }

        private fun pixelIndexes(data: ByteArray): ByteArray {
            val result = ArrayList<Byte>()
            data.forEachIndexed { byteIndex, value ->
                for (bit in 0 until 8) {
                    if (value.toInt() and (1 shl (7 - bit)) != 0) {
                        val index = byteIndex * 8 + bit
                        result += (index shr 8).toByte()
                        result += index.toByte()
                    }
                }
            }
            return result.toByteArray()
        }

        private fun u16(value: Int): ByteArray = byteArrayOf((value shr 8).toByte(), value.toByte())

        private fun requestMtu(gatt: BluetoothGatt) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && gatt.requestMtu(REQUESTED_MTU)) return
            connectWaiter?.complete(Unit)
        }

        private fun packet(command: Int, data: ByteArray, connect: Boolean): ByteArray {
            var checksum = command xor data.size
            data.forEach { checksum = checksum xor (it.toInt() and 0xFF) }
            val packet = ByteArray(2 + 1 + 1 + data.size + 1 + 2 + if (connect) 1 else 0)
            var offset = 0
            if (connect) packet[offset++] = 0x03
            packet[offset++] = 0x55
            packet[offset++] = 0x55
            packet[offset++] = command.toByte()
            packet[offset++] = data.size.toByte()
            data.copyInto(packet, offset)
            offset += data.size
            packet[offset++] = checksum.toByte()
            packet[offset++] = 0xAA.toByte()
            packet[offset] = 0xAA.toByte()
            return packet
        }

        private fun ByteArray.indexOfHeader(): Int? {
            for (index in 0 until size - 1) if (this[index] == 0x55.toByte() && this[index + 1] == 0x55.toByte()) return index
            return null
        }
    }
}

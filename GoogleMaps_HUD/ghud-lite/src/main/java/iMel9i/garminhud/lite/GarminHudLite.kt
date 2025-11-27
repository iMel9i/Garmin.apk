package iMel9i.garminhud.lite

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothState
import app.akexorcist.bluetotohspp.library.DeviceList

/**
 * Упрощенная реализация протокола Garmin HUD для тестирования
 */
class GarminHudLite(private val context: Context) {
    
    companion object {
        private const val TAG = "GarminHudLite"
        private const val DEBUG = true
        private const val MAX_UPDATES_PER_SECOND = 6
    }
    
    private var bt: BluetoothSPP? = null
    private var updateCount = 0
    private var lastUpdateClearTime = System.currentTimeMillis()
    private var connected = false
    private var connectedDeviceName: String? = null
    private var connectedDeviceAddress: String? = null
    
    var onConnectionStateChanged: ((Boolean, String?) -> Unit)? = null
    
    init {
        // Не инициализируем Bluetooth сразу, чтобы избежать ошибок разрешений
    }
    
    fun initBluetooth() {
        if (bt != null) return
        
        bt = BluetoothSPP(context)
        
        bt?.setBluetoothConnectionListener(object : BluetoothSPP.BluetoothConnectionListener {
            override fun onDeviceConnected(name: String, address: String) {
                Log.d(TAG, "Connected: $name ($address)")
                connected = true
                connectedDeviceName = name
                connectedDeviceAddress = address
                onConnectionStateChanged?.invoke(true, name)
            }
            
            override fun onDeviceDisconnected() {
                Log.d(TAG, "Disconnected")
                connected = false
                connectedDeviceName = null
                connectedDeviceAddress = null
                onConnectionStateChanged?.invoke(false, null)
            }
            
            override fun onDeviceConnectionFailed() {
                Log.d(TAG, "Connection failed")
                connected = false
                connectedDeviceName = null
                connectedDeviceAddress = null
                onConnectionStateChanged?.invoke(false, null)
            }
        })
        
        if (bt?.isServiceAvailable == false) {
            bt?.setupService()
            bt?.startService(BluetoothState.DEVICE_OTHER)
        }
    }
    
    fun connectToDevice(address: String) {
        initBluetooth()
        bt?.connect(address)
    }
    
    fun scanForDevice() {
        initBluetooth()
        
        if (bt?.isBluetoothAvailable == false) {
            Toast.makeText(context, R.string.bt_not_available, Toast.LENGTH_SHORT).show()
            return
        }
        
        bt?.setDeviceTarget(BluetoothState.DEVICE_OTHER)
        val intent = Intent(context, DeviceList::class.java)
        (context as Activity).startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE)
    }
    
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return when (requestCode) {
            BluetoothState.REQUEST_CONNECT_DEVICE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    bt?.connect(data)
                }
                true
            }
            BluetoothState.REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    bt?.setupService()
                    bt?.startService(BluetoothState.DEVICE_OTHER)
                } else {
                    Toast.makeText(context, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> false
        }
    }
    
    fun disconnect() {
        bt?.stopAutoConnect()
        bt?.stopService()
        connected = false
    }
    
    fun isConnected(): Boolean = connected
    
    fun getConnectedDeviceName(): String? = connectedDeviceName
    fun getConnectedDeviceAddress(): String? = connectedDeviceAddress
    
    // ========== Протокол Garmin HUD ==========
    
    private fun isUpdatable(): Boolean {
        val now = System.currentTimeMillis()
        val interval = now - lastUpdateClearTime
        if (interval > 1000) {
            lastUpdateClearTime = now
            updateCount = 0
        }
        return updateCount < MAX_UPDATES_PER_SECOND
    }
    
    private fun toDigit(n: Int): Char {
        val digit = n % 10
        return if (digit == 0) 10.toChar() else digit.toChar()
    }
    
    private fun sendPacket(pBuf: CharArray): Boolean {
        if (!isUpdatable() || bt?.isServiceAvailable != true) {
            return false
        }
        
        updateCount++
        
        val packet = ByteArray(pBuf.size) { pBuf[it].code.toByte() }
        
        if (DEBUG) {
            Log.d(TAG, "Sending packet: ${packet.joinToString(" ") { "%02X".format(it) }}")
        }
        
        bt?.send(packet, false)
        return true
    }
    
    private fun sendToHud(pBuf: CharArray) {
        val sendBuf = CharArray(255)
        var len = 0
        var stuffingCount = 0
        
        // Header
        sendBuf[len++] = 0x10.toChar()
        sendBuf[len++] = 0x7b.toChar()
        sendBuf[len++] = (pBuf.size + 6).toChar()
        
        if (pBuf.size == 0x0a) {
            sendBuf[len++] = 0x10.toChar()
            stuffingCount++
        }
        
        sendBuf[len++] = pBuf.size.toChar()
        sendBuf[len++] = 0x00.toChar()
        sendBuf[len++] = 0x00.toChar()
        sendBuf[len++] = 0x00.toChar()
        sendBuf[len++] = 0x55.toChar()
        sendBuf[len++] = 0x15.toChar()
        
        // Data with byte stuffing
        for (c in pBuf) {
            sendBuf[len++] = c
            if (c.code == 0x10) {
                sendBuf[len++] = 0x10.toChar()
                stuffingCount++
            }
        }
        
        // CRC
        var nCrc = 0
        for (i in 1 until len) {
            nCrc += sendBuf[i].code
        }
        nCrc -= stuffingCount * 0x10
        
        sendBuf[len++] = ((-nCrc) and 0xff).toChar()
        sendBuf[len++] = 0x10.toChar()
        sendBuf[len++] = 0x03.toChar()
        
        sendPacket(sendBuf.copyOf(len))
    }
    
    // ========== Команды HUD ==========
    
    /**
     * Установить время
     */
    fun setTime(hour: Int, minute: Int) {
        val arr = charArrayOf(
            0x05.toChar(),              // Command: Time
            0x00.toChar(),              // Traffic flag
            toDigit(hour / 10),         // Hour tens
            toDigit(hour),              // Hour ones
            0xff.toChar(),              // Colon
            toDigit(minute / 10),       // Minute tens
            toDigit(minute),            // Minute ones
            0x00.toChar(),              // 'h' suffix
            0x00.toChar()               // Flag
        )
        sendToHud(arr)
    }
    
    /**
     * Установить направление (стрелку)
     * @param nDir: направление стрелки (eOutAngle)
     * @param nType: тип (eOutType) - по умолчанию ArrowOnly
     * @param nRoundaboutOut: выход из кругового движения (eOutAngle)
     */
    fun setDirection(nDir: Int, nType: Int = 0x80, nRoundaboutOut: Int = 0x00) {
        val arr = charArrayOf(
            0x01.toChar(),  // Command: Direction
            nType.toChar(), // Type
            if (nType == 0x04 || nType == 0x08) { // LeftRoundabout or RightRoundabout
                if (nRoundaboutOut == 0x00) nDir.toChar() else nRoundaboutOut.toChar()
            } else {
                0x00.toChar()
            },
            nDir.toChar()   // Direction angle
        )
        sendToHud(arr)
    }
    
    /**
     * Установить скорость
     */
    fun setSpeed(speed: Int, showIcon: Boolean = true) {
        val hundredsDigit = if (speed < 10) 0.toChar() else ((speed / 100) % 10).toChar()
        val tensDigit = if (speed < 10) 0.toChar() else toDigit(speed / 10)
        val onesDigit = toDigit(speed)
        
        val arr = charArrayOf(
            0x06.toChar(),      // Command: Speed
            0x00.toChar(),      // Speed hundreds
            0x00.toChar(),      // Speed tens
            0x00.toChar(),      // Speed ones
            0x00.toChar(),      // Slash
            hundredsDigit,      // Limit hundreds
            tensDigit,          // Limit tens
            onesDigit,          // Limit ones
            0x00.toChar(),      // Speeding
            if (showIcon) 0xff.toChar() else 0x00.toChar()  // Icon
        )
        sendToHud(arr)
    }
    
    /**
     * Установить расстояние
     * @param nDist: расстояние в метрах (float)
     * @param unit: единица измерения (1=метры, 3=километры, 5=мили)
     */
    fun setDistance(nDist: Float, unit: Int = 1) {
        // Определяем нужна ли десятичная точка (для км/миль < 10)
        val hasDecimal = (unit == 3 || unit == 5) && nDist < 10
        val distance = if (hasDecimal) (nDist * 10.0f).toInt() else nDist.toInt()
        
        val arr = charArrayOf(
            0x03.toChar(),              // Command: Distance
            toDigit(distance / 1000),   // Thousands
            toDigit(distance / 100),    // Hundreds
            toDigit(distance / 10),     // Tens
            if (hasDecimal) 0xff.toChar() else 0x00.toChar(), // Decimal point
            toDigit(distance),          // Ones
            unit.toChar()               // Unit (1=m, 3=km, 5=mi)
        )
        
        // Убираем ведущие нули
        if (arr[1].code == 0x0a) {
            arr[1] = 0.toChar()
            if (arr[2].code == 0x0a) {
                arr[2] = 0.toChar()
                if (arr[3].code == 0x0a) {
                    arr[3] = 0.toChar()
                }
            }
        }
        
        // Для десятичных показываем ведущий ноль
        if (hasDecimal && (distance / 10) == 0) {
            arr[3] = 0x0a.toChar()
        }
        
        sendToHud(arr)
    }
    
    /**
     * Очистить экран
     */
    fun clear() {
        // Clear direction
        sendToHud(charArrayOf(0x01.toChar(), 0x00.toChar(), 0x00.toChar(), 0x00.toChar()))
        
        // Clear speed
        sendToHud(charArrayOf(
            0x06.toChar(), 0x00.toChar(), 0x00.toChar(), 0x00.toChar(), 0x00.toChar(),
            0x00.toChar(), 0x00.toChar(), 0x00.toChar(), 0x00.toChar(), 0x00.toChar()
        ))
        
        // Clear distance
        sendToHud(charArrayOf(
            0x03.toChar(), 0x00.toChar(), 0x00.toChar(), 0x00.toChar(),
            0x00.toChar(), 0x00.toChar(), 0x00.toChar()
        ))
    }
}

package com.geeksville.mesh

import android.graphics.Color
import android.os.Parcelable
import com.geeksville.mesh.MeshProtos.User

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


//
// model objects that directly map to the corresponding protobufs
//

@Serializable
@Parcelize
data class MeshUser(
    val id: String,
    val longName: String,
    val shortName: String,
    val hwModel: MeshProtos.HardwareModel,
    val isLicensed: Boolean = false,
) : Parcelable {

    override fun toString(): String {
        return "MeshUser(id=${id}, longName=${longName}, shortName=${shortName}, hwModel=${hwModelString}, isLicensed=${isLicensed})"
    }

    fun toProto(): User = User.newBuilder().setId(id).setLongName(longName).setShortName(shortName)
        .setHwModel(hwModel).setIsLicensed(isLicensed).build()

    /** a string version of the hardware model, converted into pretty lowercase and changing _ to -, and p to dot
     * or null if unset
     * */
    val hwModelString: String?
        get() =
            if (hwModel == MeshProtos.HardwareModel.UNSET) null
            else hwModel.name.replace('_', '-').replace('p', '.').lowercase()
}

@Serializable
@Parcelize
data class Position(
    val latitude: Double,
    val longitude: Double,
    val altitude: Int,
    val time: Int = currentTime() // default to current time in secs (NOT MILLISECONDS!)
) : Parcelable {
    companion object {
        /// Convert to a double representation of degrees
        fun degD(i: Int) = i * 1e-7
        fun degI(d: Double) = (d * 1e7).toInt()

        fun currentTime() = (System.currentTimeMillis() / 1000).toInt()
    }

    /** Create our model object from a protobuf.  If time is unspecified in the protobuf, the provided default time will be used.
     */
    constructor(p: MeshProtos.Position, defaultTime: Int = currentTime()) : this(
        // We prefer the int version of lat/lon but if not available use the depreciated legacy version
        degD(p.latitudeI),
        degD(p.longitudeI),
        p.altitude,
        if (p.time != 0) p.time else defaultTime
    )

    // If GPS gives a crap position don't crash our app
    fun isValid(): Boolean {
        return latitude != 0.0 && longitude != 0.0 &&
                (latitude >= -90 && latitude <= 90.0) &&
                (longitude >= -180 && longitude <= 180)
    }

    override fun toString(): String {
        return "Position(lat=${latitude}, lon=${longitude}, alt=${altitude}, time=${time})"
    }
}


@Serializable
@Parcelize
data class DeviceMetrics(
    val time: Int = currentTime(), // default to current time in secs (NOT MILLISECONDS!)
    val batteryLevel: Int = 0,
    val voltage: Float,
    val channelUtilization: Float,
    val airUtilTx: Float
) : Parcelable {
    companion object {
        fun currentTime() = (System.currentTimeMillis() / 1000).toInt()
    }

    /** Create our model object from a protobuf.
     */
    constructor(p: TelemetryProtos.DeviceMetrics, telemetryTime: Int = currentTime()) : this(
        telemetryTime,
        p.batteryLevel,
        p.voltage,
        p.channelUtilization,
        p.airUtilTx
    )

    override fun toString(): String {
        return "DeviceMetrics(time=${time}, batteryLevel=${batteryLevel}, voltage=${voltage}, channelUtilization=${channelUtilization}, airUtilTx=${airUtilTx})"
    }
}

@Serializable
@Parcelize
data class EnvironmentMetrics(
    val time: Int = currentTime(), // default to current time in secs (NOT MILLISECONDS!)
    val temperature: Float,
    val relativeHumidity: Float,
    val barometricPressure: Float,
    val gasResistance: Float,
    val voltage: Float,
    val current: Float,
) : Parcelable {
    companion object {
        fun currentTime() = (System.currentTimeMillis() / 1000).toInt()
    }

    /** Create our model object from a protobuf.
     */
    constructor(t: TelemetryProtos.EnvironmentMetrics, telemetryTime: Int = currentTime()) : this(
        telemetryTime,
        t.temperature,
        t.relativeHumidity,
        t.barometricPressure,
        t.gasResistance,
        t.voltage,
        t.current
    )

    override fun toString(): String {
        return "EnvironmentMetrics(time=${time}, temperature=${temperature}, humidity=${relativeHumidity}, pressure=${barometricPressure}), resistance=${gasResistance}, voltage=${voltage}, current=${current}"
    }
}

@Serializable
@Parcelize
data class NodeInfo(
    val num: Int, // This is immutable, and used as a key
    var user: MeshUser? = null,
    var position: Position? = null,
    var snr: Float = Float.MAX_VALUE,
    var rssi: Int = Int.MAX_VALUE,
    var lastHeard: Int = 0, // the last time we've seen this node in secs since 1970
    var deviceMetrics: DeviceMetrics? = null,
    var environmentMetrics: EnvironmentMetrics? = null,
) : Parcelable {

    val colors: Pair<Int, Int>
        get() { // returns foreground and background @ColorInt for each 'num'
            val r = (num and 0xFF0000) shr 16
            val g = (num and 0x00FF00) shr 8
            val b = num and 0x0000FF
            val brightness = ((r * 0.299) + (g * 0.587) + (b * 0.114)) / 255
            return (if (brightness > 0.5) Color.BLACK else Color.WHITE) to Color.rgb(r, g, b)
        }

    val batteryLevel get() = deviceMetrics?.batteryLevel
    val voltage get() = deviceMetrics?.voltage
    val batteryStr get() = if (batteryLevel in 1..100) String.format("%d%%", batteryLevel) else ""

    /**
     * true if the device was heard from recently
     *
     * Note: if a node has never had its time set, it will have a time of zero.  In that
     * case assume it is online - so that we will start sending GPS updates
     */
    val isOnline: Boolean
        get() {
            val now = System.currentTimeMillis() / 1000
            // FIXME - use correct timeout from the device settings
            val timeout =
                15 * 60 // Don't set this timeout too tight, or otherwise we will stop sending GPS helper positions to our device
            return (now - lastHeard <= timeout) || lastHeard == 0
        }

    /// return the position if it is valid, else null
    val validPosition: Position?
        get() {
            return position?.takeIf { it.isValid() }
        }
}

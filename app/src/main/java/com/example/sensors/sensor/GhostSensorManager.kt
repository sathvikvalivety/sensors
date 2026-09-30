package com.example.sensors.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.sensors.engine.GhostScoreEngine
import com.example.sensors.model.SensorData
import kotlin.math.abs
import kotlin.math.sqrt

class GhostSensorManager(
    private val context: Context,
    private val onTelemetryUpdate: (SensorData) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Sensors
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    // Running State
    private var currentData = SensorData()
    private var isRunning = false

    // Baselines & calibration
    private var magBaseline = 45f
    private var lightBaseline = 150f

    val availableSensorsCount: Int
        get() {
            var count = 0
            if (magnetometer != null) count++
            if (accelerometer != null) count++
            if (gyroscope != null) count++
            if (lightSensor != null) count++
            if (proximitySensor != null) count++
            return count
        }

    fun start() {
        if (isRunning) return
        isRunning = true

        val delay = SensorManager.SENSOR_DELAY_UI
        magnetometer?.let { sensorManager.registerListener(this, it, delay) }
        accelerometer?.let { sensorManager.registerListener(this, it, delay) }
        gyroscope?.let { sensorManager.registerListener(this, it, delay) }
        lightSensor?.let { sensorManager.registerListener(this, it, delay) }
        proximitySensor?.let { sensorManager.registerListener(this, it, delay) }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        sensorManager.unregisterListener(this)
    }

    fun updateSoundTelemetry(amplitude: Float, decibels: Float, score: Int) {
        val composite = GhostScoreEngine.calculateCompositeScore(
            magneticScore = currentData.magneticScore,
            movementScore = currentData.movementScore,
            rotationScore = currentData.rotationScore,
            lightScore = currentData.lightScore,
            soundScore = score,
            proximityScore = currentData.proximityScore
        )
        currentData = currentData.copy(
            soundAmplitude = amplitude,
            soundDecibels = decibels,
            soundScore = score,
            ghostScore = composite,
            timestamp = System.currentTimeMillis()
        )
        onTelemetryUpdate(currentData)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x) + (y * y) + (z * z))
                val delta = abs(magnitude - magBaseline)

                // Slow exponential moving average for baseline
                magBaseline = (magBaseline * 0.98f) + (magnitude * 0.02f)

                val score = when {
                    delta > 55f -> 95
                    delta > 30f -> 80
                    delta > 15f -> 55
                    delta > 6f -> 30
                    else -> 10
                }

                currentData = currentData.copy(
                    magneticRawX = x,
                    magneticRawY = y,
                    magneticRawZ = z,
                    magneticMagnitude = magnitude,
                    magneticDelta = delta,
                    magneticScore = score
                )
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x) + (y * y) + (z * z))
                val motionDelta = abs(magnitude - SensorManager.GRAVITY_EARTH)

                val score = when {
                    motionDelta > 12f -> 95
                    motionDelta > 6.0f -> 80
                    motionDelta > 2.5f -> 55
                    motionDelta > 0.8f -> 30
                    else -> 10
                }

                currentData = currentData.copy(
                    accelX = x,
                    accelY = y,
                    accelZ = z,
                    accelMotionMagnitude = motionDelta,
                    movementScore = score
                )
            }

            Sensor.TYPE_GYROSCOPE -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val rotSpeed = sqrt((x * x) + (y * y) + (z * z))

                val score = when {
                    rotSpeed > 4.5f -> 95
                    rotSpeed > 2.0f -> 80
                    rotSpeed > 0.8f -> 55
                    rotSpeed > 0.2f -> 25
                    else -> 10
                }

                currentData = currentData.copy(
                    gyroX = x,
                    gyroY = y,
                    gyroZ = z,
                    rotationSpeed = rotSpeed,
                    rotationScore = score
                )
            }

            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                val delta = abs(lux - lightBaseline)
                val relativeChange = if (lightBaseline > 1f) delta / lightBaseline else 0f

                lightBaseline = (lightBaseline * 0.95f) + (lux * 0.05f)

                val score = when {
                    lux < 3f && lightBaseline > 20f -> 90 // Sudden blackout
                    relativeChange > 0.7f -> 80
                    relativeChange > 0.4f -> 55
                    relativeChange > 0.2f -> 30
                    else -> 10
                }

                currentData = currentData.copy(
                    lightLux = lux,
                    lightDelta = delta,
                    lightScore = score
                )
            }

            Sensor.TYPE_PROXIMITY -> {
                val dist = event.values[0]
                val maxRange = event.sensor.maximumRange
                val isNear = dist < 4.0f || (dist < maxRange && maxRange > 0)
                val score = if (isNear) 85 else 10

                currentData = currentData.copy(
                    proximityDistanceCm = dist,
                    isNear = isNear,
                    proximityScore = score
                )
            }
        }

        // Recompute overall score
        val composite = GhostScoreEngine.calculateCompositeScore(
            magneticScore = currentData.magneticScore,
            movementScore = currentData.movementScore,
            rotationScore = currentData.rotationScore,
            lightScore = currentData.lightScore,
            soundScore = currentData.soundScore,
            proximityScore = currentData.proximityScore
        )

        currentData = currentData.copy(
            ghostScore = composite,
            timestamp = System.currentTimeMillis()
        )

        onTelemetryUpdate(currentData)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

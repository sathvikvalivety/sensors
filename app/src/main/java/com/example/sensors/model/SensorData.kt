package com.example.sensors.model

/**
 * Encapsulates raw and normalized telemetry from all 6 hardware sensors.
 */
data class SensorData(
    // 1. Magnetometer (EMF)
    val magneticRawX: Float = 0f,
    val magneticRawY: Float = 0f,
    val magneticRawZ: Float = 0f,
    val magneticMagnitude: Float = 0f, // in microteslas (uT)
    val magneticDelta: Float = 0f,     // deviation from baseline
    val magneticScore: Int = 0,        // 0 - 100

    // 2. Accelerometer (Kinetic Movement)
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val accelMotionMagnitude: Float = 0f, // deviation from 1G (9.81 m/s^2)
    val movementScore: Int = 0,           // 0 - 100

    // 3. Gyroscope (Rotational Velocity)
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val rotationSpeed: Float = 0f, // in rad/s
    val rotationScore: Int = 0,    // 0 - 100

    // 4. Ambient Light (Lux)
    val lightLux: Float = 0f,
    val lightDelta: Float = 0f, // sudden change relative to ambient baseline
    val lightScore: Int = 0,    // 0 - 100

    // 5. Microphone / Audio EVP (Decibels / Amplitude)
    val soundAmplitude: Float = 0f, // 0.0 - 1.0 normalized or dB
    val soundDecibels: Float = 0f,
    val soundScore: Int = 0,        // 0 - 100

    // 6. Proximity Sensor (Distance / Near-Far)
    val proximityDistanceCm: Float = 5f,
    val isNear: Boolean = false,
    val proximityScore: Int = 0,    // 0 or 100

    // Overall Computed Ghost Score
    val ghostScore: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

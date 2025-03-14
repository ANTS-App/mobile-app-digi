package com.example.attendanceapp

import java.util.*
import kotlin.random.Random

class PinGenerator {
    companion object {
        // Generate a random PIN with specified length
        private fun generatePin(length: Int = 6): String {
            // Use random to generate a PIN
            val random = Random
            val pin = StringBuilder()

            repeat(length) {
                pin.append(random.nextInt(10))
            }

            return pin.toString()
        }

        // Generate PIN with expiry time
        fun generatePinWithExpiry(expiryMinutes: Int = 1, length: Int = 6): Pair<String, Date> {
            val pin = generatePin(length)

            // Calculate expiry time
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, expiryMinutes)
            val expiryTime = calendar.time

            return Pair(pin, expiryTime)
        }
    }
}

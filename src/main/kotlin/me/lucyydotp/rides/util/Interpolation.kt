package me.lucyydotp.rides.util

public fun lerp(first: Double, second: Double, value: Double): Double =
    first + ((second - first) * value)

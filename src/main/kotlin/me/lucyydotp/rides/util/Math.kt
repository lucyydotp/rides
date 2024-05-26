package me.lucyydotp.rides.util

import kotlin.math.absoluteValue
import kotlin.math.sign

public fun Int.ceilDiv(other: Int): Int {
    return floorDiv(other) + rem(other).sign.absoluteValue
}

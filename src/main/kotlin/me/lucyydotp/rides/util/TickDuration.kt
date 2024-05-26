package me.lucyydotp.rides.util

@JvmInline
public value class TickDuration(public val ticks: Int) {
    public operator fun plus(other: TickDuration): TickDuration = TickDuration(ticks + other.ticks)
}

public val Int.ticks: TickDuration
    get() = TickDuration(this)

public val Int.seconds: TickDuration
    get() = TickDuration(this * 20)

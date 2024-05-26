package me.lucyydotp.rides.sequence

import me.lucyydotp.rides.util.Point
import me.lucyydotp.rides.util.TickDuration
import org.jetbrains.annotations.Contract


public class RideSequenceBuilder private constructor(
    private val points: RideSequence,
) {
    public constructor(firstPoint: Point) : this(mutableListOf(firstPoint to 0))

    public infix fun Point.over(ticks: TickDuration) {
        points += this to ticks.ticks
    }

    @Contract("_, _, _ -> new", pure = true)
    public fun move(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Point =
        points.last().first.let {
            Point(
                it.x + x,
                it.y + y,
                it.z + z,
            )
        }

    public fun build(): RideSequence = points
}

public inline fun rideSequence(startPoint: Point, builder: RideSequenceBuilder.() -> Unit): RideSequence =
    RideSequenceBuilder(startPoint).apply(builder).build()

public fun test() {

}

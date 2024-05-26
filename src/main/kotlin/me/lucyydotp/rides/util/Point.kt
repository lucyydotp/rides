package me.lucyydotp.rides.util

import org.bukkit.Location
import org.bukkit.World

/**
 * A location and rotation in 3d space.
 */
public data class Point(
    val x: Double,
    val y: Double,
    val z: Double,
    val pitch: Double? = null,
    val yaw: Double? = null,
) {

    public fun plus(x: Double, y: Double, z: Double): Point = copy(
        x = this.x + x,
        y = this.y + y,
        z = this.z + z,
    )

    public fun lerp(point: Point, dist: Double): Point = Point(
        lerp(x, point.x, dist),
        lerp(y, point.y, dist),
        lerp(z, point.z, dist),
        0.0,
        0.0,
    )

    /**
     * Gets this point as a Bukkit location.
     */
    public fun asLocation(world: World? = null): Location =
        Location(world, x, y, z, pitch?.toFloat() ?: 0f, yaw?.toFloat() ?: 0f)
}

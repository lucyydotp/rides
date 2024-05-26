package me.lucyydotp.rides.sequence

import io.papermc.paper.entity.TeleportFlag
import me.lucyydotp.rides.util.Point
import me.lucyydotp.rides.util.ceilDiv
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.craftbukkit.entity.CraftDisplay
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f

public abstract class SequenceRunner {
    protected abstract fun run(player: Player, ride: Display, sequence: RideSequence)

    public fun run(player: Player, sequence: RideSequence) {
        val ride = player.world.spawn(sequence.first().first.asLocation(), BlockDisplay::class.java) {
            it.isPersistent = false
            it.block = Bukkit.createBlockData(Material.END_ROD)
            it.brightness = Display.Brightness(15, 15)
            it.addPassenger(player)
            it.transformation = Transformation(
                Vector3f(-0.5f, -0.5f, -0.5f),
                Quaternionf(),
                Vector3f(1.0f, 1.0f, 1.0f),
                Quaternionf()
            )
        }

        this.run(player, ride, sequence)
    }
}

public class SimpleRunner(private val plugin: Plugin) : SequenceRunner() {
    protected override fun run(player: Player, ride: Display, sequence: RideSequence) {
        var ticks = 0

        sequence.asSequence()
            .drop(1)
            .forEach { (point, time) ->
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    ride.teleportDuration = time
                    val location = (point.plus(0.5, 0.5, 0.5)).asLocation(ride.world)
                    plugin.logger.info("Moving to $location over $time ticks")
                    ride.teleport(location, TeleportFlag.EntityState.RETAIN_PASSENGERS)
                }, ticks.toLong())
                ticks += time
            }

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            ride.remove()
        }, ticks.toLong())
    }
}

public class TickingRunner(private val plugin: Plugin, private val interval: Int = 1) : SequenceRunner() {

    private data class InterpolatedPoint(
        val tick: Int,
        val point: Point,
        val duration: Int,
    )

    private fun MutableList<InterpolatedPoint>.interpolatePoints(from: Point, to: Pair<Point, Int>, startTick: Int) {
        val steps = to.second.ceilDiv(interval)

        var lastTickTime = 0
        for (i in 1..steps) {
            val relativeTick = (i * interval).coerceAtMost(to.second)
            val tickIncrement = relativeTick - lastTickTime

            val nextPoint = from.lerp(to.first, relativeTick / to.second.toDouble())

            this += InterpolatedPoint(
                startTick + lastTickTime,
                nextPoint,
                tickIncrement
            )
            lastTickTime = relativeTick
        }
    }

    private fun interpolate(sequence: RideSequence) = buildList {
        var last = sequence.first().first

        sequence.fold(0) { acc, it ->
            interpolatePoints(last, it, acc)
            last = it.first
            acc + it.second
        }
    }

    protected override fun run(player: Player, ride: Display, sequence: RideSequence) {
        val points = interpolate(sequence)
        val finalTick = points.last().let { it.tick + it.duration }
        val pointMap = points.associateBy { it.tick }

        var tick = 0

        lateinit var task: BukkitTask
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            pointMap[tick]?.let { (_, point, duration) ->

                Bukkit.broadcast(Component.text("$tick: Teleporting to ${point.x}, ${point.y}, ${point.z} over $duration ticks"))
                ride.teleportDuration = 0
                ride.teleportDuration = duration
                ride.teleportAsync(
                    point.asLocation(ride.world).add(0.5, 0.5, 0.5),
                    PlayerTeleportEvent.TeleportCause.PLUGIN,
                    TeleportFlag.EntityState.RETAIN_PASSENGERS
                )

                (ride as CraftDisplay).handle
            }
            if (tick == finalTick) {
                Bukkit.broadcast(Component.text("$tick: Finished"))
                ride.remove()
                task.cancel()
            }
            tick++
        }, 0, 1)
    }
}

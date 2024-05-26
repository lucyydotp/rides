package me.lucyydotp.rides.sequence

import io.papermc.paper.entity.TeleportFlag
import me.lucyydotp.rides.util.Point
import me.lucyydotp.rides.util.ceilDiv
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

public abstract class SequenceRunner {
    protected abstract fun run(player: Player, ride: Display, sequence: RideSequence)

    public fun run(player: Player, sequence: RideSequence) {
        val ride = player.world.spawn(sequence.first().first.asLocation(), ItemDisplay::class.java) {
            it.isPersistent = false
            it.itemStack = ItemStack(Material.END_ROD)
            it.addPassenger(player)
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

        val lastTick = points.last().tick

        val pointMap = points.associateBy { it.tick }

        var tick = 0

        lateinit var task: BukkitTask
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            pointMap[tick]?.let { (_, point, duration) ->
                ride.teleportDuration = duration + 1
                Bukkit.broadcast(Component.text("$tick: Teleporting to ${point.x}, ${point.y}, ${point.z} over $duration ticks"))
                ride.teleport(
                    point.asLocation(ride.world).add(0.5, 0.5, 0.5),
                    TeleportFlag.EntityState.RETAIN_PASSENGERS
                )
            }
            if (tick == lastTick) {
                Bukkit.broadcast(Component.text("Finished"))
                ride.remove()
                task.cancel()
            }
            tick++
        }, 0, 1)
    }
}

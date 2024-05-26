package me.lucyydotp.rides.sequence

import io.papermc.paper.entity.TeleportFlag
import me.lucyydotp.rides.util.Point
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import kotlin.math.max

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

    // TODO: this is awful and doesn't work properly, rewrite it
    private fun interpolate(sequence: RideSequence) = buildList<Triple<Int, Point, Int>> {
        var tick = 0
        var lastPoint = sequence.first().first
        sequence.forEach { (point, time) ->
            if (time < interval) {
                add(Triple(tick, point, interval * (time / interval)))
            } else {
                var interpolatedTicks = 0
                do {
                    interpolatedTicks += interval
                    add(
                        Triple(
                            tick + interpolatedTicks,
                            lastPoint.lerp(point, interpolatedTicks / time.toDouble()),
                            interval * (max(interval, time - interpolatedTicks) / interval)
                        )
                    )
                } while (interpolatedTicks < time)
            }
            lastPoint = point
            tick += time
        }

    }

    protected override fun run(player: Player, ride: Display, sequence: RideSequence) {
        val points = interpolate(sequence)

        val lastTick = points.last().first

        val pointMap = points.associateBy { it.first }

        var tick = 0

        lateinit var task: BukkitTask
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            pointMap[tick++]?.let { (tick, point, duration) ->
                ride.teleportDuration = duration
                Bukkit.broadcast(Component.text("Teleporting to ${point.x}, ${point.y}, ${point.z}"))
                ride.teleport(
                    point.asLocation(ride.world).add(0.5, 0.5, 0.5),
                    TeleportFlag.EntityState.RETAIN_PASSENGERS
                )
            }
            if (tick == lastTick) {
                Bukkit.broadcast(Component.text("Finished, cancelling task"))
                ride.remove()
                task.cancel()
            }
        }, 1, 1)
    }
}

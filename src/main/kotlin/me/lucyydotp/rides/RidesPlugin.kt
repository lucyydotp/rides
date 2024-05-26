package me.lucyydotp.rides

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.lucyydotp.rides.sequence.TickingRunner
import me.lucyydotp.rides.sequence.rideSequence
import me.lucyydotp.rides.util.Point
import me.lucyydotp.rides.util.seconds
import me.lucyydotp.rides.util.ticks
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

public class RidesPlugin : JavaPlugin() {

    private val testSequence = rideSequence(Point(0.0, -54.0, 0.0)) {
        // Start
        move(z = -3.0) over 30.ticks

        // Climb
        move(z = -6.0, y = 6.0) over 60.ticks
        move(z = -2.0, y = 2.0) over 40.ticks

        // Turn
        move(z = -3.0) over 60.ticks
        move(x = -3.0) over 60.ticks
        move(z = 3.0) over 60.ticks

        // wait for a half second
        move() over 1.seconds

        // drop
        move(z = 2.0, y = -2.0) over 10.ticks
        move(z = 6.0, y = -6.0) over 10.ticks

        move(z = 4.0) over 20.ticks
        move(z = 4.0) over 40.ticks
        move(z = 3.0) over 40.ticks

        move(x = 3.0) over 40.ticks
        move(z = -8.0) over 100.ticks
    }

    private val tickingRunner = TickingRunner(this, 15)


    override fun onEnable() {
        @Suppress("UnstableApiUsage")
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(Commands.literal("ride").executes { ctx ->
                val player = (ctx.source.executor as? Player) ?: return@executes 1
                tickingRunner.run(player, testSequence)
                0
            }.build())
        }
    }
}

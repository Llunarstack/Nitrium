package dev.nitrium.pregen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /nitrium pregen <radius>} — pre-generate a square of chunks around the command source.
 * {@code /nitrium pregen stop} and {@code /nitrium pregen status} manage the running job.
 */
public final class PregenCommand {
	private PregenCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("nitrium")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("pregen")
						.then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
								.executes(context -> start(context.getSource(), IntegerArgumentType.getInteger(context, "radius"))))
						.then(Commands.literal("stop").executes(context -> stop(context.getSource())))
						.then(Commands.literal("status").executes(context -> status(context.getSource())))));
	}

	private static int start(CommandSourceStack source, int radius) {
		ServerLevel level = source.getLevel();
		Vec3 position = source.getPosition();
		int centerX = Math.floorDiv((int) Math.floor(position.x), 16);
		int centerZ = Math.floorDiv((int) Math.floor(position.z), 16);

		boolean started = PregenManager.get().start(level, centerX, centerZ, radius, source::sendSystemMessage);
		if (!started) {
			source.sendFailure(Component.literal("A Nitrium pre-gen job is already running. Use /nitrium pregen stop."));
			return 0;
		}

		int side = radius * 2 + 1;
		int total = side * side;
		source.sendSuccess(() -> Component.literal(
				"Nitrium pre-gen started: " + total + " chunks (radius " + radius + ") around chunk " + centerX + ", " + centerZ + "."), true);
		return 1;
	}

	private static int stop(CommandSourceStack source) {
		if (!PregenManager.get().isActive()) {
			source.sendFailure(Component.literal("No Nitrium pre-gen job is running."));
			return 0;
		}
		PregenManager.get().stop();
		return 1;
	}

	private static int status(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal(PregenManager.get().status()), false);
		return 1;
	}
}

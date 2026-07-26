package shelf.paster.client.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import shelf.paster.Skycore;
import shelf.paster.client.command.CmdFeedback;
import shelf.paster.client.command.Command;
import shelf.paster.client.waypoint.Waypoint;

public final class GpsCommand extends Command {
    public GpsCommand() {
        super("gps");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(ctx -> {
            CmdFeedback.info("usage: .gps <x> <z> | player <name> <x> <z> | player remove | remove");
            return 1;
        });
        builder.then(arg("X", IntegerArgumentType.integer()).then(arg("Z", IntegerArgumentType.integer()).executes(ctx -> {
            int x = ctx.getArgument("X", Integer.class);
            int z = ctx.getArgument("Z", Integer.class);
            Skycore.get().waypoints().set(new Waypoint(x, z));
            CmdFeedback.success("gps → " + x + ", " + z);
            return 1;
        })));
        builder.then(literal("player").then(arg("name", StringArgumentType.word())
                .then(arg("X", IntegerArgumentType.integer()).then(arg("Z", IntegerArgumentType.integer()).executes(ctx -> {
                    String name = ctx.getArgument("name", String.class);
                    int x = ctx.getArgument("X", Integer.class);
                    int z = ctx.getArgument("Z", Integer.class);
                    Skycore.get().waypoints().setPlayerWaypoint(new Waypoint(name, x, z));
                    CmdFeedback.success("gps player " + name + " → " + x + ", " + z);
                    return 1;
                })))));
        builder.then(literal("player").then(literal("remove").executes(ctx -> {
            if (Skycore.get().waypoints().isEmptyPlayerWaypoint()) {
                CmdFeedback.info("no player gps");
                return 0;
            }
            Skycore.get().waypoints().clearPlayerWaypoint();
            CmdFeedback.success("player gps cleared");
            return 1;
        })));
        builder.then(literal("remove").executes(ctx -> {
            if (Skycore.get().waypoints().isEmpty()) {
                CmdFeedback.info("no gps");
                return 0;
            }
            Skycore.get().waypoints().clear();
            CmdFeedback.success("gps cleared");
            return 1;
        }));
    }
}

package shelf.paster.client.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.lwjgl.glfw.GLFW;
import shelf.paster.Skycore;
import shelf.paster.client.command.CmdFeedback;
import shelf.paster.client.command.Command;
import shelf.paster.client.macro.MacroManager;

import java.util.List;
import java.util.Locale;

public final class MacroCommand extends Command {
    public MacroCommand() {
        super("macro");
    }

    @Override
    public void register(com.mojang.brigadier.CommandDispatcher<ClientSuggestionProvider> dispatcher) {
        for (String alias : List.of("macro", "mac")) {
            LiteralArgumentBuilder<ClientSuggestionProvider> builder = LiteralArgumentBuilder.literal(alias);
            build(builder);
            dispatcher.register(builder);
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(ctx -> {
            CmdFeedback.info("usage: .macro add <name> <key> <text...> | remove <name> | list | clear");
            return 1;
        });
        builder.then(literal("add")
                .then(arg("name", StringArgumentType.word())
                        .then(arg("bind", StringArgumentType.word())
                                .then(arg("text", StringArgumentType.greedyString()).executes(ctx -> {
                                    String name = ctx.getArgument("name", String.class);
                                    String bind = ctx.getArgument("bind", String.class);
                                    String text = ctx.getArgument("text", String.class);
                                    int key = resolveKey(bind);
                                    if (key <= 0) {
                                        CmdFeedback.error("key " + bind + " not found");
                                        return 0;
                                    }
                                    Skycore.get().macros().addMacro(name, key, text);
                                    CmdFeedback.success("macro " + name + " → " + bind.toUpperCase(Locale.ROOT));
                                    return 1;
                                })))));
        builder.then(literal("remove").then(arg("name", StringArgumentType.word()).executes(ctx -> {
            String name = ctx.getArgument("name", String.class);
            if (Skycore.get().macros().find(name) == null) {
                CmdFeedback.error("macro " + name + " not found");
                return 0;
            }
            Skycore.get().macros().removeMacro(name);
            CmdFeedback.success("removed macro " + name);
            return 1;
        })));
        builder.then(literal("list").executes(ctx -> {
            var macros = Skycore.get().macros().getMacros();
            if (macros.isEmpty()) {
                CmdFeedback.info("no macros");
                return 1;
            }
            for (MacroManager.MacroEntry macro : macros) {
                String key = GLFW.glfwGetKeyName(macro.keyCode(), 0);
                CmdFeedback.send(net.minecraft.network.chat.Component.literal(macro.name() + " · "
                        + (key != null ? key.toUpperCase(Locale.ROOT) : macro.keyCode()) + " · " + macro.command()));
            }
            return 1;
        }));
        builder.then(literal("clear").executes(ctx -> {
            Skycore.get().macros().clear();
            CmdFeedback.success("cleared macros");
            return 1;
        }));
    }

    private static int resolveKey(String keyName) {
        for (int i = GLFW.GLFW_KEY_SPACE; i <= GLFW.GLFW_KEY_LAST; i++) {
            String name = GLFW.glfwGetKeyName(i, 0);
            if (name != null && name.equalsIgnoreCase(keyName)) {
                return i;
            }
        }
        return -1;
    }
}

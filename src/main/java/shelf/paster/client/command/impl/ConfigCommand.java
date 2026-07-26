package shelf.paster.client.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.MutableComponent;
import shelf.paster.Skycore;
import shelf.paster.client.command.CmdFeedback;
import shelf.paster.client.command.Command;
import shelf.paster.client.config.ConfigManager;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;

public final class ConfigCommand extends Command {
    public ConfigCommand() {
        super("cfg");
    }

    @Override
    public void register(com.mojang.brigadier.CommandDispatcher<ClientSuggestionProvider> dispatcher) {
        LiteralArgumentBuilder<ClientSuggestionProvider> cfg = LiteralArgumentBuilder.literal(getName());
        build(cfg);
        dispatcher.register(cfg);

        LiteralArgumentBuilder<ClientSuggestionProvider> config = LiteralArgumentBuilder.literal("config");
        build(config);
        dispatcher.register(config);
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(ctx -> {
            CmdFeedback.info("usage: .cfg save|load|remove <name> | list | dir | clear | reset");
            return 1;
        });
        builder.then(literal("save").then(arg("name", StringArgumentType.word()).executes(ctx -> {
            String name = ctx.getArgument("name", String.class);
            Skycore.get().configs().saveConfig(name);
            CmdFeedback.success("saved \"" + name + "\"");
            return 1;
        })));
        builder.then(literal("load").then(arg("name", StringArgumentType.word()).executes(ctx -> {
            String name = ctx.getArgument("name", String.class);
            ConfigManager configs = Skycore.get().configs();
            if (!configs.configExists(name)) {
                CmdFeedback.error("config \"" + name + "\" not found");
                return 0;
            }
            if (!configs.loadConfig(name)) {
                CmdFeedback.error("failed to load \"" + name + "\"");
                return 0;
            }
            CmdFeedback.success("loaded \"" + name + "\"");
            return 1;
        })));
        builder.then(literal("remove").then(arg("name", StringArgumentType.word()).executes(ctx -> {
            String name = ctx.getArgument("name", String.class);
            if (Skycore.get().configs().getConfigInfo(name) == null) {
                CmdFeedback.error("config \"" + name + "\" not found");
                return 0;
            }
            Skycore.get().configs().deleteConfig(name);
            CmdFeedback.success("removed \"" + name + "\"");
            return 1;
        })));
        builder.then(literal("list").executes(ctx -> {
            List<String> names = Skycore.get().configs().getConfigNames();
            if (names.isEmpty()) {
                CmdFeedback.info("no configs");
                return 1;
            }
            CmdFeedback.info("configs:");
            for (String name : names) {
                MutableComponent line = net.minecraft.network.chat.Component.literal(name)
                        .withStyle(net.minecraft.ChatFormatting.WHITE);
                line.append(CmdFeedback.suggest("  load", ".cfg load " + name, 0x7AD7F0, "Load " + name));
                line.append(CmdFeedback.suggest("  del", ".cfg remove " + name, 0xFF8A80, "Delete " + name));
                CmdFeedback.send(line);
            }
            return 1;
        }));
        builder.then(literal("dir").executes(ctx -> {
            openDir(Skycore.get().configs());
            return 1;
        }));
        builder.then(literal("clear").executes(ctx -> {
            for (String name : Skycore.get().configs().getConfigNames()) {
                Skycore.get().configs().deleteConfig(name);
            }
            CmdFeedback.success("cleared config list");
            return 1;
        }));
        builder.then(literal("reset").executes(ctx -> {
            Skycore.get().configs().resetToDefaults();
            CmdFeedback.success("reset modules to defaults");
            return 1;
        }));
    }

    private static void openDir(ConfigManager configs) {
        try {
            File directory = configs.getConfigDirectory();
            directory.mkdirs();
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(directory);
            } else {
                Runtime.getRuntime().exec(new String[]{"explorer", directory.getAbsolutePath()});
            }
            CmdFeedback.info(directory.getAbsolutePath());
        } catch (IOException exception) {
            CmdFeedback.error("cannot open folder: " + exception.getMessage());
        }
    }
}

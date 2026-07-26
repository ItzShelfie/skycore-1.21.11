package shelf.paster.client.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import shelf.paster.Skycore;
import shelf.paster.client.command.CmdFeedback;
import shelf.paster.client.command.Command;
import shelf.paster.client.module.Module;
import shelf.paster.client.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class BindCommand extends Command {
    public BindCommand() {
        super("bind");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(ctx -> {
            CmdFeedback.info("usage: .bind add <module> <key> | remove <module> | list | clear");
            return 1;
        });
        builder.then(literal("add").then(arg("module", StringArgumentType.greedyString())
                .suggests(this::suggestModules)
                .executes(this::add)));
        builder.then(literal("remove").then(arg("module", StringArgumentType.greedyString())
                .suggests(this::suggestModules)
                .executes(this::remove)));
        builder.then(literal("list").executes(this::list));
        builder.then(literal("clear").executes(this::clear));
    }

    private int add(CommandContext<ClientSuggestionProvider> ctx) {
        String rest = ctx.getArgument("module", String.class).trim();
        String[] parts = rest.split("\\s+");
        if (parts.length < 2) {
            CmdFeedback.info("example: .bind add Sprint R");
            return 0;
        }
        String moduleName = parts[0];
        String keyName = parts[1].toUpperCase(Locale.ROOT);
        int key = resolveKey(keyName);
        if (key <= 0) {
            CmdFeedback.error("key " + keyName + " not found");
            return 0;
        }
        Module module = resolveModule(moduleName);
        if (module == null) {
            CmdFeedback.error("module " + moduleName + " not found");
            return 0;
        }
        module.setKeyBind(key);
        CmdFeedback.success("bound " + keyName + " → " + module.getName());
        return 1;
    }

    private int remove(CommandContext<ClientSuggestionProvider> ctx) {
        Module module = resolveModule(ctx.getArgument("module", String.class));
        if (module == null) {
            CmdFeedback.error("module not found");
            return 0;
        }
        module.setKeyBind(-1);
        CmdFeedback.success("unbound " + module.getName());
        return 1;
    }

    private int list(CommandContext<ClientSuggestionProvider> ctx) {
        boolean any = false;
        for (Module module : Skycore.get().modules().getModules()) {
            if (module.getKeyBind() > 0) {
                any = true;
                String key = GLFW.glfwGetKeyName(module.getKeyBind(), 0);
                CmdFeedback.send(Component.literal(module.getName()).withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(key != null ? key.toUpperCase(Locale.ROOT) : String.valueOf(module.getKeyBind()))
                                .withStyle(ChatFormatting.AQUA)));
            }
        }
        if (!any) {
            CmdFeedback.info("no binds");
        }
        return 1;
    }

    private int clear(CommandContext<ClientSuggestionProvider> ctx) {
        for (Module module : Skycore.get().modules().getModules()) {
            module.setKeyBind(-1);
        }
        CmdFeedback.success("cleared all binds");
        return 1;
    }

    private CompletableFuture<Suggestions> suggestModules(CommandContext<ClientSuggestionProvider> ctx, SuggestionsBuilder builder) {
        List<String> names = new ArrayList<>();
        for (Module module : Skycore.get().modules().getModules()) {
            names.add(module.getName());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private int resolveKey(String keyName) {
        for (int i = GLFW.GLFW_KEY_SPACE; i <= GLFW.GLFW_KEY_LAST; i++) {
            String name = GLFW.glfwGetKeyName(i, 0);
            if (name != null && name.equalsIgnoreCase(keyName)) {
                return i;
            }
        }
        return -1;
    }

    private Module resolveModule(String name) {
        ModuleManager manager = Skycore.get().modules();
        Module module = manager.getModule(name);
        if (module != null) {
            return module;
        }
        if ("aura".equalsIgnoreCase(name) || "attackaura".equalsIgnoreCase(name)) {
            return manager.getModule("Aura");
        }
        return null;
    }
}

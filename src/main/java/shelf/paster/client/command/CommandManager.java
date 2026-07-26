package shelf.paster.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import shelf.paster.client.command.impl.BindCommand;
import shelf.paster.client.command.impl.ConfigCommand;
import shelf.paster.client.command.impl.GpsCommand;
import shelf.paster.client.command.impl.MacroCommand;
import shelf.paster.Skycore;

import java.util.ArrayList;
import java.util.List;

public final class CommandManager {
    private final String prefix = ".";
    private final CommandDispatcher<ClientSuggestionProvider> dispatcher = new CommandDispatcher<>();
    private final List<Command> commands = new ArrayList<>();

    public void init() {
        register(new BindCommand());
        register(new ConfigCommand());
        register(new MacroCommand());
        register(new GpsCommand());
    }

    public void register(Command command) {
        if (command == null) {
            return;
        }
        command.register(dispatcher);
        commands.add(command);
    }

    public boolean tryExecute(String message) {
        if (message == null || !message.startsWith(prefix)) {
            return false;
        }
        ClientSuggestionProvider source = getSource();
        if (source == null) {
            return true;
        }
        try {
            dispatcher.execute(message.substring(prefix.length()), source);
        } catch (CommandSyntaxException exception) {
            CmdFeedback.error(exception.getMessage() != null ? exception.getMessage() : "Неверная команда");
        }
        return true;
    }

    public String getPrefix() {
        return prefix;
    }

    public CommandDispatcher<ClientSuggestionProvider> getDispatcher() {
        return dispatcher;
    }

    public ClientSuggestionProvider getSource() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null ? connection.getSuggestionsProvider() : null;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public static CommandManager get() {
        Skycore skycore = Skycore.get();
        return skycore != null ? skycore.commands() : null;
    }
}

package shelf.paster;

import net.fabricmc.api.ModInitializer;
import shelf.paster.client.command.CommandManager;
import shelf.paster.client.config.ConfigManager;
import shelf.paster.client.macro.MacroHandler;
import shelf.paster.client.macro.MacroManager;
import shelf.paster.client.module.ModuleManager;
import shelf.paster.client.waypoint.WaypointManager;

public final class Skycore implements ModInitializer {
    private static Skycore INSTANCE;

    private final ModuleManager modules = new ModuleManager();
    private final ConfigManager configs = new ConfigManager(modules);
    private final CommandManager commands = new CommandManager();
    private final MacroManager macros = new MacroManager();
    private final MacroHandler macroHandler = new MacroHandler();
    private final WaypointManager waypoints = new WaypointManager();

    public Skycore() {
        INSTANCE = this;
    }

    @Override
    public void onInitialize() {
        modules.init();
        configs.init();
        commands.init();
    }

    public static Skycore get() {
        return INSTANCE;
    }

    public ModuleManager modules() {
        return modules;
    }

    public ConfigManager configs() {
        return configs;
    }

    public CommandManager commands() {
        return commands;
    }

    public MacroManager macros() {
        return macros;
    }

    public MacroHandler macroHandler() {
        return macroHandler;
    }

    public WaypointManager waypoints() {
        return waypoints;
    }
}

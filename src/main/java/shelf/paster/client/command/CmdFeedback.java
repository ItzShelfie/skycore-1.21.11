package shelf.paster.client.command;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import shelf.paster.client.hud.elements.CommandToast;

/** Command output: sky-blue branded chat + glass toast (new look vs old SkyCore / gray). */
public final class CmdFeedback {
    private static final int ACCENT = 0x87CEFA;

    private CmdFeedback() {
    }

    public static void info(String message) {
        toast("info", message);
        send(Component.literal(stripFormatting(message)).withStyle(ChatFormatting.GRAY));
    }

    public static void success(String message) {
        toast("ok", message);
        send(Component.literal(stripFormatting(message)).withStyle(style -> style.withColor(TextColor.fromRgb(0xA8E6CF))));
    }

    public static void error(String message) {
        toast("err", message);
        send(Component.literal(stripFormatting(message)).withStyle(ChatFormatting.RED));
    }

    public static void send(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        MutableComponent prefix = Component.literal("Skycore")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(ACCENT)).withBold(true));
        MutableComponent sep = Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY);
        mc.player.displayClientMessage(Component.empty().append(prefix).append(sep).append(message), false);
    }

    public static MutableComponent suggest(String label, String command, int rgb, String hover) {
        return Component.literal(label).withStyle(Style.EMPTY
                .withColor(TextColor.fromRgb(rgb))
                .withClickEvent(new ClickEvent.SuggestCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }

    private static void toast(String title, String body) {
        CommandToast.push(title, stripFormatting(body));
    }

    private static String stripFormatting(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("§.", "");
    }
}

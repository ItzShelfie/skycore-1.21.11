package shelf.paster.mixin;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shelf.paster.client.command.CommandManager;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow
    @Final
    EditBox input;

    @Shadow
    @Final
    Minecraft minecraft;

    @Shadow
    private ParseResults<ClientSuggestionProvider> currentParse;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    private CommandSuggestions.SuggestionsList suggestions;

    @Shadow
    boolean keepSuggestions;

    @Shadow
    protected abstract void updateUsageInfo();

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void pasta$dotSuggestions(CallbackInfo ci) {
        CommandManager manager = CommandManager.get();
        if (manager == null || minecraft.player == null) {
            return;
        }

        String value = this.input.getValue();
        String prefix = manager.getPrefix();
        if (!value.startsWith(prefix)) {
            return;
        }

        ci.cancel();

        if (this.currentParse != null && !this.currentParse.getReader().getString().equals(value)) {
            this.currentParse = null;
        }
        if (!this.keepSuggestions) {
            this.input.setSuggestion(null);
            this.suggestions = null;
        }

        StringReader reader = new StringReader(value);
        reader.setCursor(prefix.length());

        ClientSuggestionProvider source = manager.getSource();
        if (source == null) {
            return;
        }

        if (this.currentParse == null) {
            this.currentParse = manager.getDispatcher().parse(reader, source);
        }

        int cursor = this.input.getCursorPosition();
        if (cursor >= 1 && (this.suggestions == null || !this.keepSuggestions)) {
            this.pendingSuggestions = manager.getDispatcher().getCompletionSuggestions(this.currentParse, cursor);
            this.pendingSuggestions.thenRun(() -> {
                if (this.pendingSuggestions.isDone()) {
                    this.updateUsageInfo();
                }
            });
        }
    }
}

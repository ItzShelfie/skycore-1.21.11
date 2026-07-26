package shelf.paster.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import shelf.paster.client.command.CommandManager;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void pasta$interceptDotCommands(String message, CallbackInfo ci) {
        CommandManager manager = CommandManager.get();
        if (manager == null) {
            return;
        }
        if (manager.tryExecute(message)) {
            ci.cancel();
        }
    }
}

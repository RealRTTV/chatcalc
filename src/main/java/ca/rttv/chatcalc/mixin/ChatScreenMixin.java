package ca.rttv.chatcalc.mixin;

import ca.rttv.chatcalc.ChatCalc;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {
    @Shadow
    protected TextFieldWidget chatField;

    @Inject(at = @At("HEAD"), method = "keyPressed(III)Z", cancellable = true)
    private void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == 258 && ChatCalc.tryParse(chatField)) {
            cir.setReturnValue(true);
        }
    }
}

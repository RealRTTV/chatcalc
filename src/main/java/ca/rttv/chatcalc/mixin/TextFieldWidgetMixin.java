package ca.rttv.chatcalc.mixin;

import ca.rttv.chatcalc.ChatHelper;
import ca.rttv.chatcalc.Config;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(TextFieldWidget.class)
abstract class TextFieldWidgetMixin {
    @Shadow @Final private TextRenderer textRenderer;

    @Shadow private String text;

    @Shadow public native int getCursor();

    @Unique
    @Nullable
    private Pair<String, Double> evaluationCache;

    @Inject(method = "renderButton", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Ljava/lang/String;isEmpty()Z", ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
    private void renderButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci, int i, int j, String string, boolean bl, boolean bl2, int k, int l, int m, int n, boolean bl3, int o) {
        if (!Config.displayAbove()) {
            return;
        }

        String word = ChatHelper.getWord(text, getCursor());
        try {
            double result;
            if (evaluationCache != null && evaluationCache.getLeft().equals(word)) {
                result = evaluationCache.getRight();
            } else {
                result = Config.makeEngine().eval(word, Optional.empty());
                evaluationCache = new Pair<>(word, result);
            }
            Text text = Text.literal(Config.getDecimalFormat().format(result));
            context.drawTooltip(textRenderer, text, o - 12, l - 4);
        } catch (Exception ignored) {
            // do nothing
        }
    }
}

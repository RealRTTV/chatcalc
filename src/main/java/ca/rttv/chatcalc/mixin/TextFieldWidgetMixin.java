package ca.rttv.chatcalc.mixin;

import ca.rttv.chatcalc.ChatCalc;
import ca.rttv.chatcalc.ChatHelper;
import ca.rttv.chatcalc.Config;
import ca.rttv.chatcalc.FunctionParameter;
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

@Mixin(TextFieldWidget.class)
abstract class TextFieldWidgetMixin {
    @Shadow @Final private TextRenderer textRenderer;

    @Shadow private String text;

    @Shadow public native int getCursor();

    @Unique
    @Nullable
    private Pair<String, Double> evaluationCache;

    // 1.20.2
    @Inject(method = "renderButton", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Ljava/lang/String;isEmpty()Z", ordinal = 1), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void renderButton1202(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci, int i, int j, String string, boolean bl, boolean bl2, int k, int l, int m, int n, boolean bl3, int o) {
        chatcalc$displayAbove(context, o, l);
    }

//    // 1.20.1
//    @Inject(method = "renderButton", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Ljava/lang/String;isEmpty()Z", ordinal = 1), locals = LocalCapture.CAPTURE_FAILSOFT)
//    private void renderButton1201(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci, int i, int j, int k, String string, boolean bl, boolean bl2, int l, int m, int n, boolean bl3, int o) {
//        chatcalc$displayAbove(context, o, m);
//    }

    @Unique
    private void chatcalc$displayAbove(DrawContext context, int x, int y) {
        if (!Config.displayAbove()) {
            return;
        }

        String word = ChatHelper.getWord(text, getCursor());

        if (ChatCalc.NUMBER.matcher(word).matches()) {
            return;
        }

        try {
            double result;
            if (evaluationCache != null && evaluationCache.getLeft().equals(word)) {
                result = evaluationCache.getRight();
            } else {
                result = Config.makeEngine().eval(word, new FunctionParameter[0]);
                evaluationCache = new Pair<>(word, result);
            }
            Text text = Text.literal("=" + Config.getDecimalFormat().format(result));
            context.drawTooltip(textRenderer, text, x - 8, y - 4);
        } catch (Exception ignored) {
            // do nothing
        }
    }
}

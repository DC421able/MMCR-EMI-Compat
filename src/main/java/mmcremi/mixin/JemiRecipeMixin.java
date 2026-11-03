package mmcremi.mixin;

import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.jemi.JemiRecipe;
import mmcremi.emi.MmcrEmiInputBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 每当 JEMI 为某个配方页构建部件时，重置 input handler 收集器。
 *
 * <p>{@code WidgetHolder} 实际上就是 {@code dev.emi.emi.screen.WidgetGroup}，
 * 记下它就能把鼠标屏幕坐标换算成配方页局部坐标。
 */
@Mixin(value = JemiRecipe.class, remap = false)
public abstract class JemiRecipeMixin {

    @Inject(method = "addWidgets", at = @At("HEAD"), remap = false, require = 0)
    private void mmcremi$beginWidgets(WidgetHolder widgets, CallbackInfo ci) {
        MmcrEmiInputBridge.begin(widgets);
    }
}

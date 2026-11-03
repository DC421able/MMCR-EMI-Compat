package mmcremi.mixin;

import dev.emi.emi.screen.RecipeScreen;
import mmcremi.emi.MmcrEmiInputBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把 EMI 配方页的鼠标事件转发给 MMCR 结构预览的 JEI input handler。
 *
 * <p>EMI 的 {@code Widget} 只有 mouseClicked / keyPressed，没有 drag / scroll，
 * 所以 MMCR 的预览在 EMI 里"能看见但转不动"。这里在 {@code RecipeScreen} 的四个入口上做转发，
 * 并且只在鼠标落在 MMCR 预览区域内时才吞掉事件，配方页其它交互（翻页、点击槽位）不受影响。
 */
@Mixin(value = RecipeScreen.class, remap = false)
public abstract class RecipeScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mmcremi$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (MmcrEmiInputBridge.clicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mmcremi$mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (MmcrEmiInputBridge.released(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mmcremi$mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (MmcrEmiInputBridge.dragged(mouseX, mouseY, button, dragX, dragY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mmcremi$mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (MmcrEmiInputBridge.scrolled(mouseX, mouseY, scrollX, scrollY)) {
            cir.setReturnValue(true);
        }
    }
}

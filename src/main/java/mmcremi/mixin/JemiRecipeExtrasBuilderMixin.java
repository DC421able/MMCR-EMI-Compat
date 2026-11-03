package mmcremi.mixin;

import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.jemi.impl.extras.JemiRecipeExtrasBuilder;
import dev.emi.emi.jemi.impl.extras.JemiWidgetBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mmcremi.emi.MmcrEmiInputBridge;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修两处 JEMI 对"附加部件"的处理。
 *
 * <h2>1. 位置/尺寸（UI 位置不对的根因）</h2>
 * JEMI 原本把每个 {@link IRecipeWidget} 包成 <b>0×0、位置 (0,0)</b> 的 drawable：
 * <pre>
 * new JemiWidgetBuilder(0, 0, (self, holder) -&gt;
 *     holder.addDrawable(self.x, self.y, 0, 0, (raw, mx, my, delta) -&gt; widget.drawWidget(raw, mx, my)));
 * </pre>
 * 但 JEI 的约定是：先把绘制坐标原点平移到部件自己的位置（{@code getPosition()}），
 * 再把"部件局部坐标"的鼠标位置传给 {@code drawWidget}。
 * MMCR 的结构预览正是靠这个约定画的（面板画在 pose 原点，控制按钮画在 x=-3…），
 * 于是一旦原点变成 (0,0)，预览就整体左上偏 (4,23)，按钮还被裁掉。
 *
 * <p>这里改成：用 {@code getScreenRectangle()} 取到部件的位置与尺寸，按它建立 drawable，
 * 并把鼠标坐标减掉部件原点后再交给 {@code drawWidget} —— 与 JEI 行为一致。
 * 没有有效矩形的部件（宽高 &lt;= 0）仍然走 JEMI 原逻辑，不影响其它模组。
 *
 * <h2>2. 输入 handler 截获</h2>
 * JEMI 收集了 {@code IJeiInputHandler} 却从未使用，这里记下来交给
 * {@link MmcrEmiInputBridge}，由 {@code RecipeScreen} 的鼠标事件转发（旋转 / 缩放）。
 */
@Mixin(value = JemiRecipeExtrasBuilder.class, remap = false)
public abstract class JemiRecipeExtrasBuilderMixin {

    @Inject(method = "addInputHandler", at = @At("HEAD"), remap = false, require = 0)
    private void mmcremi$captureInputHandler(IJeiInputHandler handler, CallbackInfo ci) {
        MmcrEmiInputBridge.capture(handler);
    }

    @Inject(method = "addWidget", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mmcremi$placeWidget(IRecipeWidget widget, CallbackInfo ci) {
        ScreenRectangle rect;
        try {
            rect = widget.getScreenRectangle();
        } catch (Throwable t) {
            return;
        }
        if (rect == null || rect.width() <= 0 || rect.height() <= 0) {
            return;
        }
        int originX = rect.left();
        int originY = rect.top();
        int width = rect.width();
        int height = rect.height();
        JemiWidgetBuilder builder = new JemiWidgetBuilder(width, height,
                (self, holder) -> holder.addDrawable(self.x, self.y, width, height,
                        (graphics, mouseX, mouseY, delta) ->
                                widget.drawWidget(graphics, mouseX - originX, mouseY - originY)));
        builder.setPosition(originX, originY);
        ((JemiRecipeExtrasBuilder) (Object) this).widgets.add(builder);
        ci.cancel();
    }
}

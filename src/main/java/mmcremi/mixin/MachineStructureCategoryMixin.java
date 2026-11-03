package mmcremi.mixin;

import cn.howxu.mmcr.compat.jei.MachineStructureCategory;
import cn.howxu.mmcr.compat.jei.MachineStructureDisplay;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mmcremi.MmcrEmiCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MMCR 多方块结构 JEI 配方页的空值保护。
 *
 * <p>原方法（反编译自 MMCR 0.1.4）：
 * <pre>
 * public void createRecipeExtras(IRecipeExtrasBuilder builder, MachineStructureDisplay display, IFocusGroup focuses) {
 *    JeiStructurePreviewWidget preview = new JeiStructurePreviewWidget(...);
 *    JeiPreviewLifecycle.registerActive(preview);
 *    builder.addWidget(preview);
 *    builder.addInputHandler(preview);
 *    builder.addWidget(new StructureMaterialWidget(display.materials(),
 *          builder.getRecipeSlots().getSlots(RecipeIngredientRole.RENDER_ONLY), ...)); // ← EMI 下 NPE
 * }
 * </pre>
 *
 * <p>注入点在 {@code getRecipeSlots()} 调用之前：预览部件与输入处理器此时已经添加完毕，
 * 因此只需在 slots 不可用时取消余下逻辑，即可在不丢功能的前提下消除异常。
 */
@Mixin(value = MachineStructureCategory.class, remap = false)
public abstract class MachineStructureCategoryMixin {

    @Inject(
            method = "createRecipeExtras(Lmezz/jei/api/gui/widgets/IRecipeExtrasBuilder;Lcn/howxu/mmcr/compat/jei/MachineStructureDisplay;Lmezz/jei/api/recipe/IFocusGroup;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/api/gui/widgets/IRecipeExtrasBuilder;getRecipeSlots()Lmezz/jei/api/gui/ingredient/IRecipeSlotDrawablesView;",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void mmcremi$guardMaterialWidget(IRecipeExtrasBuilder builder,
                                             MachineStructureDisplay display,
                                             IFocusGroup focuses,
                                             CallbackInfo ci) {
        IRecipeSlotDrawablesView slots = builder.getRecipeSlots();
        // EMI/JEMI: null。极少数实现可能给出数量不为 9 的视图，此时 MMCR 会抛
        // IllegalArgumentException("nine material slots required")，一并跳过。
        if (slots == null || slots.getSlots(RecipeIngredientRole.RENDER_ONLY).size() != 9) {
            MmcrEmiCompat.noteStructureExtrasSkipped();
            ci.cancel();
        }
    }
}

package mmcremi.mixin;

import cn.howxu.mmcr.compat.jei.MachineRecipeCategory;
import cn.howxu.mmcr.compat.jei.MachineRecipeDisplay;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mmcremi.MmcrEmiCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MMCR 机器配方 JEI 配方页的空值保护。
 *
 * <p>原方法（反编译自 MMCR 0.1.4）：
 * <pre>
 * public void createRecipeExtras(IRecipeExtrasBuilder builder, MachineRecipeDisplay recipe, IFocusGroup focuses) {
 *    MachineRecipeLayout layout = MachineRecipeLayout.forDisplay(recipe);
 *    if (layout.arrow() != null) {
 *       addRecipeArrow(builder, this.guiHelper, layout.arrow());       // 箭头，先执行
 *    }
 *    if (!layout.metadataPages().isEmpty()) {
 *       List&lt;IRecipeSlotDrawable&gt; slots = builder.getRecipeSlots()   // ← EMI 下 NPE
 *             .getSlots().stream().filter(...).toList();
 *       ...
 *    }
 * }
 * </pre>
 *
 * <p>注入点在 {@code getRecipeSlots()} 调用之前，箭头此时已经添加，
 * 因此取消只会跳过必须依赖 slot 的元数据详情部件（metadataPages 非空的配方）。
 */
@Mixin(value = MachineRecipeCategory.class, remap = false)
public abstract class MachineRecipeCategoryMixin {

    @Inject(
            method = "createRecipeExtras(Lmezz/jei/api/gui/widgets/IRecipeExtrasBuilder;Lcn/howxu/mmcr/compat/jei/MachineRecipeDisplay;Lmezz/jei/api/recipe/IFocusGroup;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/api/gui/widgets/IRecipeExtrasBuilder;getRecipeSlots()Lmezz/jei/api/gui/ingredient/IRecipeSlotDrawablesView;",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void mmcremi$guardMetadataWidget(IRecipeExtrasBuilder builder,
                                             MachineRecipeDisplay recipe,
                                             IFocusGroup focuses,
                                             CallbackInfo ci) {
        if (builder.getRecipeSlots() == null) {
            MmcrEmiCompat.noteRecipeExtrasSkipped();
            ci.cancel();
        }
    }
}

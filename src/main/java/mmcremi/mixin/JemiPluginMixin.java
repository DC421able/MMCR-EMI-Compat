package mmcremi.mixin;

import dev.emi.emi.jemi.JemiPlugin;
import java.util.Set;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mmcremi.MmcrEmiCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让 JEMI 不要把 MMCR 的机器配方类别当成"该模组已经做过 EMI 支持"而跳过。
 *
 * <p>问题（用户实例日志实测）：
 * <pre>
 * [JEMI] Skipping recipe category kubejs:recipe_pool/iceiceice because mod is already handled
 * [JEMI] Skipping recipe category kubejs:recipe_pool/electric_blast_furnace because mod is already handled
 * </pre>
 *
 * <p>JEMI 的判断是 {@code handledNamespaces.contains(category.getRecipeType().getUid().getNamespace())}：
 * 只要某个模组注册过 EMI 插件，它的命名空间下的所有 JEI 类别都会被跳过。
 * 但 MMCR 的机器配方类别 id 是 {@code <配方池命名空间>:recipe_pool/<机器名>}，
 * 而在这个整合包里配方池由 KubeJS 定义（{@code kubejs:iceiceice}），于是命名空间成了 {@code kubejs}——
 * KubeJS 恰好有 EMI 插件（只做增删条目/信息页，不注册任何类别），MMCR 的机器类别就被误判并全部丢失，
 * 玩家在 EMI 左侧根本看不到机器配方。
 *
 * <p>修法：只对"类别实现类属于 MMCR"的条目绕过这一层命名空间判断；
 * 其它模组（包括 KubeJS 自己的类别）行为完全不变。
 */
@Mixin(value = JemiPlugin.class, remap = false)
public abstract class JemiPluginMixin {

    private static final ThreadLocal<Class<?>> MMCREMI_CURRENT_CATEGORY = new ThreadLocal<>();

    /** 在 register 的循环里记下当前正在处理的类别实现类。 */
    @Redirect(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/api/recipe/category/IRecipeCategory;getRecipeType()Lmezz/jei/api/recipe/RecipeType;"
            ),
            remap = false,
            require = 0
    )
    private RecipeType<?> mmcremi$rememberCategory(IRecipeCategory<?> category) {
        MMCREMI_CURRENT_CATEGORY.set(category.getClass());
        return category.getRecipeType();
    }

    /**
     * 同一个方法里有两处 {@code Set.contains}：
     * <ul>
     *   <li>{@code handledNamespaces.contains(namespace)} —— String 参数，需要放行 MMCR；</li>
     *   <li>{@code existingCategories.contains(id)} —— ResourceLocation 参数，保持原样。</li>
     * </ul>
     */
    @Redirect(
            method = "register",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"),
            remap = false,
            require = 0
    )
    private boolean mmcremi$keepMmcrCategory(Set<?> set, Object value) {
        if (value instanceof String namespace) {
            Class<?> category = MMCREMI_CURRENT_CATEGORY.get();
            if (category != null && category.getName().startsWith("cn.howxu.mmcr.")) {
                MmcrEmiCompat.noteCategoryRescued(category.getSimpleName(), namespace);
                return false;
            }
        }
        return set.contains(value);
    }
}

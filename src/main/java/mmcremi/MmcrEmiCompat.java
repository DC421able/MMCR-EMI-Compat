package mmcremi;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * MMCR × EMI 兼容修复。
 *
 * <p>问题根因（已在用户实例 latest.log 中复现）：
 * EMI 通过内置的 JEMI 桥接层加载 JEI 插件时，会构造
 * {@code new JemiRecipeExtrasBuilder(null)}，也就是
 * {@link mezz.jei.api.gui.widgets.IRecipeExtrasBuilder#getRecipeSlots()} 返回 {@code null}。
 * 而 MMCR 的 {@code MachineStructureCategory#createRecipeExtras} 与
 * {@code MachineRecipeCategory#createRecipeExtras} 都假定它非空，直接链式调用
 * {@code builder.getRecipeSlots().getSlots(...)}，于是每次打开配方页都会抛出：
 *
 * <pre>
 * java.lang.NullPointerException: Cannot invoke
 *   "IRecipeSlotDrawablesView.getSlots(RecipeIngredientRole)"
 *   because the return value of "IRecipeExtrasBuilder.getRecipeSlots()" is null
 *   at cn.howxu.mmcr.compat.jei.MachineStructureCategory.createRecipeExtras
 *   at dev.emi.emi.jemi.JemiRecipe.addWidgets
 * </pre>
 *
 * <p>本模组只在「recipe slots 不可用」时取消对应的附加部件，其余行为与原生 JEI 完全一致：
 * 结构页仍会显示 3D 多方块预览与鼠标交互，配方页仍会显示流程箭头，
 * 只是材料翻页 / 元数据详情这类必须依赖可绘制 slot 的部件会被跳过。
 */
@Mod(MmcrEmiCompat.MOD_ID)
public final class MmcrEmiCompat {

    public static final String MOD_ID = "mmcr_emi_compat";

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final AtomicBoolean STRUCTURE_NOTICE = new AtomicBoolean();
    private static final AtomicBoolean RECIPE_NOTICE = new AtomicBoolean();
    private static final Set<String> RESCUED_CATEGORIES = ConcurrentHashMap.newKeySet();

    public MmcrEmiCompat(IEventBus modBus) {
        LOGGER.info("[mmcr_emi_compat] MMCR × EMI compat loaded "
                + "(JEI recipe-slot null guard + JEMI category rescue + EMI input bridge)");
    }

    /**
     * JEMI 因为"命名空间已被某模组处理"而跳过了 MMCR 的机器配方类别，
     * 这里记一条日志方便确认补丁确实救回来了（每个类别只记一次）。
     */
    public static void noteCategoryRescued(String categoryClass, String namespace) {
        String key = categoryClass + "@" + namespace;
        if (RESCUED_CATEGORIES.size() < 16 && RESCUED_CATEGORIES.add(key)) {
            LOGGER.info("[mmcr_emi_compat] re-registered MMCR JEI category '{}' (JEMI had skipped namespace '{}')",
                    categoryClass, namespace);
        }
    }

    /**
     * 结构页：EMI/JEMI 下没有可绘制的 9 个 RENDER_ONLY slot，跳过材料翻页部件。
     * 同一时刻只提示一次，避免刷屏。
     */
    public static void noteStructureExtrasSkipped() {
        if (STRUCTURE_NOTICE.compareAndSet(false, true)) {
            LOGGER.info("[mmcr_emi_compat] MMCR structure page: recipe slots unavailable "
                    + "(EMI/JEMI), skipped the 9-slot material overlay. Structure preview is unaffected.");
        }
    }

    /**
     * 机器配方页：EMI/JEMI 下没有可绘制的 slot，跳过元数据详情部件。
     */
    public static void noteRecipeExtrasSkipped() {
        if (RECIPE_NOTICE.compareAndSet(false, true)) {
            LOGGER.info("[mmcr_emi_compat] MMCR machine recipe page: recipe slots unavailable "
                    + "(EMI/JEMI), skipped the metadata detail widget. Arrow and layout are unaffected.");
        }
    }
}

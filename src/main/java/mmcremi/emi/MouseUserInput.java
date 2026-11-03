package mmcremi.emi;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.runtime.IJeiKeyMapping;
import net.minecraft.client.KeyMapping;

/**
 * 给 JEI 的 {@link IJeiInputHandler} 用的鼠标输入对象。
 *
 * <p>{@code simulate = true} 对应 JEI 的"按下/预演"输入：MMCR 的结构预览靠它记录
 * {@code pressedInputTarget}，没有这一步它就不会进入拖拽旋转状态。
 * {@code simulate = false} 对应真正的释放/点击，MMCR 在这一步执行按钮动作。
 */
public final class MouseUserInput implements IJeiUserInput {

    private final InputConstants.Key key;
    private final boolean simulate;

    public MouseUserInput(InputConstants.Key key, boolean simulate) {
        this.key = key;
        this.simulate = simulate;
    }

    @Override
    public InputConstants.Key getKey() {
        return key;
    }

    @Override
    public int getModifiers() {
        return 0;
    }

    @Override
    public boolean isSimulate() {
        return simulate;
    }

    @Override
    public boolean is(KeyMapping mapping) {
        return false;
    }

    @Override
    public boolean is(IJeiKeyMapping mapping) {
        return mapping != null && key != null && mapping.isActiveAndMatches(key);
    }
}

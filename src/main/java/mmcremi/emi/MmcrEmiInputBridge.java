package mmcremi.emi;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mmcremi.MmcrEmiCompat;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/**
 * 把 JEI 的 {@link IJeiInputHandler} 转发给 EMI 的配方页鼠标事件。
 *
 * <p>背景：EMI 的 JEMI 桥接会收集 JEI 配方类别添加的 input handler
 * （{@code JemiRecipeExtrasBuilder#addInputHandler}），但之后从未使用；
 * 而 EMI 的 {@code Widget} 只暴露 mouseClicked / keyPressed，
 * {@code RecipeScreen#mouseDragged} / {@code mouseScrolled} 也不向普通部件分发。
 * 所以 MMCR 结构预览在 EMI 里"能看见但转不动、缩放不了"。
 *
 * <p>坐标约定（与 JEI 一致）：配方页坐标 = 屏幕坐标 - 配方页原点；
 * handler 收到的是 **部件局部坐标** = 配方页坐标 - {@code handler.getArea()} 左上角。
 */
public final class MmcrEmiInputBridge {

    private static final String MMCR_PACKAGE = "cn.howxu.mmcr.";
    private static final List<IJeiInputHandler> HANDLERS = new ArrayList<>();

    /** 当前配方页的 WidgetGroup，用来把屏幕坐标换算成页内坐标（EMI 自己也是这么算的）。 */
    private static Object origin;

    private MmcrEmiInputBridge() {
    }

    /** 每次为某个配方页构建部件时重置。holder 通常是 dev.emi.emi.screen.WidgetGroup。 */
    public static void begin(Object widgetHolder) {
        HANDLERS.clear();
        origin = widgetHolder;
    }

    public static void capture(IJeiInputHandler handler) {
        if (handler == null) {
            return;
        }
        if (!handler.getClass().getName().startsWith(MMCR_PACKAGE)) {
            return;
        }
        HANDLERS.add(handler);
    }

    public static boolean clicked(double mouseX, double mouseY, int button) {
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
        return dispatch(mouseX, mouseY, (handler, x, y) -> handler.handleInput(x, y, new MouseUserInput(key, true)));
    }

    public static boolean released(double mouseX, double mouseY, int button) {
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
        return dispatch(mouseX, mouseY, (handler, x, y) -> handler.handleInput(x, y, new MouseUserInput(key, false)));
    }

    public static boolean dragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
        return dispatch(mouseX, mouseY, (handler, x, y) -> handler.handleMouseDragged(x, y, key, dragX, dragY));
    }

    public static boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return dispatch(mouseX, mouseY, (handler, x, y) -> handler.handleMouseScrolled(x, y, scrollX, scrollY));
    }

    private static boolean dispatch(double mouseX, double mouseY, HandlerCall call) {
        if (HANDLERS.isEmpty()) {
            return false;
        }
        int pageX = (int) mouseX - originCoord("x");
        int pageY = (int) mouseY - originCoord("y");
        for (IJeiInputHandler handler : snapshot()) {
            ScreenRectangle area;
            try {
                area = handler.getArea();
            } catch (Throwable t) {
                continue;
            }
            if (area == null) {
                continue;
            }
            int localX = pageX - area.left();
            int localY = pageY - area.top();
            if (localX < 0 || localY < 0 || localX >= area.width() || localY >= area.height()) {
                continue;
            }
            try {
                if (call.apply(handler, localX, localY)) {
                    return true;
                }
            } catch (Throwable t) {
                MmcrEmiCompat.LOGGER.debug("[mmcr_emi_compat] JEI input handler failed", t);
            }
        }
        return false;
    }

    private static List<IJeiInputHandler> snapshot() {
        return new ArrayList<>(HANDLERS);
    }

    private static int originCoord(String name) {
        Object group = origin;
        if (group == null) {
            return 0;
        }
        try {
            return (int) group.getClass().getMethod(name).invoke(group);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface HandlerCall {
        boolean apply(IJeiInputHandler handler, int localX, int localY);
    }
}

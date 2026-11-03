# MMCR x EMI —— 证据摘录

实例：`E:\PCL2\.minecraft\versions\o-cek&pt 2`
日志：`logs\2026-10-24-7.log.gz`（1.0.0 之前）与 `logs\latest.log`（安装 1.0.0 之后，2026-10-24 21:36–21:37）

## 1. 配方页 NPE（1.0.0 已修）

旧日志中 `[EMI/]: Exception adding JEMI extras` 共 18 次：16 次是 MMCR 的 `NullPointerException`，
2 次是 JEI 自带 TagInfo 页的 `AbstractMethodError`（EMI 1.1.24 对 JEI 19.56，与本补丁无关）。

```text
[Render thread/ERROR] [EMI/]: Exception adding JEMI extras
java.lang.NullPointerException: Cannot invoke
  "mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView.getSlots(mezz.jei.api.recipe.RecipeIngredientRole)"
  because the return value of "mezz.jei.api.gui.widgets.IRecipeExtrasBuilder.getRecipeSlots()" is null
    at TRANSFORMER/mmcr@0.1.4/cn.howxu.mmcr.compat.jei.MachineStructureCategory.createRecipeExtras(MachineStructureCategory.java:120)
    at TRANSFORMER/emi@1.1.24+1.21.1+neoforge/dev.emi.emi.jemi.JemiRecipe.addWidgets(JemiRecipe.java:148)
```

安装 1.0.0 后的 `latest.log` 中该类异常 **0 次**，并出现：

```text
[mmcremi.MmcrEmiCompat/]: [mmcr_emi_compat] MMCR × EMI compat loaded ...
[mmcremi.MmcrEmiCompat/]: [mmcr_emi_compat] MMCR structure page: recipe slots unavailable (EMI/JEMI), skipped the 9-slot material overlay. ...
```

## 2. 机器配方类别被 JEMI 跳过（1.1.0 修）

`latest.log`（JEMI 收集 JEI 类别阶段）里只有两条 `Skipping`，正好是本整合包定义的两台 MMCR 机器：

```text
[EMI/]: [JEMI] Skipping recipe category kubejs:recipe_pool/iceiceice because mod is already handled
[EMI/]: [JEMI] Skipping recipe category kubejs:recipe_pool/electric_blast_furnace because mod is already handled
```

机器定义来自 `kubejs/startup_scripts/machines/machines.js`：

```js
const Machines = [
    ['iceiceice', '', 'minecraft:blue_ice', false],
    ['electric_blast_furnace', '', 'kubejs:mechanical_casing', false]
]
MMCREvents.startup(event => { ... builder.recipePool(`kubejs:${m[0]}`) ... })
```

即机器类别 id = `kubejs:recipe_pool/<机器名>`，命名空间 `kubejs` 命中 JEMI 的
“该模组已有 EMI 插件” 判断（`handledNamespaces.contains(id.getNamespace())`）。
而 KubeJS 的 EMI 插件（`KubeJSEMIPlugin`，见 `kjs-KubeJSEMIPlugin.java`）只做条目增删 / 信息页，
**不注册任何类别**，所以这些类别既没被 KubeJS 注册、又被 JEMI 跳过 —— EMI 里彻底消失。

## 3. 结构预览无法旋转 / 缩放（1.1.0 修）

- `emi-JemiRecipe.java` 第 135 行：`new JemiRecipeExtrasBuilder(null)`；
  第 138–140 行只消费 `extras.widgets`，`extras.inputHandlers` 从未使用；
- `emi-JemiRecipeExtrasBuilder.java`：`addWidget` 用 `(0, 0, …)` 尺寸包装部件；`addInputHandler` 只往列表里塞；
- EMI `RecipeScreen.mouseDragged` / `mouseScrolled` 的字节码只处理 `SlotWidget` 拖拽与配方翻页，不向普通部件分发；
  EMI 的 `Widget` 接口也只有 `mouseClicked` / `keyPressed`；
- MMCR 侧（`mmcr-JeiStructurePreviewWidget.java`）：旋转走 `handleMouseDragged`、缩放走 `handleMouseScrolled`，
  且拖拽前需要一次 `handleInput(..., isSimulate() == true)` 来记录 `pressedInputTarget`。

结论：在 EMI 1.1.24 下，JEMI 的附加部件既没有尺寸信息也没有输入通道，预览只能显示、不能交互。

## 4. 附加部件位置/尺寸错位（1.2.0 修）

JEMI 的 `addWidget(IRecipeWidget)`（`emi-JemiRecipeExtrasBuilder.java` 第 48–56 行）把部件包成 0×0：

```java
public void addWidget(IRecipeWidget widget) {
   this.addEmi(new JemiWidgetBuilder(0, 0,
      (self, holder) -> holder.addDrawable(self.x, self.y, 0, 0,
         (raw, mouseX, mouseY, delta) -> widget.drawWidget(raw, mouseX, mouseY))));
}
```

而 EMI 的 `DrawableWidget.render`（`emi-DrawableWidget.java` 第 40–46 行）只是把 pose 平移到 drawable 自身的 (x, y)，
鼠标坐标原样透传；`WidgetHolder.addDrawable(x, y, w, h, …)` 的 (x, y) 就是部件锚点。

JEI 的约定与此不同：JEI 把 pose 平移到**部件自身位置**再调用 `drawWidget`，并传**部件局部坐标**的鼠标。
MMCR 预览完全按 JEI 约定写（`mmcr-JeiStructurePreviewWidget.java`）：

```java
public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
   ScreenPosition origin = guiOrigin(graphics);                  // pose 原点 = 部件位置
   this.panel.render(graphics, this.width, this.height, 0.0F,
        origin.x(), origin.y(), origin.x() + (int)mouseX, origin.y() + (int)mouseY, ...);
   int controlX = -3 + index * CONTROL_STEP;                     // 层按钮在局部 x=-3 起
   int controlY = this.height + -13;
}

private boolean insidePreview(double mouseX, double mouseY) {
   return mouseX >= 0 && mouseX < this.width && mouseY >= 0 && mouseY < this.height;  // 局部坐标
}
```

于是在 EMI 下：预览整体左上偏 (4, 23)、层按钮（局部 x=-3）被裁到页面左上角、鼠标命中区域错位。

## 5. 相关反编译源码

| 文件 | 说明 |
| --- | --- |
| `mmcr-MachineStructureCategory.java` | 出事的第 116–126 行（`getRecipeSlots()`） |
| `mmcr-MachineRecipeCategory.java` | 出事的第 172–188 行（元数据详情部件） |
| `mmcr-StructureMaterialWidget.java` | 「必须正好 9 个 slot」的构造约束 |
| `mmcr-JeiStructurePreviewWidget.java` | 结构预览的绘制/输入约定（pose 原点、局部坐标、层按钮） |
| `mmcr-neoforge.mods.toml` | MMCR 的 mixin 配置与可选依赖（JEI 为 `requiredMods`） |
| `emi-JemiRecipe.java` | JEMI 的配方桥接；`JemiRecipeExtrasBuilder(null)` 就在第 135 行 |
| `emi-JemiRecipeExtrasBuilder.java` | `addWidget` 的 0 尺寸包装、`addInputHandler` 的收集 |
| `emi-JemiPlugin.java` | JEMI 的类别收集与 `handledNamespaces` 跳过逻辑 |
| `emi-DrawableWidget.java` | EMI drawable 的 pose 平移与鼠标透传（坐标系对照） |
| `emi-WidgetHolder.java` | `addDrawable` 等默认方法 |
| `kjs-KubeJSEMIPlugin.java` | KubeJS 的 EMI 插件（证明它不注册类别） |

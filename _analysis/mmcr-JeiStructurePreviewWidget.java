package cn.howxu.mmcr.compat.jei;

import cn.howxu.mmcr.api.machine.Machine;
import cn.howxu.mmcr.client.preview.StructurePreviewPanel;
import cn.howxu.mmcr.client.preview.StructurePreviewSchema;
import cn.howxu.mmcr.client.preview.StructurePreviewSchema.Candidate;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.LongSupplier;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public final class JeiStructurePreviewWidget implements IRecipeWidget, IJeiInputHandler, AutoCloseable {
   private static final float CONTROL_SCALE = 0.9F;
   private static final int CONTROL_SIZE = Math.round(13.5F);
   private static final int CONTROL_STEP = CONTROL_SIZE + 4;
   private static final int UI_X_OFFSET = -3;
   private static final int CONTROL_Y_OFFSET = -13;
   private static final float LAYER_TEXT_SCALE = 0.9F;
   private static final int LAYER_TEXT_Y_OFFSET = -24;
   private static final int CANDIDATE_SIZE = 16;
   private static final int CANDIDATE_STEP = 18;
   private static final int LAYOUT_WIDTH = 168;
   private static final int PREVIEW_INPUT_TARGET = 1;
   private static final int CONTROL_INPUT_TARGET_BASE = 100;
   private static final int CANDIDATE_INPUT_TARGET_BASE = 200;
   @Nullable
   private final StructurePreviewPanel panel;
   @Nullable
   private final JeiStructurePreviewWidget.Preview testingPreview;
   private final IntPredicate mouseButtonDown;
   private final int x;
   private final int y;
   private final int width;
   private final int height;
   @Nullable
   private final IIngredientManager ingredientManager;
   private final LongSupplier clock = System::currentTimeMillis;
   private int pressedInputTarget = -1;
   private int pressedInputButton = -1;
   private int previewDragButton = -1;
   private boolean closed;

   public JeiStructurePreviewWidget(Machine machine, int x, int y, int width, int height, IIngredientManager ingredientManager) {
      this.panel = new StructurePreviewPanel(machine);
      this.testingPreview = null;
      this.mouseButtonDown = button -> GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), button) == 1;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.ingredientManager = ingredientManager;
   }

   private JeiStructurePreviewWidget(JeiStructurePreviewWidget.Preview preview, int x, int y, int width, int height, IntPredicate mouseButtonDown) {
      this.panel = null;
      this.testingPreview = preview;
      this.mouseButtonDown = mouseButtonDown;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.ingredientManager = null;
   }

   static JeiStructurePreviewWidget forTesting(JeiStructurePreviewWidget.Preview preview, int x, int y, int width, int height) {
      return forTesting(preview, x, y, width, height, button -> true);
   }

   static JeiStructurePreviewWidget forTesting(JeiStructurePreviewWidget.Preview preview, int x, int y, int width, int height, IntPredicate mouseButtonDown) {
      return new JeiStructurePreviewWidget(preview, x, y, width, height, mouseButtonDown);
   }

   public ScreenPosition getPosition() {
      return new ScreenPosition(this.x, this.y);
   }

   public ScreenRectangle getScreenRectangle() {
      return new ScreenRectangle(this.x, this.y, 168, this.height + 54);
   }

   public ScreenRectangle getArea() {
      return this.getScreenRectangle();
   }

   public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
      int heldButton = this.previewDragButton >= 0 ? this.previewDragButton : this.pressedInputButton;
      if (heldButton >= 0 && !this.mouseButtonDown.test(heldButton)) {
         this.cancelPreviewDrag();
         this.pressedInputTarget = -1;
         this.pressedInputButton = -1;
      }

      if (this.panel != null) {
         ScreenPosition origin = guiOrigin(graphics);
         this.panel
            .render(graphics, this.width, this.height, 0.0F, origin.x(), origin.y(), origin.x() + (int)mouseX, origin.y() + (int)mouseY, origin.x(), origin.y());
         if (this.panel.isReady()) {
            String[] labels = new String[]{"+", "-", "A", "R", "M"};
            int highlightControl = this.highlightControlIndex();

            for (int index = 0; index <= highlightControl; index++) {
               int controlX = -3 + index * CONTROL_STEP;
               int controlY = this.height + -13;
               int background = index == highlightControl && this.positionHighlightsEnabled() ? -12551797 : -8355712;
               graphics.fill(controlX, controlY, controlX + CONTROL_SIZE, controlY + CONTROL_SIZE, background);
               graphics.pose().pushPose();
               graphics.pose().translate((float)controlX + (float)CONTROL_SIZE / 2.0F, (float)controlY + (float)CONTROL_SIZE / 2.0F, 0.0F);
               graphics.pose().scale(0.9F, 0.9F, 1.0F);
               Component label = index == highlightControl
                  ? Component.translatable("gui.mmcr.structure_preview.highlights.short")
                  : Component.literal(labels[index]);
               int labelWidth = Minecraft.getInstance().font.width(label);
               graphics.drawString(Minecraft.getInstance().font, label, -labelWidth / 2, -9 / 2, -1, false);
               graphics.pose().popPose();
            }

            int selectedLayer = this.panel.selectedLayer();
            StructurePreviewSchema schema = this.panel.schema();
            List<Integer> layers = schema == null ? List.of() : schema.layers();
            Component layerText = selectedLayer < 0
               ? Component.translatable("jei.mmcr.structure_preview.all_layers")
               : Component.translatable("jei.mmcr.structure_preview.layer", new Object[]{selectedLayer, layers.indexOf(selectedLayer) + 1, layers.size()});
            graphics.pose().pushPose();
            graphics.pose().scale(0.9F, 0.9F, 1.0F);
            int layerTextY = (int)((float)(this.height + -24) / 0.9F);
            int layerTextX = -3;
            graphics.drawString(Minecraft.getInstance().font, layerText, layerTextX, layerTextY, -1, false);
            if (this.panel.hasMultipleStages()) {
               int levelTextX = Minecraft.getInstance().font.width(layerText) + 4;
               graphics.drawString(
                  Minecraft.getInstance().font,
                  Component.literal("Level=" + this.panel.stageNumber()),
                  (int)((float)(-3 + levelTextX) / 0.9F),
                  layerTextY,
                  -1,
                  false
               );
            }

            graphics.pose().popPose();
            this.renderCandidates(graphics);
         }
      }
   }

   private void renderCandidates(GuiGraphics graphics) {
      if (this.panel != null) {
         List<Candidate> candidates = this.panel.selectedCandidates();
         if (!candidates.isEmpty()) {
            int visibleCount = Math.min(maxVisibleCandidates(), candidates.size());
            long timeMillis = this.clock.getAsLong();

            for (int index = 0; index < visibleCount; index++) {
               Candidate candidate = this.candidateForSlot(candidates, index, timeMillis);
               if (candidate != null) {
                  graphics.renderItem(candidate.stack(), 0, index * 18, 0);
               }
            }
         }
      }
   }

   @Nullable
   private Candidate candidateForSlot(List<Candidate> candidates, int slot, long timeMillis) {
      if (this.panel == null) {
         return null;
      } else if (slot == 0) {
         return candidates.getFirst();
      } else {
         int remainingCandidates = candidates.size() - 1;
         if (remainingCandidates == 0) {
            return null;
         } else {
            int offset = (int)Math.floorDiv(timeMillis, 1000L) % remainingCandidates;
            return candidates.get(1 + Math.floorMod(slot - 1 + offset, remainingCandidates));
         }
      }
   }

   private static ScreenPosition guiOrigin(GuiGraphics graphics) {
      Matrix4f pose = graphics.pose().last().pose();
      return new ScreenPosition(Math.round(pose.m30()), Math.round(pose.m31()));
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         this.cancelPreviewDrag();
         if (this.panel != null) {
            this.panel.close();
         }

         if (this.testingPreview != null) {
            this.testingPreview.close();
         }
      }
   }

   public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
      boolean simulate = input.isSimulate();
      if (input.getKey().getType() != Type.MOUSE) {
         if (!simulate) {
            this.pressedInputTarget = -1;
            this.cancelPreviewDrag();
         }

         return false;
      } else {
         int button = input.getKey().getValue();
         if (button >= 0 && button <= 2 && this.isReady()) {
            if (simulate && this.previewDragButton >= 0 && (button == this.previewDragButton || !this.mouseButtonDown.test(this.previewDragButton))) {
               this.cancelPreviewDrag();
            }

            if (this.previewDragButton >= 0 && button != this.previewDragButton) {
               return false;
            } else {
               int inputTarget = this.inputTarget(mouseX, mouseY);
               if (button != 0 && inputTarget != 1) {
                  inputTarget = -1;
               }

               if (simulate) {
                  this.pressedInputTarget = inputTarget;
                  this.pressedInputButton = button;
                  return inputTarget >= 0;
               } else {
                  int pressedTarget = this.pressedInputTarget;
                  int pressedButton = this.pressedInputButton;
                  this.pressedInputTarget = -1;
                  this.pressedInputButton = -1;
                  if (this.previewDragButton >= 0) {
                     this.previewDragButton = -1;
                     boolean inside = this.insidePreview(mouseX, mouseY);
                     boolean handled = this.mouseReleased(this.previewMouseX(mouseX), this.previewMouseY(mouseY), button);
                     return inside || handled;
                  } else if (pressedTarget == inputTarget && pressedButton == button) {
                     int control = this.controlAt(mouseX, mouseY);
                     if (control >= 0) {
                        if (button != 0) {
                           return false;
                        } else if (input.isSimulate()) {
                           return true;
                        } else {
                           if (control == this.highlightControlIndex()) {
                              this.togglePositionHighlights();
                           } else {
                              switch (control) {
                                 case 0:
                                    this.selectPreviousLayer();
                                    break;
                                 case 1:
                                    this.selectNextLayer();
                                    break;
                                 case 2:
                                    this.showAllLayers();
                                    break;
                                 case 3:
                                    this.reset();
                                    break;
                                 case 4:
                                    this.selectNextStage();
                              }
                           }

                           return true;
                        }
                     } else if (this.candidateAt(mouseX, mouseY) >= 0) {
                        return button == 0;
                     } else if (!this.insidePreview(mouseX, mouseY)) {
                        return false;
                     } else {
                        return !this.mouseClicked(this.previewMouseX(mouseX), this.previewMouseY(mouseY), button)
                           ? false
                           : this.mouseReleased(this.previewMouseX(mouseX), this.previewMouseY(mouseY), button);
                     }
                  } else {
                     this.cancelPreviewDrag();
                     return false;
                  }
               }
            }
         } else {
            if (!simulate) {
               this.pressedInputTarget = -1;
               this.cancelPreviewDrag();
            }

            return false;
         }
      }
   }

   public boolean handleMouseDragged(double mouseX, double mouseY, Key mouseKey, double dragX, double dragY) {
      if (this.previewDragButton >= 0 && mouseKey.getType() == Type.MOUSE && mouseKey.getValue() != this.previewDragButton) {
         return false;
      } else if (mouseKey.getType() == Type.MOUSE
         && mouseKey.getValue() >= 0
         && mouseKey.getValue() <= 2
         && this.isReady()
         && this.controlAt(mouseX, mouseY) < 0
         && this.candidateAt(mouseX, mouseY) < 0
         && this.insidePreview(mouseX, mouseY)) {
         if (this.previewDragButton < 0) {
            if (this.pressedInputTarget != 1 || this.pressedInputButton != mouseKey.getValue()) {
               return false;
            }

            double pressX = this.previewMouseX(mouseX - dragX);
            double pressY = this.previewMouseY(mouseY - dragY);
            if (!this.mouseClicked(pressX, pressY, mouseKey.getValue())) {
               return false;
            }

            this.previewDragButton = mouseKey.getValue();
         }

         boolean handled = this.mouseDragged(this.previewMouseX(mouseX), this.previewMouseY(mouseY), mouseKey.getValue(), dragX, dragY);
         if (!handled) {
            this.cancelPreviewDrag();
         }

         return handled;
      } else {
         this.cancelPreviewDrag();
         return false;
      }
   }

   public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
      this.cancelPreviewDrag();
      return this.controlAt(mouseX, mouseY) < 0
         && this.candidateAt(mouseX, mouseY) < 0
         && this.insidePreview(mouseX, mouseY)
         && this.mouseScrolled(this.previewMouseX(mouseX), this.previewMouseY(mouseY), scrollDeltaY);
   }

   private void cancelPreviewDrag() {
      if (this.previewDragButton >= 0) {
         int button = this.previewDragButton;
         this.previewDragButton = -1;
         this.pressedInputTarget = -1;
         this.pressedInputButton = -1;
         this.mouseReleased(-1.0, -1.0, button);
      }
   }

   private int candidateAt(double mouseX, double mouseY) {
      if (this.panel == null) {
         return -1;
      } else {
         int count = Math.min(maxVisibleCandidates(), this.panel.selectedCandidates().size());
         if (!(mouseX < 0.0) && !(mouseX >= 16.0) && !(mouseY < 0.0)) {
            int index = (int)Math.floor(mouseY / 18.0);
            return index < count && mouseY < (double)(index * 18 + 16) ? index : -1;
         } else {
            return -1;
         }
      }
   }

   private int inputTarget(double mouseX, double mouseY) {
      int control = this.controlAt(mouseX, mouseY);
      if (control >= 0) {
         return 100 + control;
      } else {
         int candidate = this.candidateAt(mouseX, mouseY);
         if (candidate >= 0) {
            return 200 + candidate;
         } else {
            return this.insidePreview(mouseX, mouseY) ? 1 : -1;
         }
      }
   }

   private static int maxVisibleCandidates() {
      return switch ((int)Minecraft.getInstance().getWindow().getGuiScale()) {
         case 1 -> 12;
         case 2 -> 10;
         case 3 -> 8;
         default -> 4;
      };
   }

   private boolean insidePreview(double mouseX, double mouseY) {
      return mouseX >= 0.0 && mouseX < (double)this.width && mouseY >= 0.0 && mouseY < (double)this.height;
   }

   private double previewMouseX(double mouseX) {
      return mouseX;
   }

   private double previewMouseY(double mouseY) {
      return mouseY;
   }

   private int controlAt(double mouseX, double mouseY) {
      if (!(mouseX < 0.0) && !(mouseY < (double)(this.height + -13)) && !(mouseY >= (double)(this.height + -13 + CONTROL_SIZE))) {
         int relativeX = (int)Math.floor(mouseX - -3.0);
         if (relativeX < 0) {
            return -1;
         } else {
            int column = relativeX / CONTROL_STEP;
            return column >= 0 && column <= this.highlightControlIndex() && mouseX < (double)(-3 + column * CONTROL_STEP + CONTROL_SIZE) ? column : -1;
         }
      } else {
         return -1;
      }
   }

   public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
      if (this.isReady()) {
         int control = this.controlAt(mouseX, mouseY);
         if (control >= 0) {
            if (control == this.highlightControlIndex()) {
               tooltip.add(Component.translatable("gui.mmcr.structure_preview.highlights." + (this.positionHighlightsEnabled() ? "disable" : "enable")));
               tooltip.add(Component.translatable("gui.mmcr.structure_preview.highlights.legend"));
            } else {
               String[] keys = new String[]{"previous_layer", "next_layer", "all_layers", "reset", "next_level"};
               tooltip.add(Component.translatable("jei.mmcr.structure_preview." + keys[control]));
            }
         } else {
            int candidateIndex = this.candidateAt(mouseX, mouseY);
            if (candidateIndex >= 0 && this.panel != null) {
               List<Candidate> candidates = this.panel.selectedCandidates();
               Candidate candidate = this.candidateForSlot(candidates, candidateIndex, this.clock.getAsLong());
               if (candidate == null) {
                  return;
               }

               ItemStack stack = candidate.stack();
               tooltip.add(stack.getHoverName());
               if (candidate.modifier()) {
                  tooltip.add(Component.translatable("jei.mmcr.structure_preview.modifier"));
               }

               this.ingredientManager.createTypedIngredient(VanillaTypes.ITEM_STACK, stack, false).ifPresent(tooltip::setIngredient);
            }
         }
      }
   }

   private boolean isReady() {
      return this.testingPreview != null || this.panel != null && this.panel.isReady();
   }

   private boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.testingPreview != null ? this.testingPreview.mouseClicked(mouseX, mouseY, button) : this.panel.mouseClicked(mouseX, mouseY, button);
   }

   private boolean mouseReleased(double mouseX, double mouseY, int button) {
      return this.testingPreview != null ? this.testingPreview.mouseReleased(mouseX, mouseY, button) : this.panel.mouseReleased(mouseX, mouseY, button);
   }

   private boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      return this.testingPreview != null
         ? this.testingPreview.mouseDragged(mouseX, mouseY, button, dragX, dragY)
         : this.panel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
   }

   private boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
      return this.testingPreview != null
         ? this.testingPreview.mouseScrolled(mouseX, mouseY, scrollDelta)
         : this.panel.mouseScrolled(mouseX, mouseY, scrollDelta);
   }

   private void selectPreviousLayer() {
      if (this.testingPreview != null) {
         this.testingPreview.previous();
      } else {
         this.panel.selectPreviousLayer();
      }
   }

   private void selectNextLayer() {
      if (this.testingPreview != null) {
         this.testingPreview.next();
      } else {
         this.panel.selectNextLayer();
      }
   }

   private void showAllLayers() {
      if (this.testingPreview != null) {
         this.testingPreview.all();
      } else {
         this.panel.showAllLayers();
      }
   }

   private void reset() {
      if (this.testingPreview != null) {
         this.testingPreview.reset();
      } else {
         this.panel.reset();
      }
   }

   private void selectNextStage() {
      if (this.testingPreview != null) {
         this.testingPreview.selectNextStage();
      } else {
         this.panel.selectNextStage();
      }
   }

   private int highlightControlIndex() {
      boolean multipleStages = this.testingPreview != null ? this.testingPreview.hasMultipleStages() : this.panel.hasMultipleStages();
      return multipleStages ? 5 : 4;
   }

   private boolean positionHighlightsEnabled() {
      return this.testingPreview != null ? this.testingPreview.positionHighlightsEnabled() : this.panel.positionHighlightsEnabled();
   }

   private void togglePositionHighlights() {
      if (this.testingPreview != null) {
         this.testingPreview.togglePositionHighlights();
      } else {
         this.panel.togglePositionHighlights();
      }
   }

   interface Preview {
      boolean mouseClicked(double mouseX, double mouseY, int button);

      default boolean mouseReleased(double mouseX, double mouseY, int button) {
         return false;
      }

      default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
         return false;
      }

      default boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
         return false;
      }

      default void close() {
      }

      default void previous() {
      }

      default void next() {
      }

      default void all() {
      }

      default void reset() {
      }

      default boolean hasMultipleStages() {
         return false;
      }

      default void selectNextStage() {
      }

      default boolean positionHighlightsEnabled() {
         return false;
      }

      default void togglePositionHighlights() {
      }
   }
}

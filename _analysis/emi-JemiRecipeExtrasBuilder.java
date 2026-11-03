package dev.emi.emi.jemi.impl.extras;

import com.google.common.collect.Lists;
import dev.emi.emi.api.render.EmiTexture;
import java.util.List;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.placement.IPlaceable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.gui.widgets.IScrollBoxWidget;
import mezz.jei.api.gui.widgets.IScrollGridWidget;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import mezz.jei.api.gui.widgets.ITextWidget;
import net.minecraft.network.chat.FormattedText;

public class JemiRecipeExtrasBuilder implements IRecipeExtrasBuilder {
   public IRecipeSlotDrawablesView slots;
   public List<IJeiInputHandler> inputHandlers = Lists.newArrayList();
   public List<IJeiGuiEventListener> eventListeners = Lists.newArrayList();
   public List<JemiWidgetBuilder> widgets = Lists.newArrayList();

   public JemiRecipeExtrasBuilder(IRecipeSlotDrawablesView slots) {
      this.slots = slots;
   }

   public IRecipeSlotDrawablesView getRecipeSlots() {
      return this.slots;
   }

   public void addDrawable(IDrawable drawable, int xPos, int yPos) {
      this.addDrawable(drawable).setPosition(xPos, yPos);
   }

   public IPlaceable<?> addDrawable(IDrawable drawable) {
      return this.addEmi(
         new JemiWidgetBuilder(
            drawable.getWidth(),
            drawable.getHeight(),
            (self, holder) -> holder.addDrawable(self.x, self.y, drawable.getWidth(), drawable.getHeight(), (raw, mouseX, mouseY, delta) -> drawable.draw(raw))
         )
      );
   }

   public void addWidget(IRecipeWidget widget) {
      this.addEmi(
         new JemiWidgetBuilder(
            0,
            0,
            (self, holder) -> holder.addDrawable(self.x, self.y, 0, 0, (raw, mouseX, mouseY, delta) -> widget.drawWidget(raw, (double)mouseX, (double)mouseY))
         )
      );
   }

   public void addSlottedWidget(ISlottedRecipeWidget widget, List<IRecipeSlotDrawable> slots) {
      this.addWidget(widget);
   }

   public void addInputHandler(IJeiInputHandler inputHandler) {
      this.inputHandlers.add(inputHandler);
   }

   public void addGuiEventListener(IJeiGuiEventListener guiEventListener) {
      this.eventListeners.add(guiEventListener);
   }

   public IScrollBoxWidget addScrollBoxWidget(int width, int height, int xPos, int yPos) {
      return new JemiScrollBoxWidget(xPos, yPos, width, height);
   }

   public IScrollGridWidget addScrollGridWidget(List<IRecipeSlotDrawable> slots, int columns, int visibleRows) {
      return new JemiScrollGridWidget(slots, 0, 0, columns, visibleRows);
   }

   public IPlaceable<?> addRecipeArrow() {
      return this.addEmiTexture(EmiTexture.EMPTY_ARROW);
   }

   public IPlaceable<?> addRecipePlusSign() {
      return this.addEmiTexture(EmiTexture.PLUS);
   }

   public IPlaceable<?> addAnimatedRecipeArrow(int ticksPerCycle) {
      return this.addAnimatedEmiTexture(EmiTexture.EMPTY_ARROW, EmiTexture.FULL_ARROW, ticksPerCycle * 1000 / 20, true, false, false);
   }

   public IPlaceable<?> addAnimatedRecipeFlame(int cookTime) {
      return this.addAnimatedEmiTexture(EmiTexture.EMPTY_FLAME, EmiTexture.FULL_FLAME, cookTime * 1000 / 20, false, true, true);
   }

   public ITextWidget addText(List<FormattedText> text, int maxWidth, int maxHeight) {
      return new JemiTextWidget(text, maxWidth, maxHeight);
   }

   private IPlaceable<?> addEmiTexture(EmiTexture texture) {
      return this.addEmi(new JemiWidgetBuilder(texture.width, texture.height, (self, holder) -> holder.addTexture(texture, self.x, self.y)));
   }

   private IPlaceable<?> addAnimatedEmiTexture(EmiTexture texture, EmiTexture animated, int time, boolean horizontal, boolean endToStart, boolean fullToEmpty) {
      return this.addEmi(new JemiWidgetBuilder(texture.width, texture.height, (self, holder) -> {
         holder.addTexture(texture, self.x, self.y);
         holder.addAnimatedTexture(animated, self.x, self.y, time, horizontal, endToStart, fullToEmpty);
      }));
   }

   private IPlaceable<?> addEmi(JemiWidgetBuilder builder) {
      this.widgets.add(builder);
      return builder;
   }
}

package dev.emi.emi.api.widget;

import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.ButtonWidget.ClickAction;
import dev.emi.emi.api.widget.DrawableWidget.DrawableWidgetConsumer;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public interface WidgetHolder {
   int getWidth();

   int getHeight();

   <T extends Widget> T add(T var1);

   default SlotWidget addSlot(EmiIngredient ingredient, int x, int y) {
      return this.add(new SlotWidget(ingredient, x, y));
   }

   default SlotWidget addSlot(int x, int y) {
      return this.addSlot(EmiStack.EMPTY, x, y);
   }

   default TankWidget addTank(EmiIngredient stack, int x, int y, int width, int height, int capacity) {
      return this.add(new TankWidget(stack, x, y, width, height, (long)capacity));
   }

   default TextureWidget addTexture(ResourceLocation texture, int x, int y, int width, int height, int u, int v) {
      return this.add(new TextureWidget(texture, x, y, width, height, u, v));
   }

   default TextureWidget addTexture(
      ResourceLocation texture, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int textureWidth, int textureHeight
   ) {
      return this.add(new TextureWidget(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight));
   }

   default TextureWidget addTexture(EmiTexture texture, int x, int y) {
      return this.addTexture(
         texture.texture,
         x,
         y,
         texture.width,
         texture.height,
         texture.u,
         texture.v,
         texture.regionWidth,
         texture.regionHeight,
         texture.textureWidth,
         texture.textureHeight
      );
   }

   default DrawableWidget addDrawable(int x, int y, int width, int height, DrawableWidgetConsumer consumer) {
      return this.add(new DrawableWidget(x, y, width, height, consumer));
   }

   default TextWidget addText(Component text, int x, int y, int color, boolean shadow) {
      return this.addText(text.getVisualOrderText(), x, y, color, shadow);
   }

   default TextWidget addText(FormattedCharSequence text, int x, int y, int color, boolean shadow) {
      return this.add(new TextWidget(text, x, y, color, shadow));
   }

   default ButtonWidget addButton(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, ClickAction action) {
      return this.add(new ButtonWidget(x, y, width, height, u, v, isActive, action));
   }

   default ButtonWidget addButton(int x, int y, int width, int height, int u, int v, ResourceLocation texture, BooleanSupplier isActive, ClickAction action) {
      return this.add(new ButtonWidget(x, y, width, height, u, v, texture, isActive, action));
   }

   default TooltipWidget addTooltip(BiFunction<Integer, Integer, List<ClientTooltipComponent>> tooltipSupplier, int x, int y, int width, int height) {
      return this.add(new TooltipWidget(tooltipSupplier, x, y, width, height));
   }

   default TooltipWidget addTooltip(List<ClientTooltipComponent> tooltip, int x, int y, int width, int height) {
      return this.addTooltip((mx, my) -> tooltip, x, y, width, height);
   }

   default TooltipWidget addTooltipText(List<Component> tooltip, int x, int y, int width, int height) {
      return this.addTooltip(
         tooltip.stream().map(Component::getVisualOrderText).<ClientTooltipComponent>map(ClientTooltipComponent::create).toList(), x, y, width, height
      );
   }

   default FillingArrowWidget addFillingArrow(int x, int y, int time) {
      return this.add(new FillingArrowWidget(x, y, time));
   }

   default AnimatedTextureWidget addAnimatedTexture(
      ResourceLocation texture, int x, int y, int width, int height, int u, int v, int time, boolean horizontal, boolean endToStart, boolean fullToEmpty
   ) {
      return this.add(new AnimatedTextureWidget(texture, x, y, width, height, u, v, time, horizontal, endToStart, fullToEmpty));
   }

   default AnimatedTextureWidget addAnimatedTexture(
      ResourceLocation texture,
      int x,
      int y,
      int width,
      int height,
      int u,
      int v,
      int regionWidth,
      int regionHeight,
      int textureWidth,
      int textureHeight,
      int time,
      boolean horizontal,
      boolean endToStart,
      boolean fullToEmpty
   ) {
      return this.add(
         new AnimatedTextureWidget(
            texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight, time, horizontal, endToStart, fullToEmpty
         )
      );
   }

   default AnimatedTextureWidget addAnimatedTexture(EmiTexture texture, int x, int y, int time, boolean horizontal, boolean endToStart, boolean fullToEmpty) {
      return this.addAnimatedTexture(
         texture.texture,
         x,
         y,
         texture.width,
         texture.height,
         texture.u,
         texture.v,
         texture.regionWidth,
         texture.regionHeight,
         texture.textureWidth,
         texture.textureHeight,
         time,
         horizontal,
         endToStart,
         fullToEmpty
      );
   }

   default GeneratedSlotWidget addGeneratedSlot(Function<Random, EmiIngredient> stackSupplier, int unique, int x, int y) {
      return this.add(new GeneratedSlotWidget(stackSupplier, unique, x, y));
   }
}

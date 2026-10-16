package gcewing.architecture.client;

import gcewing.architecture.*;
import gcewing.architecture.shape.Shape;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SawbenchScreen extends AbstractContainerScreen<SawbenchMenu> {
  private int page;
  private final List<Button> shapeButtons = new ArrayList<>();

  public SawbenchScreen(SawbenchMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageWidth = 276;
    imageHeight = 236;
    inventoryLabelX = 56;
    inventoryLabelY = 142;
  }

  @Override
  protected void init() {
    super.init();
    shapeButtons.clear();
    for (int i = 0; i < 10; i++) {
      final int slot = i;
      shapeButtons.add(
          addRenderableWidget(
              Button.builder(Component.empty(), b -> send(page * 10 + slot))
                  .bounds(leftPos + 8 + (i % 2) * 132, topPos + 22 + (i / 2) * 18, 128, 18)
                  .build()));
    }
    addRenderableWidget(
        Button.builder(
                Component.literal("<"),
                b -> {
                  page = Math.floorMod(page - 1, 11);
                  refresh();
                })
            .bounds(leftPos + 8, topPos + 114, 24, 16)
            .build());
    addRenderableWidget(
        Button.builder(
                Component.literal(">"),
                b -> {
                  page = (page + 1) % 11;
                  refresh();
                })
            .bounds(leftPos + 244, topPos + 114, 24, 16)
            .build());
    addRenderableWidget(
        Button.builder(
                Component.translatable("gui.architecturecraft.cut"), b -> send(SawbenchMenu.CUT))
            .bounds(leftPos + 90, topPos + 132, 96, 20)
            .build());
    refresh();
  }

  private void send(int id) {
    if (minecraft != null && minecraft.gameMode != null)
      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
  }

  private void refresh() {
    for (int i = 0; i < 10; i++) {
      int index = page * 10 + i;
      Button button = shapeButtons.get(i);
      button.visible = index <= SawbenchMenu.CLADDING;
      if (button.visible) {
        String title =
            index == SawbenchMenu.CLADDING
                ? "Cladding sheets"
                : Component.translatable("shape.architecturecraft." + Shape.values()[index].key())
                    .getString();
        button.setMessage(Component.literal(font.plainSubstrByWidth(title, 118)));
        button.setTooltip(Tooltip.create(Component.literal(title)));
      }
    }
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    super.render(graphics, mouseX, mouseY, delta);
    renderTooltip(graphics, mouseX, mouseY);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    g.drawString(font, title, 8, 6, 0xff303030, false);
  }

  @Override
  protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
    g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
    for (var slot : menu.slots) {
      int x = leftPos + slot.x - 1, y = topPos + slot.y - 1;
      g.fill(x, y, x + 18, y + 18, 0xff373737);
      g.fill(x + 1, y + 1, x + 17, y + 17, 0xff8b8b8b);
    }
    int selected = menu.selected();
    if (selected >= 0 && selected <= SawbenchMenu.CLADDING) {
      Shape shape =
          selected == SawbenchMenu.CLADDING ? Shape.CladdingSheet : Shape.values()[selected];
      String yield =
          selected == SawbenchMenu.CLADDING
              ? SawbenchMenu.materialCost(shape, menu.getSlot(0).getItem(), true)
                  + " blocks → 16 sheets"
              : SawbenchMenu.materialCost(shape, menu.getSlot(0).getItem(), false)
                  + " blocks → "
                  + shape.itemsProduced
                  + " shapes";
      g.drawCenteredString(font, Component.literal(yield), leftPos + 138, topPos + 118, 0xff303030);
      if (selected / 10 == page) {
        int n = selected % 10;
        g.fill(
            leftPos + 7 + (n % 2) * 132,
            topPos + 21 + (n / 2) * 18,
            leftPos + 137 + (n % 2) * 132,
            topPos + 41 + (n / 2) * 18,
            0xff377ab8);
      }
    }
    g.drawCenteredString(
        font, Component.literal((page + 1) + " / 11"), leftPos + 244, topPos + 7, 0xff303030);
  }
}

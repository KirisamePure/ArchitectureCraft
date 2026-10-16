package gcewing.architecture.client;

import gcewing.architecture.*;
import gcewing.architecture.shape.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Original 242x224 sawbench, using the original background, shape sprites and selection artwork.
 */
public final class SawbenchScreen extends AbstractContainerScreen<SawbenchMenu> {
  private static final ResourceLocation GUI = ArchitectureCraft.id("textures/gui/gui_sawbench.png");
  private static final ResourceLocation GRID =
      ArchitectureCraft.id("textures/gui/shapemenu_bg.png");
  public static final int MENU_X = 44, MENU_Y = 23, CELL = 24, PAGES_X = 176, PAGES_Y = 19;

  public SawbenchScreen(SawbenchMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageWidth = 242;
    imageHeight = 224;
  }

  public int guiLeft() {
    return leftPos;
  }

  public int guiTop() {
    return topPos;
  }

  @Override
  public void render(GuiGraphics g, int x, int y, float delta) {
    super.render(g, x, y, delta);
    renderTooltip(g, x, y);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int x, int y) {}

  @Override
  protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
    g.blit(RenderPipelines.GUI_TEXTURED, GUI, leftPos, topPos, 0, 0, 242, 224, 256, 256);
    g.blit(
        RenderPipelines.GUI_TEXTURED,
        GRID,
        leftPos + 40,
        topPos + 19,
        0,
        0,
        128,
        104,
        256,
        208,
        256,
        256);
    int p = Math.clamp(menu.page(), 0, 7), selected = menu.selectedSlot(p);
    var shapes = ShapePage.PAGES.get(p).shapes();
    g.pose().pushMatrix();
    g.pose().translate(leftPos + MENU_X, topPos + MENU_Y);
    g.pose().scale(.5f, .5f);
    for (int i = 0; i < shapes.size(); i++) {
      int id = shapes.get(i).id, sheet = id / 110, offset = id % 110;
      ResourceLocation texture =
          ArchitectureCraft.id("textures/gui/shapemenu_items_" + sheet + ".png");
      g.pose().pushMatrix();
      g.pose().translate(0, 1.5f);
      g.blit(
          RenderPipelines.GUI_TEXTURED,
          texture,
          (i % 5) * 48 + 4,
          (i / 5) * 48,
          (offset % 10) * 40,
          (offset / 10) * 45,
          40,
          45,
          512,
          512);
      g.pose().popMatrix();
    }
    if (ShapePage.valid(p, selected))
      g.blit(
          RenderPipelines.GUI_TEXTURED,
          GUI,
          (selected % 5) * 48,
          (selected / 5) * 48,
          44,
          23,
          49,
          49,
          256,
          256);
    g.pose().popMatrix();
    g.fill(
        leftPos + PAGES_X,
        topPos + PAGES_Y + p * 10,
        leftPos + PAGES_X + 58,
        topPos + PAGES_Y + p * 10 + 10,
        color("selectedbgcolor", 0x66ccff));
    for (int i = 0; i < 8; i++)
      g.drawString(
          font,
          Component.translatable("shape.architecturecraft." + ShapePage.PAGES.get(i).key()),
          leftPos + PAGES_X + 1,
          topPos + PAGES_Y + 1 + i * 10,
          color("fontcolor", 0x404040),
          false);
    g.drawString(
        font,
        Component.translatable("container.architecturecraft.sawbench"),
        leftPos + 7,
        topPos + 7,
        color("fontcolor", 0x404040),
        false);
    Shape shape = menu.selectedShape();
    if (shape != null) {
      Component title = Component.translatable("shape.architecturecraft." + shape.key());
      g.drawString(
          font,
          title,
          leftPos + Math.min(40, 168 - font.width(title)),
          topPos + 128,
          color("fontcolor", 0x404040),
          false);
      g.pose().pushMatrix();
      g.pose().translate(leftPos + 7, topPos + 82);
      g.pose().scale(.5f, .5f);
      g.drawString(
          font,
          Component.translatable(
              "shape.architecturecraft.makes",
              SawbenchMenu.materialCost(shape, menu.getSlot(0).getItem()),
              shape.itemsProduced),
          0,
          0,
          color("fontcolor", 0x404040),
          false);
      g.pose().popMatrix();
    }
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    int x = (int) event.x() - leftPos, y = (int) event.y() - topPos;
    if (x >= PAGES_X && x < PAGES_X + 58 && y >= PAGES_Y && y < PAGES_Y + 80) {
      int p = (y - PAGES_Y) / 10;
      select(p, menu.selectedSlot(p));
      return true;
    }
    if (x >= MENU_X && x < MENU_X + 120 && y >= MENU_Y && y < MENU_Y + 96) {
      int slot = (y - MENU_Y) / CELL * 5 + (x - MENU_X) / CELL;
      select(menu.page(), slot);
      return true;
    }
    return super.mouseClicked(event, doubleClick);
  }

  private static int color(String key, int fallback) {
    String value = Component.translatable("shape.architecturecraft." + key).getString();
    if (value.length() <= 6) {
      try {
        return 0xff000000 | Integer.parseUnsignedInt(value, 16);
      } catch (NumberFormatException ignored) {
      }
    }
    return 0xff000000 | fallback;
  }

  private void select(int page, int slot) {
    if (ShapePage.valid(page, slot) && minecraft != null && minecraft.gameMode != null)
      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ShapePage.button(page, slot));
  }
}

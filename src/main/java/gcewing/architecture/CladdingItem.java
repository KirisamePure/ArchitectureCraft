package gcewing.architecture;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;

public final class CladdingItem extends Item {
  public CladdingItem(Properties properties) {
    super(properties);
  }

  @Override
  public void appendHoverText(
      ItemStack stack,
      TooltipContext context,
      TooltipDisplay display,
      Consumer<Component> lines,
      TooltipFlag flag) {
    super.appendHoverText(stack, context, display, lines, flag);
    if (stack.has(ArchitectureCraft.SHAPE_DATA))
      lines.accept(ShapeData.fromStack(stack).base().getBlock().getName());
  }
}

package gcewing.architecture;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;

public final class ShapeItem extends BlockItem {
  public ShapeItem(Block block, Properties properties) {
    super(block, properties);
  }

  @Override
  public Component getName(ItemStack stack) {
    ShapeData data = ShapeData.fromStack(stack);
    return Component.translatable("shape.architecturecraft." + data.shape().key())
        .append(" (")
        .append(data.base().getBlock().getName())
        .append(")");
  }
}

package gcewing.architecture;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public final class ShapeItem extends BlockItem {
  public ShapeItem(Block block, Properties properties) {
    super(block, properties);
  }

  @Override
  public Component getName(ItemStack stack) {
    return Component.translatable(
        "shape.architecturecraft." + ShapeData.fromStack(stack).shape().key());
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

  @Override
  protected boolean canPlace(
      BlockPlaceContext context, net.minecraft.world.level.block.state.BlockState state) {
    var plan = ShapePlacement.plan(context);
    return state.canSurvive(context.getLevel(), context.getClickedPos())
        && context
            .getLevel()
            .isUnobstructed(
                context.getPlayer(),
                ShapeGeometry.collision(
                        plan.data(), state, context.getLevel(), context.getClickedPos(), false)
                    .move(
                        context.getClickedPos().getX(),
                        context.getClickedPos().getY(),
                        context.getClickedPos().getZ()));
  }

  @Override
  public InteractionResult place(BlockPlaceContext context) {
    ShapePlacement.Plan plan = ShapePlacement.plan(context);
    InteractionResult result = super.place(context);
    if (result.consumesAction()
        && context.getLevel().getBlockEntity(context.getClickedPos())
            instanceof ShapeBlockEntity be) be.setData(plan.data());
    return result;
  }
}

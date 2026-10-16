package gcewing.architecture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gcewing.architecture.shape.Shape;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Item components and block entities use the same persistent, registry-aware material data. */
public record ShapeData(
    int shapeId, BlockState base, Optional<BlockState> secondary, int disconnected, int offset) {
  public static final Codec<ShapeData> CODEC =
      RecordCodecBuilder.create(
          i ->
              i.group(
                      Codec.INT.fieldOf("shape").forGetter(ShapeData::shapeId),
                      BlockState.CODEC.fieldOf("base").forGetter(ShapeData::base),
                      BlockState.CODEC.optionalFieldOf("secondary").forGetter(ShapeData::secondary),
                      Codec.intRange(0, 63)
                          .optionalFieldOf("disconnected", 0)
                          .forGetter(ShapeData::disconnected),
                      Codec.intRange(-8, 8)
                          .optionalFieldOf("offset", 0)
                          .forGetter(ShapeData::offset))
                  .apply(i, ShapeData::new));

  public static ShapeData defaults() {
    return new ShapeData(0, Blocks.OAK_PLANKS.defaultBlockState(), Optional.empty(), 0, 0);
  }

  public Shape shape() {
    return Shape.byId(shapeId);
  }

  public ShapeData withSecondary(BlockState state) {
    return new ShapeData(shapeId, base, Optional.ofNullable(state), disconnected, offset);
  }

  public ShapeData withConnections(int mask) {
    return new ShapeData(shapeId, base, secondary, mask, offset);
  }

  public ShapeData withOffset(int n) {
    return new ShapeData(shapeId, base, secondary, disconnected, n);
  }

  public static ShapeData fromStack(ItemStack stack) {
    return stack.getOrDefault(ArchitectureCraft.SHAPE_DATA, defaults());
  }

  public ItemStack stack(int count) {
    ItemStack stack =
        new ItemStack(
            shape() == Shape.CladdingSheet ? ArchitectureCraft.CLADDING : ArchitectureCraft.SHAPE,
            count);
    stack.set(ArchitectureCraft.SHAPE_DATA, this);
    return stack;
  }
}

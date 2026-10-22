package gcewing.architecture;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** A third-party-style transparent cube with no glass superclass or acceptance tag. */
public final class MaterialTestBlocks implements ModInitializer {
  public static Block TRANSPARENT_CUBE;

  @Override
  public void onInitialize() {
    var id = ResourceLocation.fromNamespaceAndPath("architecturecraft-test", "transparent_cube");
    TRANSPARENT_CUBE =
        Registry.register(
            BuiltInRegistries.BLOCK,
            id,
            new Block(
                BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .noOcclusion()));
    Registry.register(
        BuiltInRegistries.ITEM,
        id,
        new BlockItem(
            TRANSPARENT_CUBE,
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
  }
}

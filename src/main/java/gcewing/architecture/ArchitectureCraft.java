package gcewing.architecture;

import gcewing.architecture.shape.Shape;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ArchitectureCraft implements ModInitializer {
  public static final String MOD_ID = "architecturecraft";

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
  }

  public static final net.minecraft.tags.TagKey<Block> SAWBENCH_MATERIALS =
      net.minecraft.tags.TagKey.create(Registries.BLOCK, id("sawbench_materials"));
  public static final net.minecraft.tags.TagKey<Block> EMISSIVE_MATERIALS =
      net.minecraft.tags.TagKey.create(Registries.BLOCK, id("emissive_materials"));

  public static final DataComponentType<ShapeData> SHAPE_DATA =
      Registry.register(
          BuiltInRegistries.DATA_COMPONENT_TYPE,
          id("shape"),
          DataComponentType.<ShapeData>builder()
              .persistent(ShapeData.CODEC)
              .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(ShapeData.CODEC))
              .build());
  public static final ShapeBlock SHAPE =
      registerBlock(
          "shape",
          new ShapeBlock(
              blockProperties("shape")
                  .dynamicShape()
                  .strength(2.0F)
                  .noOcclusion()
                  .lightLevel(s -> s.getValue(ShapeBlock.GLOWING) ? 15 : 0)));
  public static final SawbenchBlock SAWBENCH =
      registerBlock(
          "sawbench", new SawbenchBlock(blockProperties("sawbench").strength(2.0F).noOcclusion()));
  public static final Item SAWBLADE =
      registerItem("sawblade", new Item(itemProperties("sawblade")));
  public static final Item LARGE_PULLEY =
      registerItem("large_pulley", new Item(itemProperties("large_pulley")));
  public static final Item CHISEL =
      registerItem(
          "chisel",
          new ArchitectTool(itemProperties("chisel").stacksTo(1), ArchitectTool.Kind.CHISEL));
  public static final Item HAMMER =
      registerItem(
          "hammer",
          new ArchitectTool(itemProperties("hammer").stacksTo(1), ArchitectTool.Kind.HAMMER));
  public static final Item GLOWBRUSH =
      registerItem(
          "glowbrush",
          new ArchitectTool(itemProperties("glowbrush").stacksTo(1), ArchitectTool.Kind.GLOWBRUSH));
  public static final Item CLADDING =
      registerItem("cladding", new CladdingItem(itemProperties("cladding")));
  public static final BlockEntityType<ShapeBlockEntity> SHAPE_ENTITY =
      Registry.register(
          BuiltInRegistries.BLOCK_ENTITY_TYPE,
          id("shape"),
          FabricBlockEntityTypeBuilder.create(ShapeBlockEntity::new, SHAPE).build());
  public static final BlockEntityType<SawbenchBlockEntity> SAWBENCH_ENTITY =
      Registry.register(
          BuiltInRegistries.BLOCK_ENTITY_TYPE,
          id("sawbench"),
          FabricBlockEntityTypeBuilder.create(SawbenchBlockEntity::new, SAWBENCH).build());
  public static final MenuType<SawbenchMenu> SAWBENCH_MENU =
      Registry.register(
          BuiltInRegistries.MENU,
          id("sawbench"),
          new MenuType<>(SawbenchMenu::new, FeatureFlags.VANILLA_SET));

  private static BlockBehaviour.Properties blockProperties(String name) {
    return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(name)));
  }

  private static Item.Properties itemProperties(String name) {
    return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
  }

  private static <T extends Block> T registerBlock(String name, T block) {
    Registry.register(BuiltInRegistries.BLOCK, id(name), block);
    registerItem(
        name,
        name.equals("shape")
            ? new ShapeItem(block, itemProperties(name).useBlockDescriptionPrefix())
            : new BlockItem(block, itemProperties(name).useBlockDescriptionPrefix()));
    return block;
  }

  private static <T extends Item> T registerItem(String name, T item) {
    return Registry.register(BuiltInRegistries.ITEM, id(name), item);
  }

  @Override
  public void onInitialize() {
    Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        id("architecturecraft"),
        FabricItemGroup.builder()
            .title(Component.literal("ArchitectureCraft"))
            .icon(() -> new ItemStack(SAWBENCH))
            .displayItems(
                (parameters, output) -> {
                  output.accept(SAWBENCH);
                  output.accept(SAWBLADE);
                  output.accept(LARGE_PULLEY);
                  output.accept(CHISEL);
                  output.accept(HAMMER);
                  output.accept(GLOWBRUSH);
                  for (Shape shape : Shape.values())
                    output.accept(
                        new ShapeData(
                                shape.id,
                                Blocks.OAK_PLANKS.defaultBlockState(),
                                java.util.Optional.empty(),
                                0,
                                0)
                            .stack(1));
                })
            .build());
  }

  public static boolean validMaterial(ItemStack stack) {
    if (!(stack.getItem() instanceof BlockItem bi)
        || bi.getBlock() == SHAPE
        || bi.getBlock() == SAWBENCH) return false;
    var state = SawbenchMenu.materialState(stack);
    if (state.is(SAWBENCH_MATERIALS)) return true;
    return !state.hasBlockEntity()
        && (bi.getBlock() instanceof SlabBlock
            || state.isCollisionShapeFullBlock(
                net.minecraft.world.level.EmptyBlockGetter.INSTANCE,
                net.minecraft.core.BlockPos.ZERO));
  }
}

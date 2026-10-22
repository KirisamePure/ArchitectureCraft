package gcewing.architecture;

import gcewing.architecture.shape.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/**
 * Reference-behavior tests, including extraction paths which a simple direct craft-button test
 * missed.
 */
public final class ArchitectureGameTests {
  private static final BlockPos POS = new BlockPos(2, 2, 2);

  private static void check(GameTestHelper h, boolean value, String message) {
    h.assertTrue(value, Component.literal(message));
  }

  private static Player player(GameTestHelper h) {
    Player p = h.makeMockPlayer(GameType.SURVIVAL);
    p.setPos(h.absolutePos(POS).offset(2, 0, 0).getCenter());
    return p;
  }

  private static SawbenchBlockEntity bench(GameTestHelper h) {
    h.setBlock(POS, ArchitectureCraft.SAWBENCH);
    return h.getBlockEntity(POS, SawbenchBlockEntity.class);
  }

  private static ShapeBlockEntity shape(GameTestHelper h, BlockPos pos, Shape type) {
    h.setBlock(pos, ArchitectureCraft.SHAPE);
    var be = h.getBlockEntity(pos, ShapeBlockEntity.class);
    be.setData(new ShapeData(type.id, Blocks.BRICKS.defaultBlockState(), Optional.empty(), 0, 0));
    return be;
  }

  private static ShapeData material(Shape shape) {
    return new ShapeData(shape.id, Blocks.BRICKS.defaultBlockState(), Optional.empty(), 0, 0);
  }

  private static ShapeData stackData(ItemStack stack) {
    return ShapeData.fromStack(stack);
  }

  private static void select(SawbenchMenu menu, Player player, int page, int slot) {
    menu.clickMenuButton(player, ShapePage.button(page, slot));
  }

  private static ShapeBlockEntity load(GameTestHelper h, ShapeBlockEntity be) {
    return (ShapeBlockEntity)
        BlockEntity.loadStatic(
            be.getBlockPos(),
            be.getBlockState(),
            be.saveWithFullMetadata(h.getLevel().registryAccess()),
            h.getLevel().registryAccess());
  }

  @GameTest
  public void previewDoesNotChargeUntilExtraction(GameTestHelper h) {
    var be = bench(h);
    be.setItem(0, new ItemStack(Blocks.BRICKS, 4));
    check(
        h,
        be.getItem(1).getCount() == 2 && be.previewPending(),
        "Roof result appears automatically as an unpaid preview");
    check(h, be.getItem(0).getCount() == 4, "Displaying output does not consume material");
    ItemStack first = be.removeItem(1, 1);
    check(
        h,
        first.getCount() == 1 && be.getItem(0).getCount() == 3,
        "Taking half charges exactly one batch");
    check(
        h, !be.previewPending() && be.getItem(1).getCount() == 1, "Remaining half is already paid");
    be.removeItem(1, 1);
    check(
        h,
        be.getItem(0).getCount() == 3 && be.previewPending() && be.getItem(1).getCount() == 2,
        "Finishing the paid batch generates the next preview without charging again");
    h.succeed();
  }

  @GameTest
  public void selectionsAreTheOriginalEightRememberedPages(GameTestHelper h) {
    var be = bench(h);
    Player player = player(h);
    var menu = new SawbenchMenu(1, player.getInventory(), be);
    select(menu, player, 0, 8);
    select(menu, player, 1, 6);
    select(menu, player, 0, be.selectedSlot(0));
    check(
        h,
        be.page() == 0 && be.selectedSlot(0) == 8 && be.selectedSlot(1) == 6,
        "Each category remembers its slot");
    check(h, be.selectedShape() == Shape.RoofOverhangOuterCorner, "Original roofing slot order");
    check(
        h,
        !menu.clickMenuButton(player, ShapePage.button(3, 3)),
        "Window category rejects nonexistent slot");
    check(
        h,
        !menu.clickMenuButton(player, -1) && !menu.clickMenuButton(player, 1000),
        "Invalid packets are rejected");
    check(
        h,
        menu.getSlot(0).x == 12
            && menu.getSlot(0).y == 19
            && menu.getSlot(1).x == 12
            && menu.getSlot(1).y == 57,
        "Original input/output layout");
    check(
        h,
        menu.getSlot(2).x == 8 && menu.getSlot(2).y == 143 && menu.getSlot(29).y == 200,
        "Original inventory and hotbar layout");
    h.succeed();
  }

  @GameTest
  public void paidPartialBatchSurvivesMaterialAndSelectionChanges(GameTestHelper h) {
    var be = bench(h);
    be.setItem(0, new ItemStack(Blocks.BRICKS, 8));
    be.removeItem(1, 1);
    be.setItem(0, new ItemStack(Blocks.QUARTZ_BLOCK, 4));
    be.select(1, 0);
    check(
        h,
        be.getItem(1).getCount() == 1 && stackData(be.getItem(1)).base().is(Blocks.BRICKS),
        "Paid brick remainder is preserved");
    be.removeItem(1, 1);
    check(
        h,
        be.getItem(0).getCount() == 4
            && stackData(be.getItem(1)).shape() == Shape.Cylinder
            && stackData(be.getItem(1)).base().is(Blocks.QUARTZ_BLOCK),
        "New preview uses current category and material without charging");
    h.succeed();
  }

  @GameTest
  public void failedShiftTransferDoesNotConsumeAnything(GameTestHelper h) {
    var be = bench(h);
    Player player = player(h);
    var menu = new SawbenchMenu(1, player.getInventory(), be);
    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Blocks.STONE, 64));
    be.setItem(0, new ItemStack(Blocks.BRICKS, 4));
    check(h, menu.quickMoveStack(player, 1).isEmpty(), "Full inventory rejects transfer");
    check(
        h,
        be.getItem(0).getCount() == 4 && be.previewPending() && be.getItem(1).getCount() == 2,
        "Failed extraction leaves preview and input unchanged");
    h.succeed();
  }

  @GameTest
  public void partialShiftTransferChargesOnlyOnce(GameTestHelper h) {
    var be = bench(h);
    Player player = player(h);
    var menu = new SawbenchMenu(1, player.getInventory(), be);
    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Blocks.STONE, 64));
    player.getInventory().setItem(8, material(Shape.RoofTile).stack(63));
    be.setItem(0, new ItemStack(Blocks.BRICKS, 4));
    menu.quickMoveStack(player, 1);
    check(
        h,
        be.getItem(0).getCount() == 3 && be.getItem(1).getCount() == 1 && !be.previewPending(),
        "One transferred item pays for one batch and preserves the remainder");
    check(
        h,
        player.getInventory().getItem(8).getCount() == 64,
        "Only the available inventory space was used");
    h.succeed();
  }

  @GameTest
  public void rightClickAndHotbarSwapUseVanillaExtraction(GameTestHelper h) {
    var be = bench(h);
    Player player = player(h);
    var menu = new SawbenchMenu(1, player.getInventory(), be);
    player.containerMenu = menu;
    be.setItem(0, new ItemStack(Blocks.BRICKS, 4));
    menu.clicked(1, 1, ClickType.PICKUP, player);
    check(
        h,
        menu.getCarried().getCount() == 1 && be.getItem(0).getCount() == 3,
        "Right click takes half and charges once");
    menu.setCarried(ItemStack.EMPTY);
    menu.clicked(1, 0, ClickType.SWAP, player);
    check(
        h,
        player.getInventory().getItem(0).getCount() == 1 && be.getItem(0).getCount() == 3,
        "Hotbar swap extracts the paid remainder without charging again");
    h.succeed();
  }

  @GameTest
  public void insufficientSlabsDoNotProduceAPreview(GameTestHelper h) {
    var be = bench(h);
    be.setItem(0, new ItemStack(Blocks.OAK_SLAB, 1));
    check(h, be.getItem(1).isEmpty(), "A single slab cannot pay a full-block batch");
    be.setItem(0, new ItemStack(Blocks.OAK_SLAB, 2));
    check(h, be.getItem(1).getCount() == 2, "Two slabs enable the original roof yield");
    be.removeItem(1, 2);
    check(h, be.getItem(0).isEmpty(), "Exactly two slabs consumed");
    h.succeed();
  }

  @GameTest
  public void claddingUsesOriginalOtherPageAndYield(GameTestHelper h) {
    var be = bench(h);
    be.select(6, 0);
    be.setItem(0, new ItemStack(Blocks.BRICKS, 3));
    check(
        h,
        be.getItem(1).is(ArchitectureCraft.CLADDING) && be.getItem(1).getCount() == 1,
        "Other / Cladding produces one sheet, not the invented sixteen-sheet recipe");
    be.removeItem(1, 1);
    check(h, be.getItem(0).getCount() == 2, "One sheet costs one input block");
    h.succeed();
  }

  @GameTest
  public void selectionPreviewAndPaidRemainderPersist(GameTestHelper h) {
    var be = bench(h);
    be.select(0, 8);
    be.select(1, 6);
    be.select(0, 8);
    be.setItem(0, new ItemStack(Blocks.BRICKS, 4));
    be.removeItem(1, 1);
    var loaded =
        (SawbenchBlockEntity)
            BlockEntity.loadStatic(
                be.getBlockPos(),
                be.getBlockState(),
                be.saveWithFullMetadata(h.getLevel().registryAccess()),
                h.getLevel().registryAccess());
    check(
        h,
        loaded.page() == 0 && loaded.selectedSlot(0) == 8 && loaded.selectedSlot(1) == 6,
        "Per-page selection survives save/load");
    check(
        h,
        !loaded.previewPending()
            && loaded.getItem(1).getCount() == 2
            && loaded.getItem(0).getCount() == 3,
        "Paid partial batch persists without charging or regenerating");
    h.succeed();
  }

  @GameTest
  public void twoMenusShareAuthoritativeSelectionAndInventory(GameTestHelper h) {
    var be = bench(h);
    Player a = player(h), b = player(h);
    var first = new SawbenchMenu(1, a.getInventory(), be);
    var second = new SawbenchMenu(2, b.getInventory(), be);
    be.setItem(0, new ItemStack(Blocks.BRICKS, 1));
    select(first, a, 0, 8);
    check(
        h,
        second.selectedShape() == Shape.RoofOverhangOuterCorner,
        "All viewers share the selected shape");
    first.quickMoveStack(a, 1);
    check(
        h,
        second.quickMoveStack(b, 1).isEmpty() && be.getItem(0).isEmpty(),
        "A second viewer cannot extract the same preview twice");
    h.succeed();
  }

  @GameTest
  public void automationCannotExtractUnpaidOutput(GameTestHelper h) {
    var be = bench(h);
    be.setItem(0, new ItemStack(Blocks.BRICKS, 1));
    check(
        h,
        Arrays.equals(be.getSlotsForFace(Direction.UP), new int[] {0}),
        "Top exposes material slot");
    for (Direction d : Direction.values())
      check(
          h, !be.canTakeItemThroughFace(1, be.getItem(1), d), "Output automation remains disabled");
    check(
        h,
        !ArchitectureCraft.validMaterial(new ItemStack(Blocks.DANDELION)),
        "Plants do not become architectural building material");
    h.succeed();
  }

  @GameTest
  public void collisionIsDynamicAndUsesOriginalMasks(GameTestHelper h) {
    var pole = shape(h, POS, Shape.Pole);
    var roof = shape(h, POS.east(), Shape.RoofTile);
    var level = h.getLevel();
    var poleBounds = pole.getBlockState().getCollisionShape(level, pole.getBlockPos()).bounds();
    check(
        h,
        Math.abs(poleBounds.getXsize() - .25) < 1e-9,
        "Pole collision uses the 0x102 legacy format, not a cached default roof");
    var collision = roof.getBlockState().getCollisionShape(level, roof.getBlockPos());
    double volume =
        collision.toAabbs().stream()
            .mapToDouble(b -> b.getXsize() * b.getYsize() * b.getZsize())
            .sum();
    check(h, Math.abs(volume - .75) < 1e-9, "Roof collision uses original 0xcf cubelets");
    check(
        h,
        roof.getBlockState().isFaceSturdy(level, roof.getBlockPos(), Direction.DOWN),
        "Full bottom face supports blocks");
    check(
        h,
        !pole.getBlockState().isFaceSturdy(level, pole.getBlockPos(), Direction.UP),
        "Narrow post has no full supporting face");
    h.succeed();
  }

  @GameTest
  public void shapeDropsDetachSecondaryWhilePickDoesNotDuplicateIt(GameTestHelper h) {
    var be = shape(h, POS, Shape.ArchD3A);
    ShapeData data =
        new ShapeData(
            63,
            Blocks.BRICKS.defaultBlockState(),
            Optional.of(Blocks.QUARTZ_BLOCK.defaultBlockState()),
            5,
            3);
    be.setData(data);
    var drops = Block.getDrops(be.getBlockState(), h.getLevel(), be.getBlockPos(), be);
    check(
        h,
        drops.size() == 2 && drops.get(1).is(ArchitectureCraft.CLADDING),
        "Break returns shape and separate cladding");
    check(
        h,
        stackData(drops.getFirst()).secondary().isEmpty()
            && stackData(drops.getFirst()).offset() == 0
            && stackData(drops.getFirst()).disconnected() == 0,
        "Shape drop does not retain or duplicate attached material/state");
    var picked = be.getBlockState().getCloneItemStack(h.getLevel(), be.getBlockPos(), false);
    check(
        h,
        stackData(picked).secondary().isEmpty()
            && stackData(picked).disconnected() == 5
            && stackData(picked).offset() == 0,
        "Pick block retains disconnections but omits cladding and placement offset");
    check(h, load(h, be).data().equals(data), "Placed shape still persists full data");
    h.succeed();
  }

  @GameTest
  public void miningSpeedTracksBaseMaterial(GameTestHelper h) {
    var be = shape(h, POS, Shape.RoofTile);
    Player player = player(h);
    var level = h.getLevel();
    for (Block block : new Block[] {Blocks.OAK_PLANKS, Blocks.OBSIDIAN, Blocks.STONE}) {
      be.setData(new ShapeData(0, block.defaultBlockState(), Optional.empty(), 0, 0));
      float shape = be.getBlockState().getDestroyProgress(player, level, be.getBlockPos()),
          expected = block.defaultBlockState().getDestroyProgress(player, level, be.getBlockPos());
      check(h, Math.abs(shape - expected) < 1e-8, "Mining speed follows " + block);
    }
    h.succeed();
  }

  @GameTest
  public void placementUsesHitSymmetryAndOriginalProfiles(GameTestHelper h) {
    BlockPos target = h.absolutePos(POS);
    var world = h.getLevel();
    var corner =
        ShapePlacement.orient(
            Shape.RoofOuterCorner, world, target, Direction.UP, new Vec3(-.4, -.5, .4), false);
    check(h, corner.side() == 1 && corner.turn() == 0, "Corner turn follows hit quadrant");
    var upper =
        ShapePlacement.orient(
            Shape.RoofTile, world, target, Direction.NORTH, new Vec3(.1, .3, .5), false);
    check(h, upper.side() == 0, "Upper side click places a roof upside down");
    var arch =
        ShapePlacement.orient(
            Shape.ArchD1, world, target, Direction.UP, new Vec3(.1, -.5, .4), false);
    check(
        h,
        arch.side() == 0,
        "Underneath flag places arch upright relative to its mounting surface");
    var neighbor = shape(h, POS, Shape.Stairs);
    world.setBlock(
        neighbor.getBlockPos(), neighbor.getBlockState().setValue(ShapeBlock.TURN, 1), 3);
    var plan =
        ShapePlacement.orient(
            Shape.Stairs,
            world,
            neighbor.getBlockPos().north(),
            Direction.NORTH,
            new Vec3(0, 0, .5),
            false);
    check(
        h,
        ShapeProfiles.matches(
            ShapeProfiles.global(Shape.Stairs, plan, 3),
            ShapeProfiles.global(
                Shape.Stairs,
                ShapeGeometry.orientation(neighbor.getBlockState(), neighbor.data()),
                2)),
        "Matching stair profiles align against their neighbor");
    h.succeed();
  }

  @GameTest
  public void stairMountedBanistersUseClickedOffset(GameTestHelper h) {
    h.setBlock(
        POS,
        Blocks.OAK_STAIRS
            .defaultBlockState()
            .setValue(StairBlock.FACING, Direction.EAST)
            .setValue(StairBlock.HALF, Half.BOTTOM));
    var target = h.absolutePos(POS.above());
    var a =
        ShapePlacement.orient(
            Shape.BanisterPlain, h.getLevel(), target, Direction.UP, new Vec3(-.4, -.5, .2), false);
    var b =
        ShapePlacement.orient(
            Shape.BanisterPlain, h.getLevel(), target, Direction.UP, new Vec3(.4, -.5, -.2), false);
    check(
        h,
        a.side() == 1 && Math.abs(a.offset()) == .375,
        "Banister mounts to upright vanilla stairs at its original six-pixel offset");
    check(
        h,
        a.turn() == b.turn() && a.offset() == -b.offset(),
        "Opposite hit sides choose opposite offsets on the same stair orientation");
    h.succeed();
  }

  @GameTest
  public void chiselZonesAndHammerRetainOriginalBehavior(GameTestHelper h) {
    var be = shape(h, POS, Shape.RoofTile);
    var other = shape(h, POS.east(), Shape.RoofTile);
    Player player = player(h);
    be.setData(be.data().withSecondary(Blocks.QUARTZ_BLOCK.defaultBlockState()));
    BlockPos p = be.getBlockPos();
    var centre = new BlockHitResult(p.getCenter().add(0, .5, 0), Direction.UP, p, false);
    ArchitectureCraft.CHISEL.useOn(
        new UseOnContext(
            h.getLevel(),
            player,
            InteractionHand.MAIN_HAND,
            new ItemStack(ArchitectureCraft.CHISEL),
            centre));
    check(h, be.data().secondary().isEmpty(), "Center chisel removes cladding");
    var edge = new BlockHitResult(p.getCenter().add(.4, .5, 0), Direction.UP, p, false);
    ArchitectureCraft.CHISEL.useOn(
        new UseOnContext(
            h.getLevel(),
            player,
            InteractionHand.MAIN_HAND,
            new ItemStack(ArchitectureCraft.CHISEL),
            edge));
    check(
        h,
        (be.data().disconnected() & (1 << 5)) != 0 && (other.data().disconnected() & (1 << 4)) != 0,
        "Edge zone toggles both ends without requiring sneak");
    be.setData(be.data().withOffset(6));
    int turn = be.getBlockState().getValue(ShapeBlock.TURN);
    var hammer =
        new UseOnContext(
            h.getLevel(),
            player,
            InteractionHand.MAIN_HAND,
            new ItemStack(ArchitectureCraft.HAMMER),
            centre);
    ArchitectureCraft.HAMMER.useOn(hammer);
    check(
        h,
        be.data().offset() == -6 && be.getBlockState().getValue(ShapeBlock.TURN) == turn,
        "First hammer flips positive offset without turning");
    ArchitectureCraft.HAMMER.useOn(hammer);
    check(
        h,
        be.data().offset() == 6 && be.getBlockState().getValue(ShapeBlock.TURN) == (turn + 1) % 4,
        "Second hammer turns after restoring positive offset");
    player.setShiftKeyDown(true);
    ArchitectureCraft.HAMMER.useOn(hammer);
    check(
        h,
        be.getBlockState().getValue(ShapeBlock.FACING) == Direction.DOWN,
        "Sneak hammer uses legacy mounting order");
    h.succeed();
  }

  @GameTest
  public void chiselHarvestsFragileBlocks(GameTestHelper h) {
    Player player = player(h);
    for (Block block :
        new Block[] {Blocks.GLASS, Blocks.GLASS_PANE, Blocks.GLOWSTONE, Blocks.ICE}) {
      h.setBlock(POS, block);
      var p = h.absolutePos(POS);
      var hit = new BlockHitResult(p.getCenter(), Direction.UP, p, false);
      check(
          h,
          ArchitectureCraft.CHISEL
              .useOn(
                  new UseOnContext(
                      h.getLevel(),
                      player,
                      InteractionHand.MAIN_HAND,
                      new ItemStack(ArchitectureCraft.CHISEL),
                      hit))
              .consumesAction(),
          "Chisel handles fragile block");
      check(h, h.getLevel().getBlockState(p).isAir(), "Fragile block removed intact");
    }
    h.succeed();
  }

  @GameTest
  public void windowsConnectAcrossPlainAndCornerFrames(GameTestHelper h) {
    var corner = shape(h, POS, Shape.WindowCorner);
    var plain = shape(h, POS.south(), Shape.WindowFrame);
    h.getLevel()
        .setBlock(plain.getBlockPos(), plain.getBlockState().setValue(ShapeBlock.TURN, 1), 3);
    var a = ShapeGeometry.entry(corner.data(), corner.getBlockState());
    var b = ShapeGeometry.entry(plain.data(), plain.getBlockState());
    check(
        h,
        WindowTopology.matches(a, b, 3),
        "Plain window joins matching edge of a corner window despite different rotations");
    plain.setData(plain.data().withConnections(1 << 2));
    check(
        h,
        !WindowTopology.matches(a, ShapeGeometry.entry(plain.data(), plain.getBlockState()), 3),
        "Disconnection mask stops that join");
    h.succeed();
  }

  @GameTest
  public void windowSelectionKeepsFramesButWalkingCollisionOmitsInternalFrames(GameTestHelper h) {
    var bottom = shape(h, POS, Shape.WindowFrame);
    shape(h, POS.above(), Shape.WindowFrame);
    var state = bottom.getBlockState();
    var level = h.getLevel();
    double selectHeight = state.getShape(level, bottom.getBlockPos()).bounds().maxY,
        collisionHeight =
            state.getCollisionShape(level, bottom.getBlockPos()).toAabbs().stream()
                .filter(b -> b.minX > .1 && b.maxX < .9)
                .mapToDouble(b -> b.maxY)
                .max()
                .orElse(0);
    check(
        h,
        selectHeight == 1 && collisionHeight < 1,
        "Connected top frame stays selectable without obstructing the open window");
    bottom.setData(bottom.data().withSecondary(Blocks.GLASS_PANE.defaultBlockState()));
    check(
        h,
        !state.getCollisionShape(level, bottom.getBlockPos()).toAabbs().stream()
            .filter(b -> b.minX < .5 && b.maxX > .5 && b.minY < .5 && b.maxY > .5)
            .toList()
            .isEmpty(),
        "Glazing adds center collision");
    h.succeed();
  }

  @GameTest
  public void shapesUseComponentsOnRealPlacement(GameTestHelper h) {
    h.setBlock(POS, Blocks.STONE);
    Player player = player(h);
    ShapeData data = material(Shape.Cylinder);
    ItemStack stack = data.stack(2);
    BlockPos absolute = h.absolutePos(POS);
    var hit = new BlockHitResult(absolute.getCenter().add(0, .5, 0), Direction.UP, absolute, false);
    stack
        .getItem()
        .useOn(new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
    check(
        h,
        h.getBlockEntity(POS.above(), ShapeBlockEntity.class).data().equals(data)
            && stack.getCount() == 1,
        "Actual survival placement retains component data and consumes one item");
    var buf =
        new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
    try {
      ArchitectureCraft.SHAPE_DATA.streamCodec().encode(buf, data);
      check(
          h,
          ArchitectureCraft.SHAPE_DATA.streamCodec().decode(buf).equals(data),
          "Registry-aware component network roundtrip");
    } finally {
      buf.release();
    }
    h.succeed();
  }

  @GameTest
  public void allShapesAndRecipesLoad(GameTestHelper h) {
    var be = shape(h, POS, Shape.RoofTile);
    for (Shape s : Shape.values()) {
      if (s == Shape.CladdingSheet) continue;
      be.setData(material(s));
      check(
          h,
          !be.getBlockState().getShape(h.getLevel(), be.getBlockPos()).isEmpty(),
          s + " selection geometry");
      check(
          h,
          !be.getBlockState().getCollisionShape(h.getLevel(), be.getBlockPos()).isEmpty(),
          s + " collision geometry");
    }
    for (String name :
        new String[] {"sawbench", "sawblade", "large_pulley", "hammer", "chisel", "glowbrush"})
      check(
          h,
          h.getLevel()
              .getServer()
              .getRecipeManager()
              .byKey(ResourceKey.create(Registries.RECIPE, ArchitectureCraft.id(name)))
              .isPresent(),
          name + " recipe loaded");
    h.succeed();
  }

  @GameTest
  public void secondaryApplicationMatchesOriginalMaterialRules(GameTestHelper h) {
    var be = shape(h, POS, Shape.RoofTile);
    Player player = player(h);
    BlockPos p = be.getBlockPos();
    var hit = new BlockHitResult(p.getCenter().add(0, .5, 0), Direction.UP, p, false);
    ItemStack cladding =
        new ShapeData(60, Blocks.QUARTZ_BLOCK.defaultBlockState(), Optional.empty(), 0, 0).stack(2);
    be.getBlockState().useItemOn(cladding, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    check(
        h,
        cladding.getCount() == 1 && be.data().secondary().orElseThrow().is(Blocks.QUARTZ_BLOCK),
        "Cladding applies to a secondary-capable roof");
    be.getBlockState().useItemOn(cladding, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    check(h, cladding.getCount() == 1, "Already clad shape consumes no additional sheets");
    be.setData(material(Shape.WindowFrame));
    ItemStack iron = new ItemStack(Blocks.IRON_BARS, 2),
        glass = new ItemStack(Blocks.GLASS, 2),
        pane = new ItemStack(Blocks.RED_STAINED_GLASS_PANE, 2);
    check(
        h,
        !be.getBlockState()
            .useItemOn(iron, h.getLevel(), player, InteractionHand.MAIN_HAND, hit)
            .consumesAction(),
        "Iron bars cannot be used as glass");
    check(
        h,
        !be.getBlockState()
            .useItemOn(glass, h.getLevel(), player, InteractionHand.MAIN_HAND, hit)
            .consumesAction(),
        "Original window glazing requires panes");
    be.getBlockState().useItemOn(pane, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    check(
        h,
        pane.getCount() == 1
            && be.data().secondary().orElseThrow().is(Blocks.RED_STAINED_GLASS_PANE),
        "Colored glass pane applies once");
    var drops = Block.getDrops(be.getBlockState(), h.getLevel(), p, be);
    check(
        h,
        drops.size() == 2 && drops.get(1).is(Blocks.RED_STAINED_GLASS_PANE.asItem()),
        "Window drops its original colored glazing separately");
    h.succeed();
  }

  @GameTest
  public void glowBrushRetainsStateAndSurvivesDropAndSave(GameTestHelper h) {
    var be = shape(h, POS, Shape.BanisterPlain);
    ShapeData original =
        be.data()
            .withOffset(-6)
            .withConnections(9)
            .withSecondary(Blocks.QUARTZ_BLOCK.defaultBlockState());
    be.setData(original);
    Player player = player(h);
    BlockPos p = be.getBlockPos();
    var hit = new BlockHitResult(p.getCenter(), Direction.UP, p, false);
    var use =
        new UseOnContext(
            h.getLevel(),
            player,
            InteractionHand.MAIN_HAND,
            new ItemStack(ArchitectureCraft.GLOWBRUSH),
            hit);
    ArchitectureCraft.GLOWBRUSH.useOn(use);
    check(
        h,
        be.getBlockState().getLightEmission() == 15 && be.data().equals(original),
        "Glowbrush changes luminance without discarding shape state");
    var drops = Block.getDrops(be.getBlockState(), h.getLevel(), p, be);
    check(
        h,
        "true".equals(drops.getFirst().get(DataComponents.BLOCK_STATE).properties().get("glowing")),
        "Glow survives the shape drop");
    check(h, load(h, be).data().equals(original), "Full data survives save/load");
    player.setShiftKeyDown(true);
    ArchitectureCraft.GLOWBRUSH.useOn(use);
    check(
        h,
        be.getBlockState().getLightEmission() == 0 && be.data().equals(original),
        "Sneaking removes glow only");
    h.succeed();
  }

  @GameTest
  public void transparentFullCubesWorkWithoutGlassInheritanceOrTags(GameTestHelper h) {
    var block = MaterialTestBlocks.TRANSPARENT_CUBE;
    var state = block.defaultBlockState();
    check(
        h,
        !state.canOcclude() && !state.is(ArchitectureCraft.SAWBENCH_MATERIALS),
        "Regression material is transparent and is not whitelisted");
    var be = bench(h);
    for (var material :
        List.of(
            block,
            Blocks.GLASS,
            Blocks.WHITE_STAINED_GLASS,
            Blocks.TINTED_GLASS,
            Blocks.ICE,
            Blocks.STONE,
            Blocks.OAK_SLAB)) {
      be.setItem(0, new ItemStack(material, 8));
      check(h, !be.getItem(1).isEmpty(), "Expected generic material acceptance: " + material);
      var result = be.removeItem(1, be.getItem(1).getCount());
      check(h, stackData(result).base().is(material), "Crafted shape retains its source material");
      check(
          h,
          be.getItem(0).getCount() == (material == Blocks.OAK_SLAB ? 6 : 7),
          "Extraction charges the correct cost");
    }
    for (var material :
        List.of(
            Blocks.CHEST,
            Blocks.FURNACE,
            Blocks.GLASS_PANE,
            Blocks.OAK_FENCE,
            Blocks.DANDELION,
            ArchitectureCraft.SHAPE,
            ArchitectureCraft.SAWBENCH)) {
      check(
          h,
          !ArchitectureCraft.validMaterial(new ItemStack(material)),
          "Excluded non-material: " + material);
    }
    h.succeed();
  }

  @GameTest
  public void shapesDoNotOccludeLightingAsSolidMaterialCubes(GameTestHelper h) {
    var be = shape(h, POS, Shape.Slab);
    for (var material : List.of(Blocks.GLASS, Blocks.WHITE_STAINED_GLASS, Blocks.STONE)) {
      be.setData(
          new ShapeData(Shape.Slab.id, material.defaultBlockState(), Optional.empty(), 0, 0));
      check(
          h,
          be.getBlockState().getShadeBrightness(h.getLevel(), be.getBlockPos()) == 1
              && be.getBlockState().getLightEmission() == 0,
          "Shapes do not invent solid-cube AO or emit light: " + material);
    }
    h.succeed();
  }

  @GameTest
  public void nativeMaterialOverridesAndStatefulMaterialsWork(GameTestHelper h) {
    check(
        h,
        ArchitectureCraft.validMaterial(new ItemStack(Blocks.OAK_SAPLING)),
        "Test datapack explicitly allows a nonstandard material");
    check(
        h,
        Blocks.REDSTONE_BLOCK.defaultBlockState().is(ArchitectureCraft.EMISSIVE_MATERIALS),
        "Emissive material override is loaded as a synchronized block tag");
    var be = bench(h);
    ItemStack input = new ItemStack(Blocks.OAK_LOG, 1);
    input.set(
        DataComponents.BLOCK_STATE,
        new net.minecraft.world.item.component.BlockItemStateProperties(Map.of("axis", "x")));
    be.setItem(0, input);
    check(
        h,
        stackData(be.getItem(1)).base().getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
        "Sawbench preserves material state components");
    h.succeed();
  }

  @GameTest
  public void breakingAnUnpaidPreviewCannotCreateFreeOutput(GameTestHelper h) {
    var be = bench(h);
    be.setItem(0, new ItemStack(Blocks.BRICKS, 1));
    be.preRemoveSideEffects(be.getBlockPos(), be.getBlockState());
    var entities =
        h.getLevel()
            .getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new AABB(be.getBlockPos()).inflate(2));
    check(
        h,
        entities.stream().anyMatch(e -> e.getItem().is(Blocks.BRICKS.asItem())),
        "Remaining material is dropped");
    check(
        h,
        entities.stream().noneMatch(e -> e.getItem().has(ArchitectureCraft.SHAPE_DATA)),
        "Unpaid virtual result is never dropped");
    h.succeed();
  }
}

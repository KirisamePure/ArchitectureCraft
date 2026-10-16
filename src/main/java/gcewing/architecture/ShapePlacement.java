package gcewing.architecture;

import gcewing.architecture.shape.*;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

/** One placement calculation shared by real placement and the client preview. */
public final class ShapePlacement {
  public record Plan(BlockState state, ShapeData data) {}

  private ShapePlacement() {}

  public static Plan plan(BlockPlaceContext context) {
    ShapeData data = ShapeData.fromStack(context.getItemInHand());
    var level = context.getLevel();
    var pos = context.getClickedPos();
    Direction face = context.getClickedFace();
    boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
    Vec3 hit = context.getClickLocation().subtract(pos.getCenter());
    Orientation orientation = orient(data.shape(), level, pos, face, hit, sneaking);
    BlockState state =
        ArchitectureCraft.SHAPE
            .defaultBlockState()
            .setValue(ShapeBlock.FACING, Direction.from3DDataValue(orientation.side()))
            .setValue(ShapeBlock.TURN, orientation.turn())
            .setValue(ShapeBlock.GLOWING, data.shape().glowing());
    var saved =
        context.getItemInHand().get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
    if (saved != null) state = saved.apply(state);
    return new Plan(state, data.withOffset((int) (orientation.offset() * 16)));
  }

  public static Orientation orient(
      Shape shape, BlockGetter world, BlockPos pos, Direction face, Vec3 hit, boolean sneaking) {
    BlockPos neighborPos = pos.relative(face.getOpposite());
    BlockState neighborState = world.getBlockState(neighborPos);
    ShapeBlockEntity neighbor =
        world.getBlockEntity(neighborPos) instanceof ShapeBlockEntity be ? be : null;
    if (!sneaking) {
      if (shape.banister) {
        Orientation stair = null;
        if (neighborState.getBlock() instanceof StairBlock
            && (face == Direction.UP || face == Direction.DOWN)) {
          int side = neighborState.getValue(StairBlock.HALF) == Half.TOP ? 1 : 0;
          int turn =
              Orientation.turnToFace(
                  3, neighborState.getValue(StairBlock.FACING).getOpposite().get3DDataValue());
          if (side == 1 && (turn & 1) == 0) turn ^= 2;
          stair = Orientation.legacy(side, turn, 0);
        } else if (neighbor != null)
          stair = ShapeGeometry.orientation(neighbor.getBlockState(), neighbor.data());
        if (stair != null && stair.legacySide() == face.getOpposite().get3DDataValue())
          return withOffset(shape, stair, hit);
      }
      if (shape.window()) {
        Orientation window = windowPlacement(shape, world, pos, face, hit, neighbor);
        if (window != null) return window;
      }
      if (neighbor != null) {
        Orientation other = ShapeGeometry.orientation(neighbor.getBlockState(), neighbor.data());
        String profile =
            ShapeProfiles.global(neighbor.data().shape(), other, face.get3DDataValue());
        if (profile != null)
          for (int i = 0; i < 4; i++) {
            Orientation candidate =
                new Orientation(other.side(), (other.turn() + i) & 3, other.offset());
            if (ShapeProfiles.matches(
                ShapeProfiles.global(shape, candidate, face.getOpposite().get3DDataValue()),
                profile)) return candidate;
          }
      }
    }
    int right = shape.underneath ? 1 : 0, upside = shape.underneath ? 0 : 1, side;
    if (face == Direction.UP) side = right;
    else if (face == Direction.DOWN) side = shape.window() ? right : upside;
    else if (sneaking) side = face.getOpposite().get3DDataValue();
    else side = hit.y > 0 && !shape.window() ? upside : right;
    Orientation base = Orientation.legacy(side, 0, 0);
    double[] local = base.inverseVector(hit.x, hit.y, hit.z);
    int turn = turn(shape.symmetry, local[0], local[2]);
    return withOffset(shape, Orientation.legacy(side, turn, 0), hit);
  }

  public static int turn(Shape.Symmetry symmetry, double x, double z) {
    return switch (symmetry) {
      case Quadrilateral, None -> 0;
      case Bilateral -> Math.abs(z) > Math.abs(x) ? z < 0 ? 2 : 0 : x > 0 ? 1 : 3;
      case Unilateral -> z > 0 ? x < 0 ? 0 : 1 : x > 0 ? 2 : 3;
    };
  }

  private static Orientation withOffset(Shape shape, Orientation orientation, Vec3 hit) {
    if (!shape.offset) return orientation;
    double[] h = orientation.inverseVector(hit.x, hit.y, hit.z);
    double offset = shape.placementOffset();
    return new Orientation(
        orientation.side(), orientation.turn(), (int) (16 * (h[0] < 0 ? -offset : offset)) / 16.);
  }

  private static Orientation windowPlacement(
      Shape shape,
      BlockGetter world,
      BlockPos pos,
      Direction face,
      Vec3 hit,
      ShapeBlockEntity neighbor) {
    if (neighbor != null && neighbor.data().shape().window()) {
      Shape other = neighbor.data().shape();
      Orientation o = ShapeGeometry.orientation(neighbor.getBlockState(), neighbor.data());
      int local = o.localFace(face.get3DDataValue());
      int kind = WindowTopology.frameKind(other, local);
      if (shape != Shape.WindowCorner) {
        if (other != Shape.WindowCorner) return new Orientation(o.side(), o.turn(), 0);
        if (kind == 1) {
          int target = face.getOpposite().get3DDataValue();
          if (local == 3) return new Orientation(o.side(), Orientation.turnToFace(4, target), 0);
          if (local == 4) return new Orientation(o.side(), Orientation.turnToFace(5, target), 0);
        }
      } else {
        if (kind == 2) return new Orientation(o.side(), o.turn(), 0);
        if (kind == 1) {
          int normal = o.globalFace(WindowTopology.frameOrientation(local));
          for (int direction : new int[] {normal, normal ^ 1}) {
            BlockPos adjacent = pos.relative(Direction.from3DDataValue(direction ^ 1));
            if (world.getBlockEntity(adjacent) instanceof ShapeBlockEntity be
                && be.data().shape().window()) {
              Orientation candidate = ShapeGeometry.orientation(be.getBlockState(), be.data());
              if (WindowTopology.frameKind(be.data().shape(), candidate.localFace(direction)) == 2)
                return new Orientation(candidate.side(), candidate.turn(), 0);
            }
          }
        }
      }
    }
    if (neighbor == null || !neighbor.data().shape().window())
      return switch (face) {
        case EAST, WEST -> new Orientation(1, 0, 0);
        case NORTH, SOUTH -> new Orientation(1, 1, 0);
        default -> null;
      };
    return null;
  }
}

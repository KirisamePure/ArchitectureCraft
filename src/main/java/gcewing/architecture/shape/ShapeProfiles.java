package gcewing.architecture.shape;

/** Profile matching from Generic.java and Roof.java in the reference implementation. */
public final class ShapeProfiles {
  private ShapeProfiles() {}

  private static final String[] LR_STRAIGHT = {null, null, null, null, "right", "left"};
  private static final String[] LR_CORNER = {null, null, null, "left", "right", null};
  private static final String[] RL_CORNER = {null, null, "right", null, null, "left"};
  private static final String[] TB_OFFSET = {"bottom", "top", null, null, null, null};

  public static String local(Shape shape, int face) {
    if (shape.roof())
      return switch (shape) {
        case RoofTile -> face == 5 ? "roof_left" : face == 4 ? "roof_right" : "roof_none";
        case RoofOuterCorner -> face == 3 ? "roof_left" : face == 4 ? "roof_right" : "roof_none";
        case RoofInnerCorner -> face == 5 ? "roof_left" : face == 2 ? "roof_right" : "roof_none";
        case RoofRidge, RoofSmartRidge -> "ridge";
        case RoofValley, RoofSmartValley -> "valley";
        default -> "roof_none";
      };
    return switch (shape.profile) {
      case "lrStraight" -> LR_STRAIGHT[face];
      case "lrCorner" -> LR_CORNER[face];
      case "rlCorner" -> RL_CORNER[face];
      case "tbOffset" -> TB_OFFSET[face];
      default -> null;
    };
  }

  public static String global(Shape shape, Orientation orientation, int face) {
    return local(shape, orientation.localFace(face));
  }

  public static boolean matches(String a, String b) {
    if (a == null) return b == null;
    return switch (a) {
      case "left" -> "right".equals(b);
      case "right" -> "left".equals(b);
      case "bottom" -> "top".equals(b);
      case "top" -> "bottom".equals(b);
      case "roof_left" -> "roof_right".equals(b);
      case "roof_right" -> "roof_left".equals(b);
      default -> a.equals(b);
    };
  }
}

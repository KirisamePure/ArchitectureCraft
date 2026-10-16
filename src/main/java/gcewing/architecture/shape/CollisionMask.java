package gcewing.architecture.shape;

import java.util.*;

/** Exact legacy collision formats, rather than the first port's sampled roof approximation. */
public final class CollisionMask {
  private CollisionMask() {}

  public static List<Mesh.Box> boxes(Shape shape) {
    int mask = shape.occlusionMask, p = mask & 255;
    List<Mesh.Box> boxes = new ArrayList<>();
    if (!shape.roof() && mask == 0) return Mesh.model("shape/" + shape.model).boxes();
    switch (mask & 0xff00) {
      case 0 -> {
        for (int i = 0; i < 8; i++)
          if ((mask & (1 << i)) != 0) {
            double x = (i & 1) != 0 ? .5 : 0, y = (i & 4) != 0 ? .5 : 0, z = (i & 2) != 0 ? .5 : 0;
            boxes.add(new Mesh.Box(x, y, z, x + .5, y + .5, z + .5));
          }
      }
      case 0x100 -> {
        double r = p / 16.;
        boxes.add(new Mesh.Box(.5 - r, 0, .5 - r, .5 + r, 1, .5 + r));
      }
      case 0x200 -> {
        double r = p / 32.;
        boxes.add(new Mesh.Box(0, 0, .5 - r, 1, 1, .5 + r));
      }
      case 0x300 -> {
        double r = ((p & 15) + 1) / 16., h = ((p >> 4) + 1) / 16.;
        boxes.add(new Mesh.Box(0, 0, 1 - r, r, h, 1));
      }
      case 0x400, 0x500 -> {
        double r = ((p & 15) + 1) / 16., h = ((p >> 4) + 1) / 16.;
        boxes.add(new Mesh.Box(0, 0, 1 - r, 1, h, 1));
        if ((mask & 0x100) != 0) boxes.add(new Mesh.Box(0, 0, 0, r, h, 1));
      }
      default -> boxes.add(new Mesh.Box(0, 0, 0, 1, 1, 1));
    }
    return List.copyOf(boxes);
  }

  public static boolean supports(Shape shape, int face) {
    if ((shape.occlusionMask & 0xff00) != 0) return false;
    int mask =
        switch (face) {
          case 0 -> 0x0f;
          case 1 -> 0xf0;
          case 2 -> 0x33;
          case 3 -> 0xcc;
          case 4 -> 0x55;
          default -> 0xaa;
        };
    return (shape.occlusionMask & mask) == mask;
  }
}

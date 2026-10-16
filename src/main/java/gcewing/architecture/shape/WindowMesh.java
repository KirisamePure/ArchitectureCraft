package gcewing.architecture.shape;

import static gcewing.architecture.shape.WindowTopology.*;

import java.util.*;

/**
 * Direct port of RenderWindow: side frames, diagonal caps, centre ends, glazing and exposed glass
 * edges.
 */
public final class WindowMesh {
  private WindowMesh() {}

  @FunctionalInterface
  public interface Connected {
    boolean test(int x, int y, int z);
  }

  // Convenience for isolated geometry tests; production uses full 3x3x3 topology snapshots.
  public static Mesh create(Shape shape, Connected connected, boolean glass) {
    Entry self = new Entry(shape.id, 1, 0, 0, glass);
    return create(
        self,
        (x, y, z) ->
            x == 0 && y == 0 && z == 0 ? self : connected.test(x, y, z) ? self : Entry.EMPTY);
  }

  private static Mesh model(String name, boolean glass) {
    Mesh mesh = Mesh.model("shape/window_" + name);
    return glass
        ? new Mesh(
            mesh.triangles().stream().map(t -> new Mesh.Triangle(2, t.a(), t.b(), t.c())).toList(),
            mesh.boxes())
        : mesh;
  }

  private static Mesh rotated(String name, int i, boolean corner, boolean glass) {
    return MeshRotation.apply(model(name, glass), i, corner && (i == 1 || i == 2) ? -1 : 0);
  }

  public static Mesh create(Entry self, Neighborhood world) {
    Shape shape = self.shape();
    boolean corner = shape == Shape.WindowCorner, mullion = shape == Shape.WindowMullion;
    boolean[][] frame = flags(self, world);
    List<Mesh> parts = new ArrayList<>();
    if (corner || mullion) parts.add(model(corner ? "corner_centre" : "mullion_centre", false));
    for (int i = 0; i < 4; i++) {
      int j = (i + 3) & 3, k = (i + 1) & 3;
      if (frame[i][1])
        parts.add(
            rotated(
                i % 2 == 0 && (corner || mullion)
                    ? (corner ? "corner_topbot" : "mullion_topbot")
                    : "frame_side",
                i,
                corner,
                false));
      else if (i % 2 == 0 && (corner || mullion))
        parts.add(model((corner ? "corner" : "mullion") + "_centre_end" + i, false));
      if (frame[i][1] && !frame[j][1] || frame[i][0] && frame[j][2])
        parts.add(rotated("frame_end0", i, corner, false));
      if (frame[i][1] && !frame[k][1] || frame[i][2] && frame[k][0])
        parts.add(
            rotated(corner && i % 2 == 0 ? "corner_topbot_end1" : "frame_end1", i, corner, false));
      if (self.glazed() && !frame[i][1] && !frame[i][3])
        parts.add(
            rotated(
                i % 2 == 0 && (corner || mullion)
                    ? (corner ? "corner_glass_edge" : "mullion_glass_edge")
                    : "glass_edge",
                i,
                corner,
                true));
    }
    if (self.glazed()) parts.add(model(corner ? "corner_glass" : "glass", true));
    return new Mesh(Mesh.join(parts).triangles(), collision(self, world, true));
  }

  public static List<Mesh.Box> collision(Entry self, Neighborhood world, boolean selection) {
    Shape shape = self.shape();
    boolean corner = shape == Shape.WindowCorner, mullion = shape == Shape.WindowMullion;
    double r = 1 / 8., s = 3 / 32., w = 1 / 32.;
    List<Mesh.Box> boxes = new ArrayList<>();
    double[] e = new double[4];
    if (corner) boxes.add(new Mesh.Box(.5 - r, 0, .5 - r, .5 + r, 1, .5 + r));
    else if (mullion) boxes.add(new Mesh.Box(.5 - r, 0, .5 - s, .5 + r, 1, .5 + s));
    int[] sides = sides(shape);
    for (int i = 0; i < 4; i++) {
      boolean frame =
          connected(self, 0, 0, 0, self.orientation().globalFace(sides[i]), world) == null;
      if (selection || frame) {
        List<Mesh.Box> local = new ArrayList<>();
        if (corner && i % 2 == 0) {
          local.add(new Mesh.Box(0, 0, .5 - s, .5 + s, r, .5 + s));
          local.add(new Mesh.Box(.5 - s, 0, .5 - s, .5 + s, r, 1));
        } else local.add(new Mesh.Box(0, 0, .5 - s, 1, r, .5 + s));
        boxes.addAll(
            MeshRotation.apply(new Mesh(List.of(), local), i, corner && (i == 1 || i == 2) ? -1 : 0)
                .boxes());
      }
      e[i] = frame ? .5 - r : .5;
    }
    if (self.glazed()) {
      if (corner) {
        boxes.add(new Mesh.Box(.5 - e[3], .5 - e[0], .5 - w, .5 - s, .5 + e[2], .5 + w));
        boxes.add(new Mesh.Box(.5 - w, .5 - e[0], .5 + s, .5 + w, .5 + e[2], .5 + e[1]));
      } else if (mullion) {
        boxes.add(new Mesh.Box(.5 - e[3], .5 - e[0], .5 - w, .5 - r, .5 + e[2], .5 + w));
        boxes.add(new Mesh.Box(.5 + r, .5 - e[0], .5 - w, .5 + e[1], .5 + e[2], .5 + w));
      } else boxes.add(new Mesh.Box(.5 - e[3], .5 - e[0], .5 - w, .5 + e[1], .5 + e[2], .5 + w));
    }
    return List.copyOf(boxes);
  }
}

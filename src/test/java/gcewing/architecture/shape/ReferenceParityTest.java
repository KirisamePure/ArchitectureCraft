package gcewing.architecture.shape;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Golden data taken from reference commit 172a307, rather than generated from the port under test.
 */
class ReferenceParityTest {
  private static JsonArray fixture(String name) {
    return JsonParser.parseReader(
            new InputStreamReader(
                Objects.requireNonNull(
                    ReferenceParityTest.class.getResourceAsStream("/parity/" + name + ".json")),
                StandardCharsets.UTF_8))
        .getAsJsonArray();
  }

  @ParameterizedTest
  @EnumSource(Shape.class)
  void everyShapeRetainsItsReferenceMetadata(Shape shape) {
    var expected =
        fixture("shapes").asList().stream()
            .map(JsonElement::getAsJsonObject)
            .filter(x -> x.get("name").getAsString().equals(shape.name()))
            .findFirst()
            .orElseThrow();
    assertEquals(expected.get("id").getAsInt(), shape.id);
    assertEquals(expected.get("mask").getAsInt(), shape.occlusionMask);
    assertEquals(expected.get("used").getAsInt(), shape.materialUsed);
    assertEquals(expected.get("made").getAsInt(), shape.itemsProduced);
    assertEquals(expected.get("symmetry").getAsString(), shape.symmetry.name());
    assertEquals(expected.get("profile").getAsString(), shape.profile);
    assertEquals(expected.get("banister").getAsBoolean(), shape.banister);
    assertEquals(expected.get("offset").getAsBoolean(), shape.offset);
    assertEquals(expected.get("underneath").getAsBoolean(), shape.underneath);
  }

  @Test
  void originalCategoryMembershipAndOrderAreUnchanged() {
    var pages = fixture("sawbench-pages");
    assertEquals(8, ShapePage.PAGES.size());
    for (int i = 0; i < 8; i++) {
      var page = pages.get(i).getAsJsonObject();
      assertEquals(page.get("category").getAsString(), ShapePage.PAGES.get(i).key());
      assertEquals(
          page.getAsJsonArray("shapes").asList().stream().map(JsonElement::getAsString).toList(),
          ShapePage.PAGES.get(i).shapes().stream().map(Enum::name).toList());
    }
    assertEquals(Shape.Pillar, ShapePage.shape(1, 5));
    assertEquals(Shape.Pillar, ShapePage.shape(2, 1));
    assertEquals(Shape.CladdingSheet, ShapePage.shape(6, 0));
    assertFalse(ShapePage.valid(3, 3));
  }

  @Test
  void all24MountingMatricesExactlyMatchTheReference() {
    for (var element : fixture("rotations")) {
      var expected = element.getAsJsonObject();
      var o =
          Orientation.legacy(expected.get("side").getAsInt(), expected.get("turn").getAsInt(), 0);
      var m = expected.getAsJsonArray("matrix");
      for (int axis = 0; axis < 3; axis++) {
        double[] v = o.vector(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0);
        for (int row = 0; row < 3; row++)
          assertEquals(m.get(row).getAsJsonArray().get(axis).getAsDouble(), v[row], 1e-12);
      }
      for (int face = 0; face < 6; face++) assertEquals(face, o.localFace(o.globalFace(face)));
      double[] p = o.vector(.2, -.3, .4);
      assertArrayEquals(new double[] {.2, -.3, .4}, o.inverseVector(p[0], p[1], p[2]), 1e-12);
    }
  }

  @Test
  void originalCollisionFormatsAndSupportingFacesAreRetained() {
    assertEquals(6, CollisionMask.boxes(Shape.RoofTile).size());
    assertTrue(CollisionMask.supports(Shape.RoofTile, 0));
    assertTrue(CollisionMask.supports(Shape.RoofTile, 3));
    assertFalse(CollisionMask.supports(Shape.RoofTile, 1));
    var pole = CollisionMask.boxes(Shape.Pole).getFirst();
    assertEquals(.375, pole.x0());
    assertEquals(.625, pole.x1());
    assertFalse(CollisionMask.supports(Shape.Pole, 1));
    assertEquals(2, CollisionMask.boxes(Shape.CylinderQuarter).size());
    assertEquals(
        Mesh.model("shape/" + Shape.BanisterPlain.model).boxes(),
        CollisionMask.boxes(Shape.BanisterPlain));
  }

  private static WindowTopology.Entry entry(Shape shape, int side, int turn, boolean glass) {
    return new WindowTopology.Entry(shape.id, side, turn, 0, glass);
  }

  @Test
  void plainCornerAndRotatedWindowConnectionsMatchReferenceProfiles() {
    var corner = entry(Shape.WindowCorner, 1, 0, false);
    assertTrue(WindowTopology.matches(corner, entry(Shape.WindowFrame, 1, 1, false), 3));
    assertFalse(WindowTopology.matches(corner, entry(Shape.WindowFrame, 1, 0, false), 3));
    assertTrue(WindowTopology.matches(corner, entry(Shape.WindowCorner, 0, 0, false), 1));
    assertFalse(WindowTopology.matches(corner, entry(Shape.WindowCorner, 0, 2, false), 1));
    assertTrue(
        WindowTopology.matches(
            entry(Shape.WindowFrame, 1, 0, false), entry(Shape.WindowFrame, 0, 2, false), 1));
  }

  @Test
  void windowFlagsAccountForDiagonalFramesAndGlazing() {
    var self = entry(Shape.WindowFrame, 1, 0, false);
    var glazed = entry(Shape.WindowFrame, 1, 0, true);
    WindowTopology.Neighborhood eastOnly =
        (x, y, z) -> x == 1 && y == 0 && z == 0 ? glazed : WindowTopology.Entry.EMPTY;
    var flags = WindowTopology.flags(self, eastOnly);
    assertFalse(flags[1][1]);
    assertTrue(flags[1][3]);
    assertTrue(flags[0][2]);
    assertTrue(flags[2][0]);
    var withoutGlass = WindowMesh.create(self, eastOnly);
    var withGlass = WindowMesh.create(entry(Shape.WindowFrame, 1, 0, true), eastOnly);
    assertTrue(withGlass.triangles().size() > withoutGlass.triangles().size());
    WindowTopology.Neighborhood withDiagonal =
        (x, y, z) -> x == 1 && (y == 0 || y == -1) && z == 0 ? glazed : WindowTopology.Entry.EMPTY;
    assertFalse(WindowTopology.flags(self, withDiagonal)[0][2]);
  }

  @Test
  void originalWindowSelectionAndCollisionAreDifferent() {
    var self = entry(Shape.WindowFrame, 1, 0, false);
    WindowTopology.Neighborhood above =
        (x, y, z) -> x == 0 && y == 1 && z == 0 ? self : WindowTopology.Entry.EMPTY;
    assertEquals(4, WindowMesh.collision(self, above, true).size());
    assertEquals(3, WindowMesh.collision(self, above, false).size());
    assertEquals(5, WindowMesh.collision(entry(Shape.WindowFrame, 1, 0, true), above, true).size());
  }
}

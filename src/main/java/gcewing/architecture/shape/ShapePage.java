package gcewing.architecture.shape;

import java.util.List;

/** Original eight sawbench pages, in their original order (including shared entries). */
public record ShapePage(String key, List<Shape> shapes) {
  public static final List<ShapePage> PAGES =
      List.of(
          new ShapePage(
              "roofing",
              List.of(
                  Shape.RoofTile,
                  Shape.RoofOuterCorner,
                  Shape.RoofInnerCorner,
                  Shape.RoofRidge,
                  Shape.RoofSmartRidge,
                  Shape.RoofValley,
                  Shape.RoofSmartValley,
                  Shape.RoofOverhang,
                  Shape.RoofOverhangOuterCorner,
                  Shape.RoofOverhangInnerCorner,
                  Shape.RoofOverhangGableLH,
                  Shape.RoofOverhangGableRH,
                  Shape.RoofOverhangGableEndLH,
                  Shape.RoofOverhangGableEndRH,
                  Shape.RoofOverhangRidge,
                  Shape.RoofOverhangValley,
                  Shape.BevelledOuterCorner,
                  Shape.BevelledInnerCorner)),
          new ShapePage(
              "rounded",
              List.of(
                  Shape.Cylinder,
                  Shape.CylinderHalf,
                  Shape.CylinderQuarter,
                  Shape.CylinderLargeQuarter,
                  Shape.AnticylinderLargeQuarter,
                  Shape.Pillar,
                  Shape.Post,
                  Shape.Pole,
                  Shape.SphereFull,
                  Shape.SphereHalf,
                  Shape.SphereQuarter,
                  Shape.SphereEighth,
                  Shape.SphereEighthLarge,
                  Shape.SphereEighthLargeRev)),
          new ShapePage(
              "classical",
              List.of(
                  Shape.PillarBase,
                  Shape.Pillar,
                  Shape.DoricCapital,
                  Shape.DoricTriglyph,
                  Shape.DoricTriglyphCorner,
                  Shape.DoricMetope,
                  Shape.IonicCapital,
                  Shape.CorinthianCapital,
                  Shape.Architrave,
                  Shape.ArchitraveCorner,
                  Shape.CorniceLH,
                  Shape.CorniceRH,
                  Shape.CorniceEndLH,
                  Shape.CorniceEndRH,
                  Shape.CorniceRidge,
                  Shape.CorniceValley,
                  Shape.CorniceBottom)),
          new ShapePage(
              "window", List.of(Shape.WindowFrame, Shape.WindowCorner, Shape.WindowMullion)),
          new ShapePage(
              "arches",
              List.of(
                  Shape.ArchD1,
                  Shape.ArchD2,
                  Shape.ArchD3A,
                  Shape.ArchD3B,
                  Shape.ArchD3C,
                  Shape.ArchD4A,
                  Shape.ArchD4B,
                  Shape.ArchD4C)),
          new ShapePage(
              "railings",
              List.of(
                  Shape.BalustradePlain,
                  Shape.BalustradePlainOuterCorner,
                  Shape.BalustradePlainInnerCorner,
                  Shape.BalustradePlainWithNewel,
                  Shape.BalustradePlainEnd,
                  Shape.BanisterPlainTop,
                  Shape.BanisterPlain,
                  Shape.BanisterPlainBottom,
                  Shape.BanisterPlainEnd,
                  Shape.BanisterPlainInnerCorner,
                  Shape.BalustradeFancy,
                  Shape.BalustradeFancyCorner,
                  Shape.BalustradeFancyWithNewel,
                  Shape.BalustradeFancyNewel,
                  Shape.BanisterFancyTop,
                  Shape.BanisterFancy,
                  Shape.BanisterFancyBottom,
                  Shape.BanisterFancyEnd,
                  Shape.BanisterFancyNewelTall)),
          new ShapePage(
              "other",
              List.of(
                  Shape.CladdingSheet,
                  Shape.Slab,
                  Shape.Stairs,
                  Shape.StairsOuterCorner,
                  Shape.StairsInnerCorner,
                  Shape.SlopeTileA1,
                  Shape.SlopeTileA2,
                  Shape.SlopeTileB1,
                  Shape.SlopeTileB2,
                  Shape.SlopeTileB3,
                  Shape.SlopeTileC1,
                  Shape.SlopeTileC2,
                  Shape.SlopeTileC3,
                  Shape.SlopeTileC4,
                  Shape.AngledRoofRidge,
                  Shape.DoubleRoofTile)),
          new ShapePage(
              "glow",
              List.of(
                  Shape.SquareSE,
                  Shape.SlabSE,
                  Shape.RoofTileSE,
                  Shape.SlopeTileA1SE,
                  Shape.SlopeTileA2SE,
                  Shape.SlopeTileB1SE,
                  Shape.SlopeTileB2SE,
                  Shape.SlopeTileB3SE,
                  Shape.SlopeTileC1SE,
                  Shape.SlopeTileC2SE,
                  Shape.SlopeTileC3SE,
                  Shape.SlopeTileC4SE)));

  public static boolean valid(int page, int slot) {
    return page >= 0 && page < PAGES.size() && slot >= 0 && slot < PAGES.get(page).shapes.size();
  }

  public static Shape shape(int page, int slot) {
    return valid(page, slot) ? PAGES.get(page).shapes.get(slot) : null;
  }

  public static int button(int page, int slot) {
    return page * 32 + slot;
  }
}

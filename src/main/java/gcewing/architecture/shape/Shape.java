package gcewing.architecture.shape;

import java.util.Arrays;
import java.util.Locale;

/** Stable shape IDs and sawbench yields retained from the original mod. */
public enum Shape {
  RoofTile(0, "Roof Tile", "roof", 1, 2, false, false),
  RoofOuterCorner(1, "Roof Outer Corner", "roof", 1, 3, false, false),
  RoofInnerCorner(2, "Roof Inner Corner", "roof", 2, 3, false, false),
  RoofRidge(3, "Gabled Roof Ridge", "roof", 1, 4, false, false),
  RoofSmartRidge(4, "Hip Roof Ridge", "roof", 1, 2, false, false),
  RoofValley(5, "Gabled Roof Valley", "roof", 1, 2, false, false),
  RoofSmartValley(6, "Hip Roof Valley", "roof", 1, 1, false, false),
  RoofOverhang(7, "Roof Overhang", "roof_overhang", 1, 2, false, false),
  RoofOverhangOuterCorner(
      8, "Roof Overhang Outer Corner", "roof_overhang_outer_corner", 1, 3, false, false),
  RoofOverhangInnerCorner(
      9, "Roof Overhang Inner Corner", "roof_overhang_inner_corner", 2, 3, false, false),
  Cylinder(10, "Cylinder", "cylinder_full_r8h16", 1, 1, false, false),
  CylinderHalf(11, "Half Cylinder", "cylinder_half_r8h16", 1, 1, false, false),
  CylinderQuarter(12, "Quarter Cylinder", "cylinder_quarter_r8h16", 1, 1, false, false),
  CylinderLargeQuarter(13, "Round Outer Corner", "cylinder_quarter_r16h16", 1, 1, false, false),
  AnticylinderLargeQuarter(14, "Round Inner Corner", "round_inner_corner", 1, 2, false, false),
  Pillar(15, "Round Pillar", "cylinder_r6h16", 1, 1, false, false),
  Post(16, "Round Post", "cylinder_r4h16", 1, 4, false, false),
  Pole(17, "Round Pole", "cylinder_r2h16", 1, 16, false, false),
  BevelledOuterCorner(18, "Bevelled Outer Corner", "bevelled_outer_corner", 1, 3, false, false),
  BevelledInnerCorner(19, "Bevelled Inner Corner", "bevelled_inner_corner", 1, 1, false, false),
  PillarBase(20, "Round Pillar Base", "pillar_base", 1, 1, false, false),
  DoricCapital(21, "Doric Capital", "doric_capital", 1, 1, false, false),
  IonicCapital(22, "Ionic capital", "ionic_capital", 1, 1, false, false),
  CorinthianCapital(23, "Corinthian capital", "corinthian_capital", 1, 1, false, false),
  DoricTriglyph(24, "Triglyph", "doric_triglyph", 1, 1, false, false),
  DoricTriglyphCorner(25, "Triglyph Corner", "doric_triglyph_corner", 1, 1, false, false),
  DoricMetope(26, "Metope", "doric_metope", 1, 1, false, false),
  Architrave(27, "Architrave", "architrave", 1, 1, false, false),
  ArchitraveCorner(28, "Architrave Corner", "architrave_corner", 1, 1, false, false),
  WindowFrame(30, "Window Frame", "window_frame", 1, 4, false, false),
  WindowCorner(31, "Window Corner", "window_corner", 1, 2, false, false),
  WindowMullion(32, "Window Mullion", "window_mullion", 1, 2, false, false),
  SphereFull(33, "Sphere", "sphere_full_r8", 1, 1, false, false),
  SphereHalf(34, "Hemisphere", "sphere_half_r8", 1, 2, false, false),
  SphereQuarter(35, "Quarter Sphere", "sphere_quarter_r8", 1, 4, false, false),
  SphereEighth(36, "Quarter Hemisphere", "sphere_eighth_r8", 1, 8, false, false),
  SphereEighthLarge(37, "Round Outer Corner Cap", "sphere_eighth_r16", 1, 1, false, false),
  SphereEighthLargeRev(38, "Round Inner Corner Cap", "sphere_eighth_r16_rev", 1, 1, false, false),
  RoofOverhangGableLH(40, "Gable Overhang LH", "roof_overhang_gable_lh", 1, 4, false, false),
  RoofOverhangGableRH(41, "Gable Overhang RH", "roof_overhang_gable_rh", 1, 4, false, false),
  RoofOverhangGableEndLH(
      42, "Gable Overhang LH End", "roof_overhang_gable_end_lh", 1, 4, false, false),
  RoofOverhangGableEndRH(
      43, "Gable Overhang RH End", "roof_overhang_gable_end_rh", 1, 4, false, false),
  RoofOverhangRidge(44, "Ridge Overhang", "roof_overhang_gable_ridge", 1, 4, false, false),
  RoofOverhangValley(45, "Valley Overhang", "roof_overhang_gable_valley", 1, 4, false, false),
  CorniceLH(50, "Cornice LH", "cornice_lh", 1, 4, false, false),
  CorniceRH(51, "Cornice RH", "cornice_rh", 1, 4, false, false),
  CorniceEndLH(52, "Cornice LH End", "cornice_end_lh", 1, 4, false, false),
  CorniceEndRH(53, "Cornice RH End", "cornice_end_rh", 1, 4, false, false),
  CorniceRidge(54, "Cornice Ridge", "cornice_ridge", 1, 4, false, false),
  CorniceValley(55, "Cornice Valley", "cornice_valley", 1, 4, false, false),
  CorniceBottom(56, "Cornice Bottom", "cornice_bottom", 1, 4, false, false),
  CladdingSheet(60, "Cladding", "cladding", 1, 1, false, false),
  ArchD1(61, "Arch Diameter 1", "arch_d1", 1, 1, true, false),
  ArchD2(62, "Arch Diameter 2", "arch_d2", 1, 2, true, false),
  ArchD3A(63, "Arch Diameter 3 Part A", "arch_d3a", 1, 2, true, false),
  ArchD3B(64, "Arch Diameter 3 Part B", "arch_d3b", 1, 1, true, false),
  ArchD3C(65, "Arch Diameter 3 Part C", "arch_d3c", 1, 1, true, false),
  ArchD4A(66, "Arch Diameter 4 Part A", "arch_d4a", 1, 2, true, false),
  ArchD4B(67, "Arch Diameter 4 Part B", "arch_d4b", 1, 1, true, false),
  ArchD4C(68, "Arch Diameter 4 Part C", "arch_d4c", 1, 2, true, false),
  BanisterPlainBottom(
      70, "Plain Banister Bottom Transition", "balustrade_stair_plain_bottom", 1, 10, false, true),
  BanisterPlain(71, "Plain Banister", "balustrade_stair_plain", 1, 10, false, true),
  BanisterPlainTop(
      72, "Plain Banister Top Transition", "balustrade_stair_plain_top", 1, 10, false, true),
  BalustradeFancy(73, "Fancy Balustrade", "balustrade_fancy", 1, 5, false, false),
  BalustradeFancyCorner(
      74, "Fancy Corner Balustrade", "balustrade_fancy_corner", 1, 2, false, false),
  BalustradeFancyWithNewel(
      75, "Fancy Balustrade with Newel", "balustrade_fancy_with_newel", 1, 3, false, false),
  BalustradeFancyNewel(76, "Fancy Newel", "balustrade_fancy_newel", 1, 4, false, false),
  BalustradePlain(77, "Plain Balustrade", "balustrade_plain", 1, 10, false, false),
  BalustradePlainOuterCorner(
      78, "Plain Outer Corner Balustrade", "balustrade_plain_outer_corner", 1, 4, false, false),
  BalustradePlainWithNewel(
      79, "Plain Balustrade with Newel", "balustrade_plain_with_newel", 1, 6, false, false),
  BanisterPlainEnd(80, "Plain Banister End", "balustrade_stair_plain_end", 1, 8, false, true),
  BanisterFancyNewelTall(81, "Tall Fancy Newel", "balustrade_fancy_newel_tall", 1, 2, false, false),
  BalustradePlainInnerCorner(
      82, "Plain Inner Corner Balustrade", "balustrade_plain_inner_corner", 1, 8, false, false),
  BalustradePlainEnd(83, "Plain Balustrade End", "balustrade_plain_end", 1, 8, false, true),
  BanisterFancyBottom(
      84, "Fancy Banister Bottom Transition", "balustrade_stair_fancy_bottom", 1, 5, false, true),
  BanisterFancy(85, "Fancy Banister", "balustrade_stair_fancy", 1, 5, false, true),
  BanisterFancyTop(
      86, "Fancy Banister Top Transition", "balustrade_stair_fancy_top", 1, 5, false, true),
  BanisterFancyEnd(87, "Fancy Banister End", "balustrade_stair_fancy_end", 1, 2, false, true),
  BanisterPlainInnerCorner(
      88, "Plain Banister Inner Corner", "balustrade_stair_plain_inner_corner", 1, 6, false, false),
  Slab(90, "Slab", "slab", 1, 2, false, false),
  Stairs(91, "Stairs", "stairs", 3, 4, false, false),
  StairsOuterCorner(92, "Stairs Outer Corner", "stairs_outer_corner", 2, 3, false, false),
  StairsInnerCorner(93, "Stairs Inner Corner", "stairs_inner_corner", 1, 1, false, false),
  SlopeTileA1(94, "Slope A Start", "roof", 1, 1, false, false),
  SlopeTileA2(95, "Slope A End", "roof", 1, 3, false, false),
  SlopeTileB1(96, "Slope B Start", "roof", 1, 1, false, false),
  SlopeTileB2(97, "Slope B Middle", "roof", 1, 2, false, false),
  SlopeTileB3(98, "Slope B End", "roof", 1, 3, false, false),
  SlopeTileC1(99, "Slope C 1", "roof", 1, 1, false, false),
  SlopeTileC2(100, "Slope C 2", "roof", 1, 2, false, false),
  SlopeTileC3(101, "Slope C 3", "roof", 1, 3, false, false),
  SlopeTileC4(102, "Slope C 4", "roof", 1, 4, false, false),
  SlopeTileA1SE(103, "Slope A Start(Glow)", "roof", 1, 1, false, false),
  SlopeTileA2SE(104, "Slope A End(Glow)", "roof", 1, 3, false, false),
  SlopeTileB1SE(105, "Slope B Start(Glow)", "roof", 1, 1, false, false),
  SlopeTileB2SE(106, "Slope B Middle(Glow)", "roof", 1, 2, false, false),
  SlopeTileB3SE(107, "Slope B End(Glow)", "roof", 1, 3, false, false),
  SlopeTileC1SE(108, "Slope C 1(Glow)", "roof", 1, 1, false, false),
  SlopeTileC2SE(109, "Slope C 2(Glow)", "roof", 1, 2, false, false),
  SlopeTileC3SE(110, "Slope C 3(Glow)", "roof", 1, 3, false, false),
  SlopeTileC4SE(111, "Slope C 4(Glow)", "roof", 1, 4, false, false),
  RoofTileSE(112, "Roof Tile(Glow)", "roof", 1, 2, false, false),
  SquareSE(113, "Square(Glow)", "square", 1, 1, false, false),
  SlabSE(114, "Slab(Glow)", "slab", 1, 2, false, false),
  AngledRoofRidge(115, "Angled Roof Ridge", "angled_roof_ridge", 1, 4, false, false),
  DoubleRoofTile(116, "Double Roof Tile", "double_roof_tile", 1, 2, false, false);

  public final int id;
  public final String title;
  public final String model;
  public final int materialUsed;
  public final int itemsProduced;
  public final boolean underneath;
  public final boolean offset;

  Shape(
      int id, String title, String model, int used, int made, boolean underneath, boolean offset) {
    this.id = id;
    this.title = title;
    this.model = model;
    this.materialUsed = used;
    this.itemsProduced = made;
    this.underneath = underneath;
    this.offset = offset;
  }

  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }

  public boolean window() {
    return id >= 30 && id <= 32;
  }

  public boolean roof() {
    return model.equals("roof");
  }

  public boolean glowing() {
    return id >= 103 && id <= 114;
  }

  public static Shape byId(int id) {
    return Arrays.stream(values()).filter(s -> s.id == id).findFirst().orElse(RoofTile);
  }
}

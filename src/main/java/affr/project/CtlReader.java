package affr.project;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Reads a solver control file ({@code fflow.ctl}) and extracts the high-level physics model
 * selection needed to pre-populate the New Calculation dialog.
 *
 * <p>The file uses Fortran 90 namelist format: sections start with {@code &name} and end with
 * {@code /}. This reader is intentionally minimal — it only parses the subset of variables required
 * to reconstruct an {@link AFFrCalculationModel}.
 *
 * <p>Parsing is case-insensitive for keys and namelist names; string values inside single quotes
 * are preserved as-is.
 */
public final class CtlReader {

  // key = value pairs: handles 'quoted', "double-quoted", and unquoted values
  private static final Pattern KV_PATTERN =
      Pattern.compile(
          "([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*" + "(?:'([^']*)'|\"([^\"]*)\"|([^,\\s/!]+))",
          Pattern.MULTILINE);

  private CtlReader() {}

  /**
   * Reads {@code ctlFile} and returns the {@link AFFrCalculationModel} it represents.
   *
   * <p>The file is read as UTF-8. If that fails with a {@link MalformedInputException} — for
   * example when the Fortran solver wrote the file using the OS locale encoding such as MS932 on
   * Japanese Windows — reading is retried with {@link Charset#defaultCharset()}. All keywords and
   * values currently parsed are ASCII, so either charset produces correct results as long as the
   * file is otherwise well-formed.
   *
   * @param ctlFile path to the {@code fflow.ctl} file
   * @return the parsed model
   * @throws IOException if the file cannot be read or is not a recognisable namelist file
   */
  public static AFFrCalculationModel read(Path ctlFile) throws IOException {
    String content;
    try {
      content = Files.readString(ctlFile, StandardCharsets.UTF_8);
    } catch (MalformedInputException e) {
      content = Files.readString(ctlFile, Charset.defaultCharset());
    }
    String lower = content.toLowerCase(Locale.ROOT);

    Map<String, String> modelVars = parseNamelist(content, lower, "model");
    Map<String, String> timeVars = parseNamelist(content, lower, "time");

    ComprsModel comprs = parseComprs(modelVars.getOrDefault("flow", "incompressible"));
    SteadyModel steady = parseSteady(timeVars.getOrDefault("flowcon", "1"));
    TurbModel turb = parseTurb(modelVars.getOrDefault("trbmdl", "ke"));
    Set<ExtraModel> extras = parseExtras(modelVars, lower);

    return new AFFrCalculationModel(
        comprs, steady, turb, extras.isEmpty() ? Set.of() : Set.copyOf(extras));
  }

  // ── Namelist section parser ────────────────────────────────────────────────

  /**
   * Extracts a map of {@code key → value} strings from the first occurrence of {@code &name …/} in
   * the file content.
   *
   * @param content original file content (for correct substring extraction)
   * @param lower lower-cased copy for position search
   * @param name namelist name (case-insensitive)
   * @return key-value map; empty if the namelist is absent
   */
  private static Map<String, String> parseNamelist(String content, String lower, String name) {
    Map<String, String> result = new HashMap<>();
    String marker = "&" + name.toLowerCase(Locale.ROOT);
    int start = lower.indexOf(marker);
    if (start < 0) return result;

    // Find the end delimiter: '/' that is not inside a string
    int end = content.length();
    boolean inSingle = false;
    boolean inDouble = false;
    for (int i = start + marker.length(); i < content.length(); i++) {
      char c = content.charAt(i);
      if (c == '\'' && !inDouble) inSingle = !inSingle;
      else if (c == '"' && !inSingle) inDouble = !inDouble;
      else if (!inSingle && !inDouble) {
        if (c == '/') {
          end = i;
          break;
        }
        // Another namelist start ends the current section
        if (c == '&' && i > start + 1) {
          end = i;
          break;
        }
      }
    }

    String section = content.substring(start + marker.length(), end);
    Matcher m = KV_PATTERN.matcher(section);
    while (m.find()) {
      @Nullable String rawKey = m.group(1);
      if (rawKey == null) continue;
      String key = rawKey.toLowerCase(Locale.ROOT);
      // Groups: 2=single-quoted, 3=double-quoted, 4=unquoted
      String value = m.group(2) != null ? m.group(2) : m.group(3) != null ? m.group(3) : m.group(4);
      if (value != null) {
        result.put(key, value.strip());
      }
    }
    return result;
  }

  // ── Field parsers ──────────────────────────────────────────────────────────

  private static ComprsModel parseComprs(String flow) {
    String f = flow.strip().toLowerCase(Locale.ROOT);
    // "compressible" starts with "comp"; "incompressible" and "zero-mach" do not
    return f.startsWith("comp") ? ComprsModel.COMPRESSIBLE : ComprsModel.INCOMPRESSIBLE;
  }

  private static SteadyModel parseSteady(String flowcon) {
    return "2".equals(flowcon.strip()) ? SteadyModel.UNSTEADY : SteadyModel.STEADY;
  }

  /**
   * Maps {@code trbmdl} string values to the high-level {@link TurbModel} family.
   *
   * <ul>
   *   <li>{@code sles}, {@code dles}, {@code wale} → {@link TurbModel#LES}
   *   <li>{@code dns} → {@link TurbModel#DNS}
   *   <li>{@code no} → {@link TurbModel#NO}
   *   <li>everything else → {@link TurbModel#RANS}
   * </ul>
   */
  private static TurbModel parseTurb(String trbmdl) {
    String t = trbmdl.strip().toLowerCase(Locale.ROOT);
    return switch (t) {
      case "sles", "dles", "wale" -> TurbModel.LES;
      case "dns" -> TurbModel.DNS;
      case "no" -> TurbModel.NO;
      default -> TurbModel.RANS;
    };
  }

  /**
   * Detects active extra models from {@code &model} variable flags and namelist-presence checks.
   *
   * <p>Detection rules:
   *
   * <ul>
   *   <li>{@link ExtraModel#VOF} — {@code &vof} namelist present
   *   <li>{@link ExtraModel#CAVITATION} — {@code &cavitation} namelist present
   *   <li>{@link ExtraModel#RADIATION} — {@code &radoption} namelist present
   *   <li>{@link ExtraModel#PARTICLE_TRACK} — {@code model.cal_particle} &gt; 0
   *   <li>{@link ExtraModel#MOVING_MESH} — {@code model.cal_mvmsh} = 1
   *   <li>{@link ExtraModel#OVERSET_GRID} — {@code model.cal_ovset} = 1
   *   <li>{@link ExtraModel#ROTATING_FRAME} — {@code model.multi_frame} = 1
   *   <li>{@link ExtraModel#COMBUST_CHEM_REACT} — {@code model.cal_reac} = 1
   *   <li>{@link ExtraModel#COMBUSTION} — {@code &flamelet} namelist present
   *   <li>{@link ExtraModel#SURFACE_REACTION} — {@code model.cal_surf} = 1
   * </ul>
   *
   * Ghost Fluid and Porous Model have no unambiguous namelist markers and are not auto-detected.
   */
  private static Set<ExtraModel> parseExtras(Map<String, String> model, String lower) {
    Set<ExtraModel> result = EnumSet.noneOf(ExtraModel.class);

    if (lower.contains("&vof")) result.add(ExtraModel.VOF);
    if (lower.contains("&cavitation")) result.add(ExtraModel.CAVITATION);
    if (lower.contains("&radoption")) result.add(ExtraModel.RADIATION);
    if (lower.contains("&flamelet\n")
        || lower.contains("&flamelet ")
        || lower.contains("&flamelet\r")) {
      result.add(ExtraModel.COMBUSTION);
    }

    if (parseInt(model.getOrDefault("cal_particle", "0")) > 0) {
      result.add(ExtraModel.PARTICLE_TRACK);
    }
    if ("1".equals(model.getOrDefault("cal_mvmsh", "0").strip()))
      result.add(ExtraModel.MOVING_MESH);
    if ("1".equals(model.getOrDefault("cal_ovset", "0").strip()))
      result.add(ExtraModel.OVERSET_GRID);
    if ("1".equals(model.getOrDefault("multi_frame", "0").strip()))
      result.add(ExtraModel.ROTATING_FRAME);
    if ("1".equals(model.getOrDefault("cal_reac", "0").strip()))
      result.add(ExtraModel.COMBUST_CHEM_REACT);
    if ("1".equals(model.getOrDefault("cal_surf", "0").strip()))
      result.add(ExtraModel.SURFACE_REACTION);

    return result;
  }

  private static int parseInt(String s) {
    try {
      return Integer.parseInt(s.strip());
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}

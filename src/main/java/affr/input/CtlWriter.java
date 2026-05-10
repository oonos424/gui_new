package affr.input;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Serializes an {@link AFFrInput} to Fortran namelist text ({@code fflow.ctl} format) and writes it
 * to disk.
 *
 * <p>Format for each namelist block:
 *
 * <pre>
 * &amp;NAMELISTNAME
 *   field_name = value
 * /
 * </pre>
 *
 * <p>Serialization rules:
 *
 * <ul>
 *   <li>Single-instance namelists are written first (in {@link AFFrInput#SINGLE_NAMELIST_NAMES}
 *       order), then multi-instance namelists (in {@link AFFrInput#MULTI_NAMELIST_NAMES} order).
 *   <li>Namelists with no values in any instance are omitted entirely.
 *   <li>The {@code GUI} namelist is always excluded — it holds GUI-only state with no solver
 *       counterpart.
 *   <li>{@link AFFrCharacter} values are single-quoted. {@link AFFrLogical} values use {@code
 *       .TRUE.} / {@code .FALSE.}. {@link AFFrReal} values use Java's {@link
 *       Double#toString(double)} notation. {@link AFFrInteger} values are plain decimal.
 * </ul>
 */
public final class CtlWriter {

  private CtlWriter() {}

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Serializes {@code input} to a Fortran namelist string.
   *
   * <p>The result is suitable for writing directly to {@code fflow.ctl}. It does not include a
   * trailing newline after the last block.
   *
   * @param input the model to serialize
   * @return the full file content as a string
   */
  public static String serialize(AFFrInput input) {
    StringBuilder sb = new StringBuilder();

    // Singles first
    for (String name : AFFrInput.SINGLE_NAMELIST_NAMES) {
      if (NamelistNames.GUI.equals(name)) continue;
      AFFrNamelistSingle single = input.getSingle(name);
      if (single == null) continue;
      appendSingle(sb, single);
    }

    // Multis second
    for (String name : AFFrInput.MULTI_NAMELIST_NAMES) {
      AFFrNamelistMulti multi = input.getMulti(name);
      if (multi == null) continue;
      appendMulti(sb, multi);
    }

    return sb.toString();
  }

  /**
   * Writes the serialized form of {@code input} to {@code target}, replacing the file completely.
   *
   * @param input the model to serialize
   * @param target path to the {@code fflow.ctl} file to write
   * @throws IOException if the file cannot be written
   */
  public static void write(AFFrInput input, Path target) throws IOException {
    Files.writeString(target, serialize(input), StandardCharsets.UTF_8);
  }

  // ── Block writers ─────────────────────────────────────────────────────────

  private static void appendSingle(StringBuilder sb, AFFrNamelistSingle single) {
    AFFrNamelistData data = single.getData();
    Set<String> names = data.getValueNames();
    if (names.isEmpty()) return;
    appendBlock(sb, single.getListName(), data);
  }

  private static void appendMulti(StringBuilder sb, AFFrNamelistMulti multi) {
    for (String instanceKey : multi.getInstanceKeyList()) {
      AFFrNamelistData data = multi.getData(instanceKey);
      if (data == null) continue;
      if (data.getValueNames().isEmpty()) continue;
      appendBlock(sb, multi.getListName(), data);
    }
  }

  /** Writes one {@code &NAME … /} block from the given data instance. */
  private static void appendBlock(StringBuilder sb, String listName, AFFrNamelistData data) {
    sb.append('&').append(listName).append('\n');
    for (String valueName : data.getValueNames()) {
      AFFrValue value = data.getValue(valueName);
      if (value == null) continue;
      sb.append("  ").append(valueName).append(" = ").append(formatValue(value)).append('\n');
    }
    sb.append("/\n");
  }

  // ── Value formatting ──────────────────────────────────────────────────────

  /**
   * Formats an {@link AFFrValue} as a Fortran namelist literal.
   *
   * @param value the value to format
   * @return the formatted string representation
   */
  static String formatValue(AFFrValue value) {
    return switch (value) {
      case AFFrInteger i -> Integer.toString(i.getIntegerValue());
      case AFFrReal r -> formatReal(r.getRealValue());
      case AFFrCharacter c -> "'" + c.getCharacterValue() + "'";
      case AFFrLogical l -> l.getLogicalValue() ? ".TRUE." : ".FALSE.";
    };
  }

  /**
   * Formats a double as a Fortran-compatible real literal.
   *
   * <p>Uses Java's default {@link Double#toString} which produces either plain decimal or
   * scientific notation. The result is always parseable back to the same double value.
   */
  private static String formatReal(double value) {
    String s = Double.toString(value);
    // Java uses 'E' notation (e.g. "1.5E-3"). Fortran accepts this format as-is.
    return s.toUpperCase(Locale.ROOT);
  }

  // ── Snapshot support ──────────────────────────────────────────────────────

  /**
   * Produces a deterministic snapshot string suitable for unsaved-change detection.
   *
   * <p>This is identical to {@link #serialize(AFFrInput)} but is named separately to make the
   * intent clear at call sites.
   *
   * @param input the model to snapshot
   * @return the serialized text
   */
  public static String snapshot(AFFrInput input) {
    return serialize(input);
  }

  /**
   * Returns {@code true} if the current model state differs from {@code previousSnapshot}.
   *
   * <p>Used by the Input Editor to decide whether to prompt the user before navigating away.
   *
   * @param input the current model
   * @param previousSnapshot a snapshot taken when the editor was opened (via {@link
   *     #snapshot(AFFrInput)})
   * @return {@code true} if the model has unsaved changes
   */
  public static boolean hasUnsavedChanges(AFFrInput input, String previousSnapshot) {
    return !serialize(input).equals(previousSnapshot);
  }

  // ── Map serialization helper for tests ────────────────────────────────────

  /**
   * Serializes all key-value pairs from a field-name → raw-value map into the body of a namelist
   * block. Package-private; used by tests.
   */
  static String formatValues(Map<String, AFFrValue> values) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, AFFrValue> entry : values.entrySet()) {
      sb.append("  ")
          .append(entry.getKey())
          .append(" = ")
          .append(formatValue(entry.getValue()))
          .append('\n');
    }
    return sb.toString();
  }
}

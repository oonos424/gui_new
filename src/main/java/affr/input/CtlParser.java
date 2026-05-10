package affr.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Parses Fortran namelist text (the content of {@code fflow.ctl}) into an {@link AFFrInput}.
 *
 * <p>The file format uses Fortran 90 namelist syntax: each section starts with {@code &NAME} and
 * ends with {@code /}. Multiple sections with the same name are allowed — this is how
 * multi-instance namelists (e.g. {@code &BOUNDARY}) appear in the file.
 *
 * <p>Parsing is case-insensitive for namelist names and field names. String values inside single or
 * double quotes are preserved as-is.
 *
 * <p>Type inference rules (applied to unquoted values):
 *
 * <ul>
 *   <li>Single- or double-quoted value → {@link AFFrCharacter} (quotes stripped)
 *   <li>{@code .TRUE.}, {@code .FALSE.}, {@code T}, {@code F} (case-insensitive) → {@link
 *       AFFrLogical}
 *   <li>Contains {@code .} or {@code E}/{@code e} in a numeric context → {@link AFFrReal}
 *   <li>All digits, optionally signed → {@link AFFrInteger}
 *   <li>Anything else → {@link AFFrCharacter}
 * </ul>
 *
 * <p>Namelist blocks whose names are not present in the model's namelist maps are silently ignored.
 */
public final class CtlParser {

  // Matches key = value pairs. Groups: 1=key, 2=single-quoted value, 3=double-quoted value,
  // 4=unquoted value. Handles comments starting with '!' by stopping at '!' in unquoted group.
  private static final Pattern KV_PATTERN =
      Pattern.compile(
          "([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*" + "(?:'([^']*)'|\"([^\"]*)\"|([^,\\s/!]+))",
          Pattern.MULTILINE);

  private static final Pattern LOGICAL_TRUE =
      Pattern.compile("^\\.true\\.|^t$", Pattern.CASE_INSENSITIVE);
  private static final Pattern LOGICAL_FALSE =
      Pattern.compile("^\\.false\\.|^f$", Pattern.CASE_INSENSITIVE);
  private static final Pattern REAL_PATTERN =
      Pattern.compile("[+-]?(?:\\d+\\.\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?|[+-]?\\d+[eE][+-]?\\d+");
  private static final Pattern INTEGER_PATTERN = Pattern.compile("[+-]?\\d+");

  private CtlParser() {}

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Parses all namelist blocks in {@code content} and populates the namelists in {@code input}.
   *
   * <p>For single-instance namelists, field values are set directly. For multi-instance namelists,
   * each block's instance key is determined from the block's key-variable field (e.g. {@code
   * boundary_name} for {@code &BOUNDARY}); a new instance is created if needed.
   *
   * <p>Namelist names not present in {@code input}'s maps are silently ignored.
   *
   * @param input the model to populate; existing values are not cleared by this method
   * @param content the raw text content of {@code fflow.ctl}
   */
  public static void populate(AFFrInput input, String content) {
    String lower = content.toLowerCase(Locale.ROOT);
    List<NamelistBlock> blocks = extractBlocks(content, lower);

    for (NamelistBlock block : blocks) {
      String upperName = block.name.toUpperCase(Locale.ROOT);

      AFFrNamelistSingle single = input.getSingle(upperName);
      if (single != null) {
        applyToSingle(single, block.fields);
        continue;
      }

      AFFrNamelistMulti multi = input.getMulti(upperName);
      if (multi != null) {
        applyToMulti(multi, block.fields);
      }
      // Unknown namelists are silently ignored.
    }
  }

  // ── Block extraction ──────────────────────────────────────────────────────

  /** Represents one parsed {@code &NAME … /} block from the file. */
  private record NamelistBlock(String name, Map<String, String> fields) {}

  /**
   * Scans {@code content} for all {@code &NAME … /} blocks and returns them in order.
   *
   * <p>The algorithm walks the string character by character, tracking whether we are inside a
   * string literal, so that {@code /} or {@code &} inside quotes are not mistaken for delimiters.
   */
  private static List<NamelistBlock> extractBlocks(String content, String lower) {
    List<NamelistBlock> result = new ArrayList<>();
    int i = 0;
    int len = content.length();

    while (i < len) {
      int ampersand = lower.indexOf('&', i);
      if (ampersand < 0) break;

      // Read the namelist name: word characters following '&'
      int nameStart = ampersand + 1;
      int nameEnd = nameStart;
      while (nameEnd < len && Character.isLetterOrDigit(content.charAt(nameEnd))
          || (nameEnd < len && content.charAt(nameEnd) == '_')) {
        nameEnd++;
      }
      if (nameEnd == nameStart) {
        i = ampersand + 1;
        continue;
      }
      String nameRaw = content.substring(nameStart, nameEnd);

      // Find the closing '/' not inside a string
      int blockStart = nameEnd;
      int blockEnd = len;
      boolean inSingle = false;
      boolean inDouble = false;
      for (int j = blockStart; j < len; j++) {
        char c = content.charAt(j);
        if (c == '\'' && !inDouble) {
          inSingle = !inSingle;
        } else if (c == '"' && !inSingle) {
          inDouble = !inDouble;
        } else if (!inSingle && !inDouble) {
          if (c == '/') {
            blockEnd = j;
            break;
          }
          // Another '&' inside the block ends this section (malformed but defensive)
          if (c == '&' && j > blockStart) {
            blockEnd = j;
            break;
          }
        }
      }

      String section = content.substring(blockStart, blockEnd);
      Map<String, String> fields = parseFields(section);
      result.add(new NamelistBlock(nameRaw, fields));
      i = blockEnd + 1;
    }
    return result;
  }

  /** Extracts all {@code key = value} pairs from the body of one namelist block. */
  private static Map<String, String> parseFields(String section) {
    Map<String, String> fields = new HashMap<>();
    Matcher m = KV_PATTERN.matcher(section);
    while (m.find()) {
      @Nullable String rawKey = m.group(1);
      if (rawKey == null) continue;
      String key = rawKey.toLowerCase(Locale.ROOT);
      // Group 2 = single-quoted, 3 = double-quoted, 4 = unquoted
      @Nullable String sq = m.group(2);
      @Nullable String dq = m.group(3);
      @Nullable String uq = m.group(4);
      String raw;
      if (sq != null) {
        raw = "'" + sq + "'"; // preserve quote markers for type inference
      } else if (dq != null) {
        raw = "\"" + dq + "\"";
      } else if (uq != null) {
        raw = uq;
      } else {
        continue;
      }
      fields.put(key, raw.strip());
    }
    return fields;
  }

  // ── Application to namelists ──────────────────────────────────────────────

  private static void applyToSingle(AFFrNamelistSingle single, Map<String, String> fields) {
    for (Map.Entry<String, String> entry : fields.entrySet()) {
      AFFrValue value = inferType(entry.getKey(), entry.getValue());
      single.setValue(entry.getKey(), value);
    }
  }

  private static void applyToMulti(AFFrNamelistMulti multi, Map<String, String> fields) {
    // Determine the instance key from the designated key-variable field.
    String keyVar = multi.getKeyVariable();
    String instanceKey;
    if (!keyVar.isEmpty() && fields.containsKey(keyVar)) {
      instanceKey = stripQuotes(fields.get(keyVar));
    } else {
      // No key variable present: use the next auto-generated key.
      instanceKey = multi.nextInstanceKey();
    }

    multi.addInstance(instanceKey);
    for (Map.Entry<String, String> entry : fields.entrySet()) {
      AFFrValue value = inferType(entry.getKey(), entry.getValue());
      multi.setValue(instanceKey, entry.getKey(), value);
    }
  }

  // ── Type inference ────────────────────────────────────────────────────────

  /**
   * Infers the {@link AFFrValue} type from the raw string representation as it appears in the file.
   *
   * @param name the field name (lower-case)
   * @param raw the raw value string (may include surrounding quotes)
   * @return a typed {@link AFFrValue}
   */
  static AFFrValue inferType(String name, String raw) {
    // Quoted string → CHARACTER
    if ((raw.startsWith("'") && raw.endsWith("'"))
        || (raw.startsWith("\"") && raw.endsWith("\""))) {
      return new AFFrCharacter(name, raw.substring(1, raw.length() - 1));
    }

    // Logical
    if (LOGICAL_TRUE.matcher(raw).matches()) {
      return new AFFrLogical(name, true);
    }
    if (LOGICAL_FALSE.matcher(raw).matches()) {
      return new AFFrLogical(name, false);
    }

    // Real (must be checked before integer since reals contain digits too)
    if (REAL_PATTERN.matcher(raw).matches()) {
      try {
        return new AFFrReal(name, Double.parseDouble(raw.replace('d', 'e').replace('D', 'E')));
      } catch (NumberFormatException ignored) {
        // Fall through to character
      }
    }

    // Integer
    if (INTEGER_PATTERN.matcher(raw).matches()) {
      try {
        return new AFFrInteger(name, Integer.parseInt(raw));
      } catch (NumberFormatException ignored) {
        // Large integer: fall through to character
      }
    }

    // Default: character (unquoted)
    return new AFFrCharacter(name, raw);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static String stripQuotes(String raw) {
    String s = raw.strip();
    if (s.length() >= 2
        && ((s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'')
            || (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"'))) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }
}

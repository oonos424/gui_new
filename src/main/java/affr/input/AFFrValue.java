package affr.input;

/**
 * A typed name-value pair that represents one field in a solver namelist block.
 *
 * <p>The type is determined at parse time from the string representation in {@code fflow.ctl}. All
 * four type accessors are always present on every implementation; the caller is expected to use the
 * correct one based on knowledge of the field. Calling the wrong accessor throws {@link
 * UnsupportedOperationException}.
 *
 * <p>Implementations are immutable records. To update a field value, replace the whole {@code
 * AFFrValue} instance via {@link AFFrNamelist#setValue}.
 */
public sealed interface AFFrValue permits AFFrInteger, AFFrReal, AFFrCharacter, AFFrLogical {

  /** The four Fortran namelist-compatible value types. */
  enum ValueType {
    INTEGER,
    REAL,
    CHARACTER,
    LOGICAL
  }

  /** The field name as it appears in the namelist (lower-case, e.g. {@code "boundary_type"}). */
  String getName();

  /** The runtime type of this value. */
  ValueType getType();

  /**
   * Returns the integer representation of this value.
   *
   * @throws UnsupportedOperationException if this value is not of type {@link ValueType#INTEGER}
   */
  int getIntegerValue();

  /**
   * Returns the real (double) representation of this value.
   *
   * @throws UnsupportedOperationException if this value is not of type {@link ValueType#REAL}
   */
  double getRealValue();

  /**
   * Returns the character (String) representation of this value.
   *
   * @throws UnsupportedOperationException if this value is not of type {@link ValueType#CHARACTER}
   */
  String getCharacterValue();

  /**
   * Returns the logical (boolean) representation of this value.
   *
   * @throws UnsupportedOperationException if this value is not of type {@link ValueType#LOGICAL}
   */
  boolean getLogicalValue();
}

package affr.input;

/**
 * An {@link AFFrValue} holding an integer field value (Fortran {@code INTEGER}).
 *
 * <p>Only {@link #getIntegerValue()} returns a meaningful result; the other type accessors throw
 * {@link UnsupportedOperationException}.
 */
public record AFFrInteger(String getName, int value) implements AFFrValue {

  @Override
  public String getName() {
    return getName;
  }

  @Override
  public ValueType getType() {
    return ValueType.INTEGER;
  }

  @Override
  public int getIntegerValue() {
    return value;
  }

  @Override
  public double getRealValue() {
    throw new UnsupportedOperationException("AFFrInteger does not hold a REAL value");
  }

  @Override
  public String getCharacterValue() {
    throw new UnsupportedOperationException("AFFrInteger does not hold a CHARACTER value");
  }

  @Override
  public boolean getLogicalValue() {
    throw new UnsupportedOperationException("AFFrInteger does not hold a LOGICAL value");
  }
}

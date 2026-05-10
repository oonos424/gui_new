package affr.input;

/**
 * An {@link AFFrValue} holding a character (string) field value (Fortran {@code CHARACTER}).
 *
 * <p>Only {@link #getCharacterValue()} returns a meaningful result; the other type accessors throw
 * {@link UnsupportedOperationException}.
 */
public record AFFrCharacter(String getName, String value) implements AFFrValue {

  @Override
  public String getName() {
    return getName;
  }

  @Override
  public ValueType getType() {
    return ValueType.CHARACTER;
  }

  @Override
  public int getIntegerValue() {
    throw new UnsupportedOperationException("AFFrCharacter does not hold an INTEGER value");
  }

  @Override
  public double getRealValue() {
    throw new UnsupportedOperationException("AFFrCharacter does not hold a REAL value");
  }

  @Override
  public String getCharacterValue() {
    return value;
  }

  @Override
  public boolean getLogicalValue() {
    throw new UnsupportedOperationException("AFFrCharacter does not hold a LOGICAL value");
  }
}

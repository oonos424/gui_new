package affr.input;

/**
 * An {@link AFFrValue} holding a real-number field value (Fortran {@code REAL} / {@code DOUBLE
 * PRECISION}).
 *
 * <p>Only {@link #getRealValue()} returns a meaningful result; the other type accessors throw
 * {@link UnsupportedOperationException}.
 */
public record AFFrReal(String getName, double value) implements AFFrValue {

  @Override
  public String getName() {
    return getName;
  }

  @Override
  public ValueType getType() {
    return ValueType.REAL;
  }

  @Override
  public int getIntegerValue() {
    throw new UnsupportedOperationException("AFFrReal does not hold an INTEGER value");
  }

  @Override
  public double getRealValue() {
    return value;
  }

  @Override
  public String getCharacterValue() {
    throw new UnsupportedOperationException("AFFrReal does not hold a CHARACTER value");
  }

  @Override
  public boolean getLogicalValue() {
    throw new UnsupportedOperationException("AFFrReal does not hold a LOGICAL value");
  }
}

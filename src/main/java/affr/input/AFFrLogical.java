package affr.input;

/**
 * An {@link AFFrValue} holding a logical (boolean) field value (Fortran {@code LOGICAL}).
 *
 * <p>Only {@link #getLogicalValue()} returns a meaningful result; the other type accessors throw
 * {@link UnsupportedOperationException}.
 */
public record AFFrLogical(String getName, boolean value) implements AFFrValue {

  @Override
  public String getName() {
    return getName;
  }

  @Override
  public ValueType getType() {
    return ValueType.LOGICAL;
  }

  @Override
  public int getIntegerValue() {
    throw new UnsupportedOperationException("AFFrLogical does not hold an INTEGER value");
  }

  @Override
  public double getRealValue() {
    throw new UnsupportedOperationException("AFFrLogical does not hold a REAL value");
  }

  @Override
  public String getCharacterValue() {
    throw new UnsupportedOperationException("AFFrLogical does not hold a CHARACTER value");
  }

  @Override
  public boolean getLogicalValue() {
    return value;
  }
}

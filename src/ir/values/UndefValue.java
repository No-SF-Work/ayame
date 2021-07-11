package ir.values;

import ir.types.Type;
import ir.types.Type.NoType;

public class UndefValue extends Value {

  public UndefValue() {
    super(NoType.getType());
  }
}

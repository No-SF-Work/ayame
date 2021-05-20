package ir.values;

import ir.types.Type;
import ir.values.User;

/**
 * immutable at runtime
 */
public class Constant extends User {

  public Constant(String name, Type type, int numOP) {
    super(name, type, numOP);
  }
}

package ir.values;

import ir.types.Type;
import ir.values.User;
import java.util.ArrayList;

/**
 * 常量
 */
public abstract class Constant extends User {

  public Constant(Type type) {
    super("", type, 0);
  }

  public Constant(Type type, int numOP) {
    super("", type, numOP);
  }

  private ArrayList<Constant> constants;

}


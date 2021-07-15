package ir.types;

import java.security.PublicKey;

/**
 * 拿来和GEP,load,store配套使用
 * <p>
 *
 */
public class PointerType extends Type {

  public PointerType(Type contained) {
    this.contained = contained;
  }

  public Type getContained() {
    return contained;
  }

  private Type contained;

  @Override
  public String toString() {
    return null;
    //todo
  }
}

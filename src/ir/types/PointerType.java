package ir.types;

import java.security.PublicKey;

/**
 * 拿来和GEP,load,store配套使用
 * <p>
 * https://www.youtube.com/watch?v=m8G_S5LwlTo
 */
public class PointerType extends Type {

  public PointerType(Type contained) {
    this.contained = contained;
  }

  public Type getContained() {
    return contained;
  }

  private Type contained;
}

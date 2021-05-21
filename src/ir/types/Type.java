package ir.types;

import ir.values.Function;
import java.awt.Label;

/**
 * 每个 Value 都有一个 Type
 * <p>
 * 1. type 的对象是 immutable 的
 * <p>
 * 2. 每种类型只有一个对象被声明 （在类型论中int[2][3] 和 int[1][2]是不同的类型,但1和2是）
 * <p>
 * 3.
 * <p>
 */
public abstract class Type {

  /**
   * 用于需要一个Type但其不是Type的场合，我还不确定需不需要这个类
   */
  public static class NoType extends Type {

    //todo : should this exist?
    public NoType getType() {
      return type;
    }

    private NoType type = new NoType();

    private NoType() {
    }
  }

  /**
   * 这个类和Label类除了做为标记外不存在任何有用的信息，所以只用一个实例就行了
   */
  public static class VoidType extends Type {

    public VoidType getType() {
      return type;
    }

    private VoidType type = new VoidType();

    private VoidType() {
    }
  }

  public static class LabelType extends Type {

    public LabelType getType() {
      return type;
    }

    private LabelType type = new LabelType();

    //todo
    private LabelType() {
    }
  }

  //todo
  public Type() {
  }

  /**
   * 测试Type是否是合法的参数类型
   */
  public boolean isValidArgTy(Type tt) {
    return tt.isPointerTy() || tt.isIntegerTy();
  }

  /**
   * 测Type是否是合法的返回类型
   */
  public boolean isValidRetTy(Type tt) {
    return tt.isIntegerTy() || tt.isVoidTy();
  }

  public boolean isNoTy() {
    return this instanceof NoType;
  }

  public boolean isVoidTy() {
    return this instanceof VoidType;
  }

  public boolean isLabelTy() {
    return this instanceof LabelType;
  }

  public boolean isIntegerTy() {
    return this instanceof IntegerType;
  }

  public boolean isFunctionTy() {
    return this instanceof FunctionType;
  }

  public boolean isArrayTy() {
    return this instanceof ArrayType;
  }

  public boolean isPointerTy() {
    return this instanceof PointerType;
  }

  public boolean isI32() {
    if (this.isIntegerTy()) {
      return ((IntegerType) this).getNumBits() == 32;
    }
    return false;
  }

  public boolean isI1() {
    if (this.isIntegerTy()) {
      return ((IntegerType) this).getNumBits() == 1;
    }
    return false;
  }


}

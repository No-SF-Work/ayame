package ir.types;

public class VectorType extends Type {

  private int numEle;
  private Type eleType;

  public static VectorType getLen4VecTy() {
    return new VectorType(4, IntegerType.getI32());
  }

  public static VectorType getLen2VecTy() {
    return new VectorType(2, IntegerType.getI32());
  }

  public VectorType(int numEle, Type eleType) {
    this.eleType = eleType;
    this.numEle = numEle;
  }

  public int getNumEle() {
    return numEle;
  }

  public Type getEleType() {
    return eleType;
  }
}

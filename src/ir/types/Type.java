package ir.types;

/**
 * 用来记录某个Value的Type，每一个Value都有一个Type
 */
public abstract class Type {

  enum TypeID {
    VoidTyID,
    LabelTyID,
    IntegerTyID,
    FunctionTyID,
    ArrayTyID,
    PointerTyID,
  }

  //todo
  public Type(TypeID tid) {
    this.tid = tid;
  }

  public TypeID getTid() {
    return tid;
  }

  public boolean isVoidTy() {
    return getTid() == TypeID.VoidTyID;
  }

  public boolean isLabelTy() {
    return getTid() == TypeID.LabelTyID;
  }

  public boolean isIntegerTy() {
    return getTid() == TypeID.IntegerTyID;
  }

  public boolean isFunctionTy() {
    return getTid() == TypeID.FunctionTyID;
  }

  public boolean isArrayTy() {
    return getTid() == TypeID.ArrayTyID;
  }

  public boolean isPointerTy() {
    return getTid() == TypeID.PointerTyID;
  }

  private TypeID tid;
}

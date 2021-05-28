package ir.types;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数类型
 */
public class FunctionType extends Type {


  public Type getRetType() {
    return retTy;
  }

  public ArrayList<Type> getParams() {
    return params;
  }

  public Type GetParamTyAt(int i) {
    return params.get(i);
  }

  public int getNumParam() {
    return this.params.size();
  }

  public FunctionType(Type retTy, ArrayList<Type> params) {
    this.retTy = retTy;
    this.params = params;
  }

  Type retTy;
  /**
   * 函数声明中参数的Type
   */
  ArrayList<Type> params;
}

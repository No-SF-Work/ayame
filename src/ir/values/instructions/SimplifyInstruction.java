package ir.values.instructions;

import ir.MyFactoryBuilder;
import ir.values.Constants.ConstantInt;
import ir.values.GlobalVariable;
import ir.values.UndefValue;
import ir.values.Value;
import ir.values.instructions.Instruction.TAG_;

public class SimplifyInstruction {

  public static MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  public static Value simplifyInstruction(Instruction instruction) {
    Value result = instruction;
    switch (instruction.tag) {
      case Add:
        result = simplifyAddInst(instruction);
        break;
      case Sub:
        result = simplifySubInst(instruction);
        break;
      case Mul:
        result = simplifyMulInst(instruction);
        break;
      case Div:
        result = simplifyDivInst(instruction);
        break;
      case And:
        result = simplifyAndInst(instruction);
        break;
      case Or:
        result = simplifyOrInst(instruction);
        break;
      case GEP:
        result = simplifyGEPInst(instruction);
        break;
      case Phi:
        result = simplifyPhiInst(instruction);
        break;
      case Alloca:
        result = simplifyAllcoaInst(instruction);
        break;
      case Load:
        result = simplifyLoadInst(instruction);
        break;
      case Call:
        result = simplifyCallInst(instruction);
        break;
    }
    return result;
  }

  public static Boolean isCommutativeOp(TAG_ opTag) {
    switch (opTag) {
      case Add:
      case Mul:
      case And:
      case Or:
        return true;
      default:
        return false;
    }
  }

  public static Value foldConstant(TAG_ opTag, Value lhs, Value rhs) {
    if (lhs instanceof ConstantInt) {
      ConstantInt clhs = (ConstantInt) lhs;
      if (rhs instanceof ConstantInt) {
        ConstantInt crhs = (ConstantInt) rhs;
        if (opTag.ordinal() >= TAG_.Add.ordinal() && opTag.ordinal() <= TAG_.Div.ordinal()) {
          return ConstantInt.newOne(factory.getI32Ty(), BinaryInst.evalBinary(opTag, clhs, crhs));
        } else if (opTag.ordinal() >= TAG_.Lt.ordinal() && opTag.ordinal() <= TAG_.Or.ordinal()) {
          return ConstantInt.newOne(factory.getI1Ty(), BinaryInst.evalBinary(opTag, clhs, crhs));
        }
      }
    }
    return null;
  }

  public static Value simplifyAddInst(Instruction inst) {
    Value lhs = inst.getOperands().get(0);
    Value rhs = inst.getOperands().get(1);
    if (lhs instanceof GlobalVariable) {
      lhs = ((GlobalVariable) lhs).init;
    }
    if (rhs instanceof GlobalVariable) {
      rhs = ((GlobalVariable) rhs).init;
    }

    // try fold and swap
    Value c = foldConstant(inst.tag, lhs, rhs);
    if (c != null) {
      return c;
    }
    if (lhs instanceof ConstantInt || lhs.getType().isNoTy()) {
      inst.getOperands().set(0, rhs);
      inst.getOperands().set(1, lhs);
    }

    // lhs + Undef -> Undef
    if (rhs.getType().isNoTy()) {
      return new UndefValue();
    }

    // lhs + 0 -> lhs
    if (rhs instanceof ConstantInt && ((ConstantInt) rhs).getVal() == 0) {
      return lhs;
    }

    // lhs + rhs == 0
    // 1. lhs = sub(0, rhs) or rhs = sub(0, lhs)
    // 2. lhs = sub(a, b) and rhs = sub(b, a)
    if (lhs.isInstruction() && rhs.isInstruction()) {
      Instruction ilhs = (Instruction) lhs;
      Instruction irhs = (Instruction) rhs;
      if (ilhs.tag == TAG_.Sub && irhs.tag == TAG_.Sub) {
        Value lhsOfIlhs = ilhs.getOperands().get(0);
        Value rhsOfIlhs = ilhs.getOperands().get(1);
        Value lhsOfIrhs = irhs.getOperands().get(0);
        Value rhsOfIrhs = irhs.getOperands().get(1);
        if ((lhsOfIlhs instanceof ConstantInt) && ((ConstantInt) lhsOfIlhs).getVal() == 0) {
          if (rhsOfIlhs == rhs) {
            return ConstantInt.newOne(factory.getI32Ty(), 0);
          }
        } else if ((lhsOfIrhs instanceof ConstantInt) && ((ConstantInt) lhsOfIrhs).getVal() == 0) {
          if (rhsOfIrhs == lhs) {
            return ConstantInt.newOne(factory.getI32Ty(), 0);
          }
        }
      }
    }

    // TODO X + (Y - X) -> Y or (Y - X) + X -> Y

    // TODO SimplifyAssociativeBinOp

    return inst;
  }

  public static Value simplifySubInst(Instruction inst) {
    Value lhs = inst.getOperands().get(0);
    Value rhs = inst.getOperands().get(1);
    if (lhs instanceof GlobalVariable) {
      lhs = ((GlobalVariable) lhs).init;
    }
    if (rhs instanceof GlobalVariable) {
      rhs = ((GlobalVariable) rhs).init;
    }

    Value c = foldConstant(inst.tag, lhs, rhs);
    if (c != null) {
      return c;
    }

    // lhs - Undef -> Undef, Undef - rhs -> Undef
    if (lhs.getType().isNoTy() || rhs.getType().isNoTy()) {
      return new UndefValue();
    }

    // lhs - 0 -> lhs
    if (rhs instanceof ConstantInt && ((ConstantInt) rhs).getVal() == 0) {
      return lhs;
    }

    // lhs == rhs
    if (lhs.equals(rhs)) {
      return ConstantInt.newOne(factory.getI32Ty(), 0);
    }

    // 要做吗？ 0 - rhs -> rhs if rhs is 0 or the minimum signed value.

    // TODO (X + Y) - Z -> X + (Y - Z) or Y + (X - Z) if everything simplifies.
    // TODO X - (Y + Z) -> (X - Y) - Z or (X - Z) - Y if everything simplifies.
    // TODO Z - (X - Y) -> (Z - X) + Y if everything simplifies.

    return inst;
  }

  public static Value simplifyMulInst(Instruction inst) {
    Value lhs = inst.getOperands().get(0);
    Value rhs = inst.getOperands().get(1);

    Value c = foldConstant(inst.tag, lhs, rhs);
    if (c != null) {
      return c;
    }
    if (lhs instanceof ConstantInt || lhs.getType().isNoTy()) {
      inst.getOperands().set(0, rhs);
      inst.getOperands().set(1, lhs);
    }

    // lhs * Undef -> Undef
    if (lhs.getType().isNoTy() || rhs.getType().isNoTy()) {
      return new UndefValue();
    }

    // lhs * 0 -> 0
    // lhs * 1 -> lhs
    if (rhs instanceof ConstantInt) {
      int rhsVal = ((ConstantInt) rhs).getVal();
      switch (rhsVal) {
        case 0:
          return ConstantInt.newOne(factory.getI32Ty(), 0);
        case 1:
          return lhs;
      }
    }

    // TODO SimplifyAssociativeBinOp
    // TODO expandCommutativeBinOp

    return inst;
  }

  public static Value simplifyDivInst(Instruction inst) {
    return inst;
  }

  public static Value simplifyAndInst(Instruction inst) {
    return inst;
  }

  public static Value simplifyOrInst(Instruction inst) {
    return inst;
  }

  public static Value simplifyGEPInst(Instruction inst) {
    return inst;
  }

  public static Value simplifyPhiInst(Instruction inst) {
    return inst;
  }

  public static Value simplifyAllcoaInst(Instruction inst) {
    return inst;
  }

  public static Value simplifyLoadInst(Instruction inst) {
    return inst;
  }

  public static Value simplifyCallInst(Instruction inst) {
    return inst;
  }
}

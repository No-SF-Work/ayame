package ir.values.instructions;

import ir.MyFactoryBuilder;
import ir.values.Constants.ConstantInt;
import ir.values.GlobalVariable;
import ir.values.UndefValue;
import ir.values.Value;
import ir.values.instructions.Instruction.TAG_;

// 参考：LLVM InstructionSimplify
public class SimplifyInstruction {

  public static MyFactoryBuilder factory = MyFactoryBuilder.getInstance();

  public static Value simplifyInstruction(Instruction instruction) {
    return switch (instruction.tag) {
      case Add -> simplifyAddInst(instruction, true);
      case Sub -> simplifySubInst(instruction, true);
      case Mul -> simplifyMulInst(instruction, true);
      case Div -> simplifyDivInst(instruction, true);
      case Lt -> simplifyLtInst(instruction, true);
      case Le -> simplifyLeInst(instruction, true);
      case Ge -> simplifyGeInst(instruction, true);
      case Gt -> simplifyGtInst(instruction, true);
      case Eq -> simplifyEqInst(instruction, true);
      case Ne -> simplifyNeInst(instruction, true);
      case And -> simplifyAndInst(instruction, true);
      case Or -> simplifyOrInst(instruction, true);
      case GEP -> simplifyGEPInst(instruction, true);
      case Phi -> simplifyPhiInst(instruction, true);
      case Alloca -> simplifyAllocaInst(instruction, true);
      case Load -> simplifyLoadInst(instruction, true);
      case Call -> simplifyCallInst(instruction, true);
      default -> instruction;
    };
  }

  public static Boolean isCommutativeOp(TAG_ opTag) {
    return switch (opTag) {
      case Add, Mul, And, Or -> true;
      default -> false;
    };
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

  public static Value simplifyAddInst(Instruction inst, boolean canRecur) {
    Value lhs = inst.getOperands().get(0);
    Value rhs = inst.getOperands().get(1);
    if (lhs instanceof GlobalVariable) {
      lhs = ((GlobalVariable) lhs).init;
    }
    if (rhs instanceof GlobalVariable) {
      rhs = ((GlobalVariable) rhs).init;
    }

    var targetBB = inst.getBB();
    var targetEndInst = targetBB.getList().getLast().getVal();

    // try fold and swap
    Value c = foldConstant(inst.tag, lhs, rhs);
    if (c != null) {
      return c;
    }
    if (lhs instanceof ConstantInt || lhs.getType().isNoTy()) {
      inst.CORemoveAllOperand();
      inst.COaddOperand(rhs);
      inst.COaddOperand(lhs);
      lhs = inst.getOperands().get(0);
      rhs = inst.getOperands().get(1);
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
    if (lhs instanceof Instruction && rhs instanceof Instruction) {
      Instruction ilhs = (Instruction) lhs;
      Instruction irhs = (Instruction) rhs;
      if (ilhs.tag == TAG_.Sub || irhs.tag == TAG_.Sub) {
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

    if (!canRecur) {
      return inst;
    }

    if (rhs instanceof BinaryInst && ((Instruction) rhs).tag == TAG_.Sub) {
      BinaryInst subInst = (BinaryInst) rhs;
      if (subInst.getOperands().get(1) == lhs) {
        // X + (Y - X) -> Y
        return subInst.getOperands().get(0);
      } else {
        // X + (Y - Z) -> (X + Y) - Z or (X - Z) + Y
        var subLhs = subInst.getOperands().get(0);
        var subRhs = subInst.getOperands().get(1);
        BinaryInst tmpInst = new BinaryInst(targetEndInst, TAG_.Add, factory.getI32Ty(), lhs,
            subLhs);
        Value simpleAdd = simplifyAddInst(tmpInst, false);
        if (simpleAdd != tmpInst) {
          return simplifySubInst(
              new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), simpleAdd, subRhs),
              false);
//          return new BinaryInst(TAG_.Sub, factory.getI32Ty(), simpleAdd, subRhs, targetBB);
        }

        tmpInst = new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), lhs, subRhs);
        Value simpleSub = simplifySubInst(tmpInst, false);
        if (simpleSub != tmpInst) {
          return simplifyAddInst(
              new BinaryInst(targetEndInst, TAG_.Add, factory.getI32Ty(), simpleSub, subLhs),
              false);
//          return new BinaryInst(TAG_.Add, factory.getI32Ty(), simpleSub, subLhs, targetBB);
        }
      }
    }

    if (lhs instanceof BinaryInst && ((Instruction) lhs).tag == TAG_.Sub) {
      BinaryInst subInst = (BinaryInst) lhs;
      if (subInst.getOperands().get(1) == rhs) {
        // (Y - X) + X -> Y
        return subInst.getOperands().get(0);
      } else {
        // (X - Y) + Z -> (X + Z) - Y or (Z - Y) + X
        var subLhs = subInst.getOperands().get(0);
        var subRhs = subInst.getOperands().get(1);
        BinaryInst tmpInst = new BinaryInst(targetEndInst, TAG_.Add, factory.getI32Ty(), subLhs,
            rhs);
        Value simpleAdd = simplifyAddInst(tmpInst, false);
        if (simpleAdd != tmpInst) {
          return simplifySubInst(
              new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), simpleAdd, subRhs),
              false);
//          return new BinaryInst(TAG_.Sub, factory.getI32Ty(), simpleAdd, subRhs, targetBB);
        }

        tmpInst = new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), rhs, subRhs);
        Value simpleSub = simplifySubInst(tmpInst, false);
        if (simpleSub != tmpInst) {
          return simplifyAddInst(
              new BinaryInst(targetEndInst, TAG_.Add, factory.getI32Ty(), simpleSub, subLhs),
              false);
//          return new BinaryInst(TAG_.Add, factory.getI32Ty(), simpleSub, subLhs, targetBB);
        }
      }
    }

    // TODO 加法结合律优化，共4种

    return inst;
  }

  public static Value simplifySubInst(Instruction inst, boolean canRecur) {
    Value lhs = inst.getOperands().get(0);
    Value rhs = inst.getOperands().get(1);
    if (lhs instanceof GlobalVariable) {
      lhs = ((GlobalVariable) lhs).init;
    }
    if (rhs instanceof GlobalVariable) {
      rhs = ((GlobalVariable) rhs).init;
    }

    var targetBB = inst.getBB();
    var targetEndInst = targetBB.getList().getLast().getVal();

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

    // lhs - C -> lhs + (-C)
    if (rhs instanceof ConstantInt) {
      int rhsVal = ((ConstantInt) rhs).getVal();
      ConstantInt negRhs = ConstantInt.newOne(factory.getI32Ty(), -rhsVal);
      return simplifyAddInst(
          new BinaryInst(targetEndInst, TAG_.Add, factory.getI32Ty(), lhs, negRhs), true);
    }

    // 要做吗？ 0 - rhs -> rhs if rhs is 0 or the minimum signed value.

    if (!canRecur) {
      return inst;
    }

    if (lhs instanceof BinaryInst) {
      switch (((Instruction) lhs).tag) {
        // (X + Y) - Z -> X + (Y - Z) or Y + (X - Z)
        case Add -> {
          BinaryInst addInst = (BinaryInst) lhs;
          for (var i = 0; i < 2; i++) {
            var x = addInst.getOperands().get(i);
            BinaryInst tmpInst = new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), x,
                rhs);
            Value simpleSub = simplifySubInst(tmpInst, false);
            if (simpleSub != tmpInst) {
              return simplifyAddInst(
                  new BinaryInst(targetEndInst, TAG_.Add, factory.getI32Ty(),
                      addInst.getOperands().get(1 - i),
                      simpleSub), false);
//              return new BinaryInst(TAG_.Add, factory.getI32Ty(), addInst.getOperands().get(1 - i),
//                  simpleSub, targetBB);
            }
          }

        }

        // (X - Y) - Z -> (X - Z) - Y or X - (Y + Z)
        case Sub -> {
          BinaryInst subInst = (BinaryInst) lhs;
          var subLhs = subInst.getOperands().get(0);
          var subRhs = subInst.getOperands().get(1);
          BinaryInst tmpInst = new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), subLhs,
              rhs);
          Value simpleSub = simplifySubInst(tmpInst, false);
          if (simpleSub != tmpInst) {
            return simplifySubInst(
                new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), simpleSub, subRhs),
                false);
//            return new BinaryInst(TAG_.Sub, factory.getI32Ty(), simpleSub, subRhs, targetBB);
          }

          tmpInst = new BinaryInst(targetEndInst, TAG_.Add, factory.getI32Ty(), subRhs, rhs);
          Value simpleAdd = simplifyAddInst(tmpInst, false);
          if (simpleAdd != tmpInst) {
            return simplifySubInst(
                new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), subLhs, simpleAdd),
                false);
//            return new BinaryInst(TAG_.Sub, factory.getI32Ty(), subLhs, simpleAdd, targetBB);
          }
        }
      }
    }

    if (rhs instanceof BinaryInst) {
      switch (((Instruction) rhs).tag) {
        // X - (Y + Z) -> (X - Y) - Z or (X - Z) - Y if everything simplifies.
        case Add -> {
          var addInst = (BinaryInst) rhs;
          for (var i = 0; i < 2; i++) {
            var x = addInst.getOperands().get(i);
            BinaryInst tmpInst = new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), lhs,
                x);
            Value simpleSub = simplifySubInst(tmpInst, false);
            if (simpleSub != tmpInst) {
              return simplifySubInst(
                  new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), simpleSub,
                      addInst.getOperands().get(1 - i)), false);
//              return new BinaryInst(TAG_.Sub, factory.getI32Ty(), simpleSub,
//                  addInst.getOperands().get(1 - i), targetBB);
            }
          }
        }

        // Z - (X - Y) -> (Z - X) + Y or (Z + Y) - X if everything simplifies.
        case Sub -> {
          var subInst = (BinaryInst) rhs;
          var subLhs = subInst.getOperands().get(0);
          var subRhs = subInst.getOperands().get(1);
          BinaryInst tmpInst = new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), lhs,
              subLhs);
          Value simpleSub = simplifySubInst(tmpInst, false);
          if (simpleSub != tmpInst) {
            return simplifyAddInst(
                new BinaryInst(targetEndInst, TAG_.Add, factory.getI32Ty(), simpleSub, subRhs),
                false);
//            return new BinaryInst(TAG_.Add, factory.getI32Ty(), simpleSub, subRhs, targetBB);
          }

          tmpInst = new BinaryInst(targetEndInst, TAG_.Add, factory.getI32Ty(), lhs, subRhs);
          Value simpleAdd = simplifyAddInst(tmpInst, false);
          if (simpleAdd != tmpInst) {
            return simplifySubInst(
                new BinaryInst(targetEndInst, TAG_.Sub, factory.getI32Ty(), simpleAdd, subLhs),
                false);
//            return new BinaryInst(TAG_.Sub, factory.getI32Ty(), simpleAdd, subLhs, targetBB);
          }
        }
      }
    }

    return inst;
  }

  public static Value simplifyMulInst(Instruction inst, boolean canRecur) {
    Value lhs = inst.getOperands().get(0);
    Value rhs = inst.getOperands().get(1);
    if (lhs instanceof GlobalVariable) {
      lhs = ((GlobalVariable) lhs).init;
    }
    if (rhs instanceof GlobalVariable) {
      rhs = ((GlobalVariable) rhs).init;
    }

    var targetBB = inst.getBB();

    Value c = foldConstant(inst.tag, lhs, rhs);
    if (c != null) {
      return c;
    }
    if (lhs instanceof ConstantInt || lhs.getType().isNoTy()) {
      inst.CORemoveAllOperand();
      inst.COaddOperand(rhs);
      inst.COaddOperand(lhs);
      lhs = inst.getOperands().get(0);
      rhs = inst.getOperands().get(1);
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

    if (!canRecur) {
      return inst;
    }

    // TODO 乘法结合律优化，共4种
    // TODO 乘法分配律优化

    return inst;
  }

  public static Value simplifyDivInst(Instruction inst, boolean canRecur) {
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

    if (!canRecur) {
      return inst;
    }

    // lhs / 1 -> lhs
    if (rhs instanceof ConstantInt && ((ConstantInt) rhs).getVal() == 1) {
      return lhs;
    }

    return inst;
  }

  public static Value simplifyLtInst(Instruction inst, boolean canRecur) {
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

    return inst;
  }

  public static Value simplifyLeInst(Instruction inst, boolean canRecur) {
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

    return inst;
  }

  public static Value simplifyGeInst(Instruction inst, boolean canRecur) {
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

    return inst;
  }

  public static Value simplifyGtInst(Instruction inst, boolean canRecur) {
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

    return inst;
  }

  public static Value simplifyEqInst(Instruction inst, boolean canRecur) {
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

    // lhs == rhs -> 1
    if (lhs.equals(rhs)) {
      return ConstantInt.newOne(factory.getI1Ty(), 1);
    }

    return inst;
  }

  public static Value simplifyNeInst(Instruction inst, boolean canRecur) {
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

    // lhs == rhs -> 0
    if (lhs.equals(rhs)) {
      return ConstantInt.newOne(factory.getI1Ty(), 0);
    }

    return inst;
  }

  public static Value simplifyAndInst(Instruction inst, boolean canRecur) {
    return inst;
  }

  public static Value simplifyOrInst(Instruction inst, boolean canRecur) {
    return inst;
  }

  public static Value simplifyGEPInst(Instruction inst, boolean canRecur) {
    return inst;
  }

  public static Value simplifyPhiInst(Instruction inst, boolean canRecur) {
    return inst;
  }

  public static Value simplifyAllocaInst(Instruction inst, boolean canRecur) {
    return inst;
  }

  public static Value simplifyLoadInst(Instruction inst, boolean canRecur) {
    return inst;
  }

  public static Value simplifyCallInst(Instruction inst, boolean canRecur) {
    return inst;
  }
}

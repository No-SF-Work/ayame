package pass.ir;

import frontend.Visitor;
import ir.MyFactoryBuilder;
import ir.MyModule;
import ir.types.ArrayType;
import ir.types.IntegerType;
import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Constants.ConstantArray;
import ir.values.Constants.ConstantInt;
import ir.values.Function;
import ir.values.Value;
import ir.values.instructions.Instruction;
import ir.values.instructions.MemInst.AllocaInst;
import ir.values.instructions.MemInst.GEPInst;
import ir.values.instructions.MemInst.LoadInst;
import ir.values.instructions.MemInst.StoreInst;
import ir.values.instructions.TerminatorInst.CallInst;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;
import pass.Pass.IRPass;
import util.IList.INode;

public class LocalArrayPromotion implements IRPass {

  @Override
  public String getName() {
    return "promotion";
  }

  //
  MyModule m;
  MyFactoryBuilder f = MyFactoryBuilder.getInstance();

  @Override
  public void run(MyModule m) {
    this.m = m;
    m.__functions.forEach(funcNode -> {
      if (!funcNode.getVal().isBuiltin_()) {
        funcNode.getVal().getList_().getEntry().getVal().getList().forEach(inst -> {
          if (inst.getVal() instanceof AllocaInst) {
            counter = 0;
            analyse((AllocaInst) inst.getVal());
          }
        });
      }
    });
  }

  /*
  如果一个局部数组
  1.所有store的位置和值都是CONSTm,而且没有被覆盖
  2.没有store出现在load后面
  那么这个数组可以提升为GV
  todo :在用funcInline的方法加上一点trivial的数据流分析
  */
  public void analyse(AllocaInst alloca) {
    if (!(alloca.getAllocatedType() instanceof ArrayType)) {
      return;
    }
    buffer = new Constant[((ArrayType) alloca.getAllocatedType()).getIntContains()];
    for (int i = 0; i < buffer.length; i++) {
      buffer[i] = ConstantInt.CONST0();
    }
    //因为会进行各种结构变换与代码移动，所以初始化的值不一定紧贴GEP和ALLOCA，得进行一个bfs,好烦啊，早知道我一开始写一个bfs的接口了
    canBePromote = true;
    curArr = alloca;
    storeStores();
    if (canBePromote) {
      bfsAndAnalyse(alloca.getBB());
      if (canBePromote) {
        if (counter == stores.size()) {
          promote();
        }
      }
    }

    return;
  }

  private AllocaInst curArr;
  private Constant[] buffer;
  private ArrayList<StoreInst> stores = new ArrayList<>();
  private int counter = 0;
  private HashSet<BasicBlock> visitmap = new HashSet<>();
  private Queue<BasicBlock> bbqueue = new LinkedList<>();
  private boolean canBePromote = true;

  public void storeStores() {
    stores.clear();
    assert curArr != null;
    for (INode<BasicBlock, Function> bbnode : curArr.getBB().getParent().getList_()) {
      for (INode<Instruction, BasicBlock> instnode : bbnode.getVal().getList()) {
        if (instnode.getVal() instanceof StoreInst) {
          var ptr = ((StoreInst) instnode.getVal()).getPointer();
          if (ptr instanceof GEPInst) {
            if (((GEPInst) ptr).getAimTo() != null) {
              if (((GEPInst) ptr).getAimTo().equals(curArr)) {
                StoreInst store = (StoreInst) instnode.getVal();
                if (!(store.getVal() instanceof ConstantInt)) {
                  canBePromote = false;
                }
                GEPInst pointer = (GEPInst) store.getPointer();
                for (int i = 1; i < pointer.getOperands().size(); i++) {
                  var operand = pointer.getOperands().get(i);
                  if (!(operand instanceof Constant)) {
                    canBePromote = false;
                  }
                }
                if (canBePromote) {
                  if (pointer.getOperands().size() == 3) {
                    var loc = ((ConstantInt) pointer.getOperands()
                        .get(2)).getVal();
                    buffer[loc] = (Constant) store.getVal();
                    stores.add(store);
                  } else if (pointer.getOperands().size() == 2) {
                    var loc = ((ConstantInt) pointer.getOperands()
                        .get(1)).getVal();
                    buffer[loc] = (Constant) store.getVal();
                    stores.add(store);
                  } else {
                    canBePromote = false;
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private boolean metLoad = false;

  public void bfsAndAnalyse(BasicBlock start) {
    counter = 0;
    metLoad = false;
    visitmap.clear();
    bbqueue.clear();
    bbqueue.add(start);
    BasicBlock b;
    while (!bbqueue.isEmpty() && !metLoad) {
      b = bbqueue.poll();
      analyse(b);
      for (BasicBlock succ : b.getSuccessor_()) {
        if (!visitmap.contains(succ)) {
          visitmap.add(succ);
          bbqueue.add(succ);
        }
      }
    }
  }

  public void analyse(BasicBlock b) {
    visitmap.add(b);
    b.getList().forEach(instNode -> {
      var val = instNode.getVal();
      if (val instanceof LoadInst) {
        LoadInst load = (LoadInst) val;
        if (load.getPointer() instanceof GEPInst) {
          if (((GEPInst) load.getPointer()).getAimTo().equals(curArr)) {
            metLoad = true;
          }
        }
      }
      if (val instanceof StoreInst) {
        StoreInst store = (StoreInst) val;
        if (!metLoad) {
          if (store.getPointer() instanceof GEPInst) {
            if (((GEPInst) store.getPointer()).getAimTo().equals(curArr)) {
              counter++;
            }
          }
        }
      }
      if (val instanceof CallInst) {
        CallInst call = (CallInst) val;
        for (int i = 1; i < call.getOperands().size(); i++) {
          Value v = call.getOperands().get(i);
          if (v instanceof GEPInst) {
            if (((GEPInst) v).getAimTo().equals(curArr)) {
              if (call.getOperands().get(0).getName() != "memset") {
                canBePromote = false;
              } else {
                memset = call;
              }
            }
          }
        }
      }

    });
  }

  CallInst memset;

  public Constant packConstArr(ArrayList<Integer> dims, ArrayList<Constant> inits) {
    var curDimLength = dims.get(0);
    var curDimArr = new ArrayList<Constant>();
    var length = inits.size() / curDimLength;
    Type arrTy = IntegerType.getI32();
    if (length == 1) {
      for (int i = 0; i < curDimLength; i++) {
        curDimArr.add(inits.get(i));
      }
    } else {
      for (int i = 0; i < curDimLength; i++) {
        //fix subDims and add to curDimArr
        curDimArr.add(
            packConstArr(
                new ArrayList<>(dims.subList(1, dims.size())),
                new ArrayList<>(inits.subList(length * i, length * (i + 1)))));

      }
    }

    for (int i = dims.size(); i > 0; i--) {
      arrTy = f.getArrayTy(arrTy, dims.get(i - 1));
    }
    return f.getConstantArray(arrTy, curDimArr);
  }

  public void promote() {
    ArrayList<Constant> init = (ArrayList<Constant>) Arrays.stream(buffer)
        .collect(Collectors.toList());
    ArrayList<Integer> dims = new ArrayList<>();
    Type ty = curArr.getAllocatedType();
    while (ty instanceof ArrayType) {
      dims.add(((ArrayType) ty).getNumEle());
      ty = ((ArrayType) ty).getELeType();
    }
    Constant fixedInit = packConstArr(dims, init);
    ConstantArray arr = new ConstantArray(curArr.getAllocatedType(), init);
    var gv = f.getGlobalvariable("promoted" + String.valueOf(counter), curArr.getAllocatedType(),
        fixedInit,
        arr);
    gv.setConst();
    stores.forEach(store -> {
      store.CORemoveAllOperand();
      store.node.removeSelf();
    });
    if (memset != null) {
      memset.CORemoveAllOperand();
      memset.node.removeSelf();
      memset = null;
    }
    curArr.COReplaceAllUseWith(gv);
  }
}

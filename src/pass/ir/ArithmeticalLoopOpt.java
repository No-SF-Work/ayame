package pass.ir;

import ir.MyModule;
import ir.values.Function;
import ir.values.instructions.MemInst;
import pass.Pass;
import util.IList;

public class ArithmeticalLoopOpt implements Pass.IRPass {
    @Override
    public String getName() {
        return "ArithmeticalLoopOpt";
    }

    private void optAriLoop(Function func) {
        var loopInfo = func.getLoopInfo();
        for (var loop : loopInfo.getAllLoops()) {
//            if (loop.getBlocks().size() == 4) {
                System.out.println(func.getName());
                System.out.println(loop.getBlocks().size());
//            }
        }
    }

    @Override
    public void run(MyModule myModule) {
        for (var funcEntry : myModule.__functions) {
            var func = funcEntry.getVal();
            if (!func.isBuiltin_()) {
                optAriLoop(func);
            }
        }
    }
}

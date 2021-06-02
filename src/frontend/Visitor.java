package frontend;


import driver.Config;
import frontend.SysYParser.*;
import ir.MyModule;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.logging.Logger;
import util.Mylogger;

/**
 * 我们并不需要用返回值传递信息，所以将类型标注为Void
 */

public class Visitor extends SysYBaseVisitor<Void> {

  Logger log = Mylogger.getLogger(Visitor.class);


  private class Scope {

    private Stack<HashMap<String, Value>> symbols_;
    private Stack<HashMap<String, Value>> params;

    public void find(String name) {

    }

    public void addLayer() {
      symbols_.add(new HashMap<>());
      params.add(new HashMap<>());
    }

    public void pop() {
      symbols_.pop();
      params.pop();
    }

    public void addParams() {
      //todo 咱先把没数组的部分弄完
    }

    public void popParams() {
      //todo
    }

    public boolean isGlobal() {
      return this.symbols_.size() == 1;
    }

  }

  private Scope scope_ = new Scope();//symbol table
  private BasicBlock curBB_;//current basicBlock
  private Function curFunc_;//current function
  private final MyModule m = MyModule.getInstance();
  private Value tmp_;//used to trans messages between visits
  private int tmpInt_;

  @Override
  public Void visitProgram(ProgramContext ctx) {
    log.info("Syntax begin");
    ctx.compUnit().accept(this);
    return null;
  }

  @Override
  public Void visitCompUnit(CompUnitContext ctx) {
    for (int i = 0; i < ctx.children.size(); i++) {
      ctx.getChild(i).accept(this);
    }
    return null;
  }

  @Override
  public Void visitDecl(DeclContext ctx) {
    ctx.getChild(0).accept(this);
    return null;
  }

  @Override
  public Void visitConstDecl(ConstDeclContext ctx) {
//todo
    return super.visitConstDecl(ctx);
  }

  @Override
  public Void visitBType(BTypeContext ctx) {
    //todo
    return super.visitBType(ctx);
  }

  @Override
  public Void visitConstDef(ConstDefContext ctx) {
    //todo
    return super.visitConstDef(ctx);
  }

  @Override
  public Void visitConstInitVal(ConstInitValContext ctx) {
    //todo
    return super.visitConstInitVal(ctx);
  }

  @Override
  public Void visitVarDecl(VarDeclContext ctx) {
    //todo
    return super.visitVarDecl(ctx);
  }

  @Override
  public Void visitVarDef(VarDefContext ctx) {
    //todo
    return super.visitVarDef(ctx);
  }

  @Override
  public Void visitInitVal(InitValContext ctx) {
    //todo
    return super.visitInitVal(ctx);
  }

  @Override
  public Void visitFuncDef(FuncDefContext ctx) {
    //todo
    return super.visitFuncDef(ctx);
  }

  @Override
  public Void visitFuncType(FuncTypeContext ctx) {
    //todo
    return super.visitFuncType(ctx);
  }

  @Override
  public Void visitFuncFParams(FuncFParamsContext ctx) {
    //todo
    return super.visitFuncFParams(ctx);
  }

  @Override
  public Void visitFuncFParam(FuncFParamContext ctx) {
    //todo
    return super.visitFuncFParam(ctx);
  }

  @Override
  public Void visitBlock(BlockContext ctx) {
    //todo
    return super.visitBlock(ctx);
  }

  @Override
  public Void visitBlockItem(BlockItemContext ctx) {
    //todo
    return super.visitBlockItem(ctx);
  }

  @Override
  public Void visitStmt(StmtContext ctx) {
    //todo
    return super.visitStmt(ctx);
  }

  @Override
  public Void visitAssignStmt(AssignStmtContext ctx) {
    //todo
    return super.visitAssignStmt(ctx);
  }

  @Override
  public Void visitExpStmt(ExpStmtContext ctx) {
    //todo
    return super.visitExpStmt(ctx);
  }

  @Override
  public Void visitConditionStmt(ConditionStmtContext ctx) {
    //todo
    return super.visitConditionStmt(ctx);
  }

  @Override
  public Void visitWhileStmt(WhileStmtContext ctx) {
    //todo
    return super.visitWhileStmt(ctx);
  }

  @Override
  public Void visitBreakStmt(BreakStmtContext ctx) {
    //todo
    return super.visitBreakStmt(ctx);
  }

  @Override
  public Void visitContinueStmt(ContinueStmtContext ctx) {
    //todo
    return super.visitContinueStmt(ctx);
  }

  @Override
  public Void visitReturnStmt(ReturnStmtContext ctx) {
    //todo
    return super.visitReturnStmt(ctx);
  }

  @Override
  public Void visitExp(ExpContext ctx) {
    //todo
    return super.visitExp(ctx);
  }

  @Override
  public Void visitCond(CondContext ctx) {
    //todo
    return super.visitCond(ctx);
  }

  @Override
  public Void visitLVal(LValContext ctx) {
    //todo
    return super.visitLVal(ctx);
  }

  @Override
  public Void visitPrimaryExp(PrimaryExpContext ctx) {
    //todo
    return super.visitPrimaryExp(ctx);
  }

  @Override
  public Void visitNumber(NumberContext ctx) {
    //todo

    return null;
  }

  @Override
  public Void visitIntConst(IntConstContext ctx) {
    //todo
    return super.visitIntConst(ctx);
  }

  @Override
  public Void visitUnaryExp(UnaryExpContext ctx) {
    //todo
    return super.visitUnaryExp(ctx);
  }

  @Override
  public Void visitCallee(CalleeContext ctx) {
    //todo
    return super.visitCallee(ctx);
  }

  @Override
  public Void visitUnaryOp(UnaryOpContext ctx) {
    //todo
    return super.visitUnaryOp(ctx);
  }

  @Override
  public Void visitFuncRParams(FuncRParamsContext ctx) {
    //todo
    return super.visitFuncRParams(ctx);
  }

  @Override
  public Void visitParam(ParamContext ctx) {
    //todo
    return super.visitParam(ctx);
  }

  @Override
  public Void visitMulExp(MulExpContext ctx) {
    //todo
    return super.visitMulExp(ctx);
  }

  @Override
  public Void visitMulOp(MulOpContext ctx) {
    //todo
    return super.visitMulOp(ctx);
  }

  @Override
  public Void visitAddExp(AddExpContext ctx) {
    //todo
    return super.visitAddExp(ctx);
  }

  @Override
  public Void visitAddOp(AddOpContext ctx) {
    //todo
    return super.visitAddOp(ctx);
  }

  @Override
  public Void visitRelExp(RelExpContext ctx) {
    //todo
    return super.visitRelExp(ctx);
  }

  @Override
  public Void visitRelOp(RelOpContext ctx) {
    //todo
    return super.visitRelOp(ctx);
  }

  @Override
  public Void visitEqExp(EqExpContext ctx) {
    //todo

    return super.visitEqExp(ctx);
  }

  @Override
  public Void visitEqOp(EqOpContext ctx) {//todo

    return super.visitEqOp(ctx);
  }

  @Override
  public Void visitLAndExp(LAndExpContext ctx) {//todo
    return super.visitLAndExp(ctx);
  }

  @Override
  public Void visitLOrExp(LOrExpContext ctx) {//todo
    return super.visitLOrExp(ctx);
  }

  @Override
  public Void visitConstExp(ConstExpContext ctx) {//todo
    return super.visitConstExp(ctx);
  }
}

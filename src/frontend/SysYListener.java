package frontend;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SysYParser}.
 */
public interface SysYListener extends ParseTreeListener {
    /**
     * Enter a parse tree produced by {@link SysYParser#program}.
     *
     * @param ctx the parse tree
     */
    void enterProgram(SysYParser.ProgramContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#program}.
     *
     * @param ctx the parse tree
     */
    void exitProgram(SysYParser.ProgramContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#compUnit}.
     *
     * @param ctx the parse tree
     */
    void enterCompUnit(SysYParser.CompUnitContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#compUnit}.
     *
     * @param ctx the parse tree
     */
    void exitCompUnit(SysYParser.CompUnitContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#decl}.
     *
     * @param ctx the parse tree
     */
    void enterDecl(SysYParser.DeclContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#decl}.
     *
     * @param ctx the parse tree
     */
    void exitDecl(SysYParser.DeclContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#constDecl}.
     *
     * @param ctx the parse tree
     */
    void enterConstDecl(SysYParser.ConstDeclContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#constDecl}.
     *
     * @param ctx the parse tree
     */
    void exitConstDecl(SysYParser.ConstDeclContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#bType}.
     *
     * @param ctx the parse tree
     */
    void enterBType(SysYParser.BTypeContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#bType}.
     *
     * @param ctx the parse tree
     */
    void exitBType(SysYParser.BTypeContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#constDef}.
     *
     * @param ctx the parse tree
     */
    void enterConstDef(SysYParser.ConstDefContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#constDef}.
     *
     * @param ctx the parse tree
     */
    void exitConstDef(SysYParser.ConstDefContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#constInitVal}.
     *
     * @param ctx the parse tree
     */
    void enterConstInitVal(SysYParser.ConstInitValContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#constInitVal}.
     *
     * @param ctx the parse tree
     */
    void exitConstInitVal(SysYParser.ConstInitValContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#varDecl}.
     *
     * @param ctx the parse tree
     */
    void enterVarDecl(SysYParser.VarDeclContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#varDecl}.
     *
     * @param ctx the parse tree
     */
    void exitVarDecl(SysYParser.VarDeclContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#varDef}.
     *
     * @param ctx the parse tree
     */
    void enterVarDef(SysYParser.VarDefContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#varDef}.
     *
     * @param ctx the parse tree
     */
    void exitVarDef(SysYParser.VarDefContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#initVal}.
     *
     * @param ctx the parse tree
     */
    void enterInitVal(SysYParser.InitValContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#initVal}.
     *
     * @param ctx the parse tree
     */
    void exitInitVal(SysYParser.InitValContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#funcDef}.
     *
     * @param ctx the parse tree
     */
    void enterFuncDef(SysYParser.FuncDefContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#funcDef}.
     *
     * @param ctx the parse tree
     */
    void exitFuncDef(SysYParser.FuncDefContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#funcType}.
     *
     * @param ctx the parse tree
     */
    void enterFuncType(SysYParser.FuncTypeContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#funcType}.
     *
     * @param ctx the parse tree
     */
    void exitFuncType(SysYParser.FuncTypeContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#funcFParams}.
     *
     * @param ctx the parse tree
     */
    void enterFuncFParams(SysYParser.FuncFParamsContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#funcFParams}.
     *
     * @param ctx the parse tree
     */
    void exitFuncFParams(SysYParser.FuncFParamsContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#funcFParam}.
     *
     * @param ctx the parse tree
     */
    void enterFuncFParam(SysYParser.FuncFParamContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#funcFParam}.
     *
     * @param ctx the parse tree
     */
    void exitFuncFParam(SysYParser.FuncFParamContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#block}.
     *
     * @param ctx the parse tree
     */
    void enterBlock(SysYParser.BlockContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#block}.
     *
     * @param ctx the parse tree
     */
    void exitBlock(SysYParser.BlockContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#blockItem}.
     *
     * @param ctx the parse tree
     */
    void enterBlockItem(SysYParser.BlockItemContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#blockItem}.
     *
     * @param ctx the parse tree
     */
    void exitBlockItem(SysYParser.BlockItemContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#stmt}.
     *
     * @param ctx the parse tree
     */
    void enterStmt(SysYParser.StmtContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#stmt}.
     *
     * @param ctx the parse tree
     */
    void exitStmt(SysYParser.StmtContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#assignStmt}.
     *
     * @param ctx the parse tree
     */
    void enterAssignStmt(SysYParser.AssignStmtContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#assignStmt}.
     *
     * @param ctx the parse tree
     */
    void exitAssignStmt(SysYParser.AssignStmtContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#expStmt}.
     *
     * @param ctx the parse tree
     */
    void enterExpStmt(SysYParser.ExpStmtContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#expStmt}.
     *
     * @param ctx the parse tree
     */
    void exitExpStmt(SysYParser.ExpStmtContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#conditionStmt}.
     *
     * @param ctx the parse tree
     */
    void enterConditionStmt(SysYParser.ConditionStmtContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#conditionStmt}.
     *
     * @param ctx the parse tree
     */
    void exitConditionStmt(SysYParser.ConditionStmtContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#whileStmt}.
     *
     * @param ctx the parse tree
     */
    void enterWhileStmt(SysYParser.WhileStmtContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#whileStmt}.
     *
     * @param ctx the parse tree
     */
    void exitWhileStmt(SysYParser.WhileStmtContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#breakStmt}.
     *
     * @param ctx the parse tree
     */
    void enterBreakStmt(SysYParser.BreakStmtContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#breakStmt}.
     *
     * @param ctx the parse tree
     */
    void exitBreakStmt(SysYParser.BreakStmtContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#continueStmt}.
     *
     * @param ctx the parse tree
     */
    void enterContinueStmt(SysYParser.ContinueStmtContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#continueStmt}.
     *
     * @param ctx the parse tree
     */
    void exitContinueStmt(SysYParser.ContinueStmtContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#returnStmt}.
     *
     * @param ctx the parse tree
     */
    void enterReturnStmt(SysYParser.ReturnStmtContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#returnStmt}.
     *
     * @param ctx the parse tree
     */
    void exitReturnStmt(SysYParser.ReturnStmtContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#exp}.
     *
     * @param ctx the parse tree
     */
    void enterExp(SysYParser.ExpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#exp}.
     *
     * @param ctx the parse tree
     */
    void exitExp(SysYParser.ExpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#cond}.
     *
     * @param ctx the parse tree
     */
    void enterCond(SysYParser.CondContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#cond}.
     *
     * @param ctx the parse tree
     */
    void exitCond(SysYParser.CondContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#lVal}.
     *
     * @param ctx the parse tree
     */
    void enterLVal(SysYParser.LValContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#lVal}.
     *
     * @param ctx the parse tree
     */
    void exitLVal(SysYParser.LValContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#primaryExp}.
     *
     * @param ctx the parse tree
     */
    void enterPrimaryExp(SysYParser.PrimaryExpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#primaryExp}.
     *
     * @param ctx the parse tree
     */
    void exitPrimaryExp(SysYParser.PrimaryExpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#number}.
     *
     * @param ctx the parse tree
     */
    void enterNumber(SysYParser.NumberContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#number}.
     *
     * @param ctx the parse tree
     */
    void exitNumber(SysYParser.NumberContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#intConst}.
     *
     * @param ctx the parse tree
     */
    void enterIntConst(SysYParser.IntConstContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#intConst}.
     *
     * @param ctx the parse tree
     */
    void exitIntConst(SysYParser.IntConstContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#unaryExp}.
     *
     * @param ctx the parse tree
     */
    void enterUnaryExp(SysYParser.UnaryExpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#unaryExp}.
     *
     * @param ctx the parse tree
     */
    void exitUnaryExp(SysYParser.UnaryExpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#callee}.
     *
     * @param ctx the parse tree
     */
    void enterCallee(SysYParser.CalleeContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#callee}.
     *
     * @param ctx the parse tree
     */
    void exitCallee(SysYParser.CalleeContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#unaryOp}.
     *
     * @param ctx the parse tree
     */
    void enterUnaryOp(SysYParser.UnaryOpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#unaryOp}.
     *
     * @param ctx the parse tree
     */
    void exitUnaryOp(SysYParser.UnaryOpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#funcRParams}.
     *
     * @param ctx the parse tree
     */
    void enterFuncRParams(SysYParser.FuncRParamsContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#funcRParams}.
     *
     * @param ctx the parse tree
     */
    void exitFuncRParams(SysYParser.FuncRParamsContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#param}.
     *
     * @param ctx the parse tree
     */
    void enterParam(SysYParser.ParamContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#param}.
     *
     * @param ctx the parse tree
     */
    void exitParam(SysYParser.ParamContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#mulExp}.
     *
     * @param ctx the parse tree
     */
    void enterMulExp(SysYParser.MulExpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#mulExp}.
     *
     * @param ctx the parse tree
     */
    void exitMulExp(SysYParser.MulExpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#mulOp}.
     *
     * @param ctx the parse tree
     */
    void enterMulOp(SysYParser.MulOpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#mulOp}.
     *
     * @param ctx the parse tree
     */
    void exitMulOp(SysYParser.MulOpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#addExp}.
     *
     * @param ctx the parse tree
     */
    void enterAddExp(SysYParser.AddExpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#addExp}.
     *
     * @param ctx the parse tree
     */
    void exitAddExp(SysYParser.AddExpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#addOp}.
     *
     * @param ctx the parse tree
     */
    void enterAddOp(SysYParser.AddOpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#addOp}.
     *
     * @param ctx the parse tree
     */
    void exitAddOp(SysYParser.AddOpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#relExp}.
     *
     * @param ctx the parse tree
     */
    void enterRelExp(SysYParser.RelExpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#relExp}.
     *
     * @param ctx the parse tree
     */
    void exitRelExp(SysYParser.RelExpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#relOp}.
     *
     * @param ctx the parse tree
     */
    void enterRelOp(SysYParser.RelOpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#relOp}.
     *
     * @param ctx the parse tree
     */
    void exitRelOp(SysYParser.RelOpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#eqExp}.
     *
     * @param ctx the parse tree
     */
    void enterEqExp(SysYParser.EqExpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#eqExp}.
     *
     * @param ctx the parse tree
     */
    void exitEqExp(SysYParser.EqExpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#eqOp}.
     *
     * @param ctx the parse tree
     */
    void enterEqOp(SysYParser.EqOpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#eqOp}.
     *
     * @param ctx the parse tree
     */
    void exitEqOp(SysYParser.EqOpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#lAndExp}.
     *
     * @param ctx the parse tree
     */
    void enterLAndExp(SysYParser.LAndExpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#lAndExp}.
     *
     * @param ctx the parse tree
     */
    void exitLAndExp(SysYParser.LAndExpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#lOrExp}.
     *
     * @param ctx the parse tree
     */
    void enterLOrExp(SysYParser.LOrExpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#lOrExp}.
     *
     * @param ctx the parse tree
     */
    void exitLOrExp(SysYParser.LOrExpContext ctx);

    /**
     * Enter a parse tree produced by {@link SysYParser#constExp}.
     *
     * @param ctx the parse tree
     */
    void enterConstExp(SysYParser.ConstExpContext ctx);

    /**
     * Exit a parse tree produced by {@link SysYParser#constExp}.
     *
     * @param ctx the parse tree
     */
    void exitConstExp(SysYParser.ConstExpContext ctx);
}
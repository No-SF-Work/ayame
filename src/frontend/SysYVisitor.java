package frontend;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SysYParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SysYVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SysYParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(SysYParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#compUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompUnit(SysYParser.CompUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl(SysYParser.DeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#constDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDecl(SysYParser.ConstDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#bType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBType(SysYParser.BTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#constDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDef(SysYParser.ConstDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#constInitVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstInitVal(SysYParser.ConstInitValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#varDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDecl(SysYParser.VarDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#varDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDef(SysYParser.VarDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#initVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitVal(SysYParser.InitValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncDef(SysYParser.FuncDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncType(SysYParser.FuncTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcFParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncFParams(SysYParser.FuncFParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcFParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncFParam(SysYParser.FuncFParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(SysYParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#blockItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockItem(SysYParser.BlockItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(SysYParser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#assignStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignStmt(SysYParser.AssignStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#expStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpStmt(SysYParser.ExpStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#conditionStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionStmt(SysYParser.ConditionStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#whileStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStmt(SysYParser.WhileStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#breakStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStmt(SysYParser.BreakStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#continueStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStmt(SysYParser.ContinueStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#returnStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStmt(SysYParser.ReturnStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExp(SysYParser.ExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#cond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCond(SysYParser.CondContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#lVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLVal(SysYParser.LValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#primaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExp(SysYParser.PrimaryExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(SysYParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#intConst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntConst(SysYParser.IntConstContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#unaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExp(SysYParser.UnaryExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#callee}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallee(SysYParser.CalleeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#unaryOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOp(SysYParser.UnaryOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcRParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncRParams(SysYParser.FuncRParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam(SysYParser.ParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#mulExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulExp(SysYParser.MulExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#mulOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulOp(SysYParser.MulOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#addExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddExp(SysYParser.AddExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#addOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddOp(SysYParser.AddOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#relExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelExp(SysYParser.RelExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#relOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelOp(SysYParser.RelOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#eqExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqExp(SysYParser.EqExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#eqOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqOp(SysYParser.EqOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#lAndExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLAndExp(SysYParser.LAndExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#lOrExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLOrExp(SysYParser.LOrExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#constExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstExp(SysYParser.ConstExpContext ctx);
}
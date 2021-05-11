package frontend;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class AntlrTester {
    public static String genTreeString(String[] args) {
        String filePath = args[0];
        String treeString = new String();

        try {
            CharStream input = CharStreams.fromFileName(filePath);
            SysYLexer lexer = new SysYLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SysYParser parser = new SysYParser(tokens);
            ParseTree tree = parser.program();
            treeString = tree.toStringTree(parser);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return treeString;
    }
}

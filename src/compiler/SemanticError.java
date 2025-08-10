package compiler;

import gen.japyListener;
import gen.japyParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class SemanticError implements japyListener {
    private Hashtable<String, SymbolTable.SymbolInfo> SemanticError;
    private Stack<String> currentScope;
    private Hashtable<String, Hashtable<String, SymbolTable.SymbolInfo>> allScopes;
    private String inputFile;
    private List<String> scopeOrder;

    public SemanticError() {
        this.SemanticError = new Hashtable<>();
        this.currentScope = new Stack<>();
        this.allScopes = new Hashtable<>();
        this.currentScope.push("global");
        this.allScopes.put("global", new Hashtable<>());
        this.scopeOrder = new ArrayList<>();
        this.scopeOrder.add("global");
    }

    @Override
    public void enterProgram(japyParser.ProgramContext ctx) {
        inputFile = ctx.getStart().getInputStream().getSourceName();
    }

    @Override
    public void exitProgram(japyParser.ProgramContext ctx) {
        System.out.printf("------------------ program:\"%s\" ------------------%n", inputFile);
        List<String> orderedScopes = new ArrayList<>(Arrays.asList("global"));

        for (String scope : scopeOrder) {
            if (!scope.equals("global")) {
                orderedScopes.add(scope);
            }
        }

        for (String scope : orderedScopes) {
            System.out.printf("-------------------- %s --------------------%n", scope);
            Hashtable<String, SymbolTable.SymbolInfo> scopeTable = allScopes.getOrDefault(scope, new Hashtable<>());

            List<String> sortedKeys = new ArrayList<>(scopeTable.keySet());
            sortedKeys.sort(Comparator.naturalOrder());

            if (scope.startsWith("if") || scope.startsWith("while")) {
                System.out.println("                    !NO KEY FOUND!");
                for (String key : sortedKeys) {
                    SymbolTable.SymbolInfo info = scopeTable.get(key);
                    if (info.kind.equals("variable")) {
                        System.out.printf("key = %s, value = (name: %s) (first_appearance: %d)%n", key, info.name, info.firstAppearance);
                    }
                }
            } else {
                for (String key : sortedKeys) {
                    if (scope.startsWith("else")){
                        SymbolTable.SymbolInfo info = scopeTable.get(key);
                        System.out.printf("key = %s, value = (name: %s) (first_appearance: %d)%n", key, info.name, info.firstAppearance);
                    }
                    else  {
                        SymbolTable.SymbolInfo info = scopeTable.get(key);
                        System.out.printf("key = %s, value = %s%n", key, info);
                    }
                }

                if (scopeTable.isEmpty()) {
                    System.out.println("                    !NO KEY FOUND!");
                }
            }

            System.out.println("------------------------------------------------------");
        }
    }


    @Override
    public void enterClassDeclaration(japyParser.ClassDeclarationContext ctx) {
        String className = ctx.className.getText();
        String accessModifier = (ctx.access_modifier() != null) ? ctx.access_modifier().getText() : "default";
        String inherits = (ctx.classParent != null) ? ctx.classParent.getText() : "";
        String key = "class_" + className;

        SymbolTable.SymbolInfo classInfo = new SymbolTable.SymbolInfo("class", className);
        classInfo.accessModifier = accessModifier;
        if (!inherits.isEmpty()) {
            classInfo.inherits = "class_" + inherits;
        }

        Hashtable<String, SymbolTable.SymbolInfo> globalScope = allScopes.get("global");
        if (globalScope.containsKey(key)) {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            System.out.println("Error102: in line [" + line + ":" + column + "], class [" + className + "] has been defined already.");
            String newKey = key + "_" + line + "_" + column;
            globalScope.put(newKey, classInfo);
        } else {
            globalScope.put(key, classInfo);
        }

        currentScope.push(key);
        allScopes.put(key, new Hashtable<>());

        if (!key.equals("class_global")) {
            scopeOrder.add(key);
        }
    }



    @Override
    public void exitClassDeclaration(japyParser.ClassDeclarationContext ctx) {
        currentScope.pop();
    }

    @Override
    public void enterEntryClassDeclaration(japyParser.EntryClassDeclarationContext ctx) {
    }

    @Override
    public void exitEntryClassDeclaration(japyParser.EntryClassDeclarationContext ctx) {
    }


    @Override
    public void enterFieldDeclaration(japyParser.FieldDeclarationContext ctx) {
        String fieldName = ctx.fieldName.getText();
        String fieldType = ctx.fieldType.getText();
        String accessModifier = (ctx.access_modifier() != null) ? ctx.access_modifier().getText() : "default";
        String key = "field_" + fieldName;

        SymbolTable.SymbolInfo fieldInfo = new SymbolTable.SymbolInfo("field", fieldName, fieldType);
        fieldInfo.accessModifier = accessModifier;

        Hashtable<String, SymbolTable.SymbolInfo> currentScopeTable = allScopes.get(currentScope.peek());
        if (currentScopeTable.containsKey(key)) {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            System.out.println("Error104: in line [" + line + ":" + column + "], field [" + fieldName + "] has been defined already.");
            String newKey = key + "_" + line + "_" + column;
            currentScopeTable.put(newKey, fieldInfo);
        } else {
            currentScopeTable.put(key, fieldInfo);
        }
    }




    @Override
    public void exitFieldDeclaration(japyParser.FieldDeclarationContext ctx) {
    }

    @Override
    public void enterAccess_modifier(japyParser.Access_modifierContext ctx) {
    }

    @Override
    public void exitAccess_modifier(japyParser.Access_modifierContext ctx) {
    }

    @Override
    public void enterMethodDeclaration(japyParser.MethodDeclarationContext ctx) {
        String methodName = ctx.methodName.getText();
        String returnType = ctx.t.getText();
        String accessModifier = (ctx.methodAccessModifier != null) ? ctx.methodAccessModifier.getText() : "default";

        StringBuilder methodSignature = new StringBuilder(methodName);
        methodSignature.append("(");
        if (ctx.param1 != null) {
            List<TerminalNode> paramNames = ctx.ID();
            List<japyParser.JapyTypeContext> paramTypes = ctx.japyType();
            for (int i = 1; i < paramNames.size(); i++) {
                String paramType = paramTypes.get(i - 1).getText();
                methodSignature.append(paramType);
                if (i < paramNames.size() - 1) {
                    methodSignature.append(",");
                }
            }
        }
        methodSignature.append(")");
        String key = "method_" + methodSignature.toString();

        SymbolTable.SymbolInfo methodInfo = new SymbolTable.SymbolInfo("method", methodName, returnType);
        methodInfo.accessModifier = accessModifier;
        methodInfo.parameters = new ArrayList<>();

        if (ctx.param1 != null) {
            List<TerminalNode> paramNames = ctx.ID();
            List<japyParser.JapyTypeContext> paramTypes = ctx.japyType();

            for (int i = 1; i < paramNames.size(); i++) {
                String paramName = paramNames.get(i).getText();
                String paramType = paramTypes.get(i - 1).getText();
                methodInfo.parameters.add(new SymbolTable.ParameterInfo(i, paramName, paramType));
            }
        }

        String currentScopeName = currentScope.peek();
        if (allScopes.get(currentScopeName).containsKey(key)) {
            int line = ctx.getStart().getLine();
            int column = ctx.getStart().getCharPositionInLine();
            System.out.println("Error102: in line [" + line + ":" + column + "], method [" + methodName + "] has been defined already.");
            String newKey = key + "_" + line + "_" + column;
            allScopes.get(currentScopeName).put(newKey, methodInfo);
        } else {
            String expectedReturnType = "MethodReturnType";
            if (!returnType.equals(expectedReturnType)) {
                int line = ctx.getStart().getLine();
                int column = ctx.getStart().getCharPositionInLine();
                System.out.println("Error210: in line [" + line + ":" + column + "], ReturnType of this method must be [" + returnType + "]");
            }

            // Get parameter names
            List<String> paramNames = new ArrayList<>();
            if (ctx.param1 != null) {
                paramNames.add(ctx.param1.getText());
            }
            if (ctx.param2 != null) {
                paramNames.add(ctx.param2.getText());
            }

            int formalParamCount = paramNames.size();
            int actualParamCount = ctx.ID().size() - 1;
            if (actualParamCount != formalParamCount) {
                int line = ctx.getStart().getLine();
                int column = ctx.getStart().getCharPositionInLine();
                System.out.println("Error210: Line " + line + ":" + column + ", Number of parameters in method call does not match with formal parameter count in method definition.");
            }

            allScopes.get(currentScopeName).put(key, methodInfo);
            currentScope.push(key);
            allScopes.put(key, new Hashtable<>());

            scopeOrder.add(key);
        }
    }


    @Override
    public void exitMethodDeclaration(japyParser.MethodDeclarationContext ctx) {
        currentScope.pop();
    }

    @Override
    public void enterClosedStatement(japyParser.ClosedStatementContext ctx) {
    }

    @Override
    public void exitClosedStatement(japyParser.ClosedStatementContext ctx) {
    }

    @Override
    public void enterClosedConditional(japyParser.ClosedConditionalContext ctx) {
        String ifScopeName = "if_" + ctx.getStart().getLine() + "_" + ctx.getStop().getLine();
        currentScope.push(ifScopeName);
        allScopes.put(ifScopeName, new Hashtable<>());
        scopeOrder.add(ifScopeName);

        SymbolTable.SymbolInfo ifInfo = new SymbolTable.SymbolInfo("if", ifScopeName);
        ifInfo.firstAppearance = ctx.getStart().getLine();
        allScopes.get(ifScopeName).put("if_condition", ifInfo);

        if (ctx.elseStmt != null) {
            String elseScopeName = "else_" + ctx.getStart().getLine() + "_" + ctx.getStop().getLine();
            currentScope.push(elseScopeName);
            allScopes.put(elseScopeName, new Hashtable<>());
            scopeOrder.add(elseScopeName);

            SymbolTable.SymbolInfo elseInfo = new SymbolTable.SymbolInfo("else", elseScopeName);
            elseInfo.firstAppearance = ctx.elseStmt.getStart().getLine();
//            allScopes.get(elseScopeName).put("else_condition", elseInfo);
        }
    }

    @Override
    public void exitClosedConditional(japyParser.ClosedConditionalContext ctx) {
        if (ctx.elseStmt != null) {
            currentScope.pop();
        }
        currentScope.pop();
    }

    @Override
    public void enterOpenConditional(japyParser.OpenConditionalContext ctx) {
    }

    @Override
    public void exitOpenConditional(japyParser.OpenConditionalContext ctx) {
    }

    @Override
    public void enterOpenStatement(japyParser.OpenStatementContext ctx) {
    }

    @Override
    public void exitOpenStatement(japyParser.OpenStatementContext ctx) {
    }

    @Override
    public void enterStatement(japyParser.StatementContext ctx) {
    }

    @Override
    public void exitStatement(japyParser.StatementContext ctx) {
    }

    @Override
    public void enterStatementVarDef(japyParser.StatementVarDefContext ctx) {
        for (TerminalNode varNode : ctx.ID()) {
            String varName = varNode.getText();
            SymbolTable.SymbolInfo varInfo = new SymbolTable.SymbolInfo("variable", varName);
            varInfo.firstAppearance = ctx.getStart().getLine();
            String currentScopeName = currentScope.peek();
            allScopes.get(currentScopeName).put("var_" + varName, varInfo);
        }
    }

    @Override
    public void exitStatementVarDef(japyParser.StatementVarDefContext ctx) {
    }

    @Override
    public void enterStatementBlock(japyParser.StatementBlockContext ctx) {
    }

    @Override
    public void exitStatementBlock(japyParser.StatementBlockContext ctx) {
    }

    @Override
    public void enterStatementContinue(japyParser.StatementContinueContext ctx) {
    }

    @Override
    public void exitStatementContinue(japyParser.StatementContinueContext ctx) {
    }

    @Override
    public void enterStatementBreak(japyParser.StatementBreakContext ctx) {
    }

    @Override
    public void exitStatementBreak(japyParser.StatementBreakContext ctx) {
    }

    @Override
    public void enterStatementReturn(japyParser.StatementReturnContext ctx) {
    }

    @Override
    public void exitStatementReturn(japyParser.StatementReturnContext ctx) {
    }


    @Override
    public void enterStatementClosedLoop(japyParser.StatementClosedLoopContext ctx) {
        String loopScopeName = "while_" + ctx.getStart().getLine() + "_" + ctx.getStop().getLine();
        currentScope.push(loopScopeName);
        allScopes.put(loopScopeName, new Hashtable<>());
        scopeOrder.add(loopScopeName);

        SymbolTable.SymbolInfo loopInfo = new SymbolTable.SymbolInfo("while", loopScopeName);
        loopInfo.firstAppearance = ctx.getStart().getLine();
        allScopes.get(loopScopeName).put("while_loop", loopInfo);
    }

    @Override
    public void exitStatementClosedLoop(japyParser.StatementClosedLoopContext ctx) {
        currentScope.pop();
    }


    @Override
    public void enterStatementOpenLoop(japyParser.StatementOpenLoopContext ctx) {
    }

    @Override
    public void exitStatementOpenLoop(japyParser.StatementOpenLoopContext ctx) {
    }

    @Override
    public void enterStatementWrite(japyParser.StatementWriteContext ctx) {

    }

    @Override
    public void exitStatementWrite(japyParser.StatementWriteContext ctx) {

    }

    @Override
    public void enterStatementAssignment(japyParser.StatementAssignmentContext ctx) {

    }

    @Override
    public void exitStatementAssignment(japyParser.StatementAssignmentContext ctx) {

    }

    @Override
    public void enterStatementInc(japyParser.StatementIncContext ctx) {

    }

    @Override
    public void exitStatementInc(japyParser.StatementIncContext ctx) {

    }

    @Override
    public void enterStatementDec(japyParser.StatementDecContext ctx) {

    }

    @Override
    public void exitStatementDec(japyParser.StatementDecContext ctx) {

    }

    @Override
    public void enterExpression(japyParser.ExpressionContext ctx) {

    }

    @Override
    public void exitExpression(japyParser.ExpressionContext ctx) {

    }

    @Override
    public void enterExpressionOr(japyParser.ExpressionOrContext ctx) {

    }

    @Override
    public void exitExpressionOr(japyParser.ExpressionOrContext ctx) {

    }

    @Override
    public void enterExpressionOrTemp(japyParser.ExpressionOrTempContext ctx) {

    }

    @Override
    public void exitExpressionOrTemp(japyParser.ExpressionOrTempContext ctx) {

    }

    @Override
    public void enterExpressionAnd(japyParser.ExpressionAndContext ctx) {

    }

    @Override
    public void exitExpressionAnd(japyParser.ExpressionAndContext ctx) {

    }

    @Override
    public void enterExpressionAndTemp(japyParser.ExpressionAndTempContext ctx) {

    }

    @Override
    public void exitExpressionAndTemp(japyParser.ExpressionAndTempContext ctx) {

    }

    @Override
    public void enterExpressionEq(japyParser.ExpressionEqContext ctx) {

    }

    @Override
    public void exitExpressionEq(japyParser.ExpressionEqContext ctx) {

    }

    @Override
    public void enterExpressionEqTemp(japyParser.ExpressionEqTempContext ctx) {

    }

    @Override
    public void exitExpressionEqTemp(japyParser.ExpressionEqTempContext ctx) {

    }

    @Override
    public void enterExpressionCmp(japyParser.ExpressionCmpContext ctx) {

    }

    @Override
    public void exitExpressionCmp(japyParser.ExpressionCmpContext ctx) {

    }

    @Override
    public void enterExpressionCmpTemp(japyParser.ExpressionCmpTempContext ctx) {

    }

    @Override
    public void exitExpressionCmpTemp(japyParser.ExpressionCmpTempContext ctx) {

    }

    @Override
    public void enterExpressionAdd(japyParser.ExpressionAddContext ctx) {

    }

    @Override
    public void exitExpressionAdd(japyParser.ExpressionAddContext ctx) {

    }

    @Override
    public void enterExpressionAddTemp(japyParser.ExpressionAddTempContext ctx) {

    }

    @Override
    public void exitExpressionAddTemp(japyParser.ExpressionAddTempContext ctx) {

    }

    @Override
    public void enterExpressionMultMod(japyParser.ExpressionMultModContext ctx) {

    }

    @Override
    public void exitExpressionMultMod(japyParser.ExpressionMultModContext ctx) {

    }

    @Override
    public void enterExpressionMultModTemp(japyParser.ExpressionMultModTempContext ctx) {

    }

    @Override
    public void exitExpressionMultModTemp(japyParser.ExpressionMultModTempContext ctx) {

    }

    @Override
    public void enterExpressionUnary(japyParser.ExpressionUnaryContext ctx) {

    }

    @Override
    public void exitExpressionUnary(japyParser.ExpressionUnaryContext ctx) {

    }

    @Override
    public void enterExpressionMethods(japyParser.ExpressionMethodsContext ctx) {

    }

    @Override
    public void exitExpressionMethods(japyParser.ExpressionMethodsContext ctx) {

    }

    @Override
    public void enterExpressionMethodsTemp(japyParser.ExpressionMethodsTempContext ctx) {

    }

    @Override
    public void exitExpressionMethodsTemp(japyParser.ExpressionMethodsTempContext ctx) {

    }

    @Override
    public void enterExpressionOther(japyParser.ExpressionOtherContext ctx) {

    }

    @Override
    public void exitExpressionOther(japyParser.ExpressionOtherContext ctx) {

    }

    @Override
    public void enterJapyType(japyParser.JapyTypeContext ctx) {
    }

    @Override
    public void exitJapyType(japyParser.JapyTypeContext ctx) {
    }

    @Override
    public void enterSingleType(japyParser.SingleTypeContext ctx) {

    }

    @Override
    public void exitSingleType(japyParser.SingleTypeContext ctx) {

    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
    }

    @Override
    public void visitTerminal(TerminalNode node) {
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
    }

    private static class SymbolInfo {
        String kind;
        String name;
        String type;
        String accessModifier;
        String inherits;
        List<SymbolTable.ParameterInfo> parameters;
        int firstAppearance;

        SymbolInfo(String kind, String name) {
            this.kind = kind;
            this.name = name;
        }

        SymbolInfo(String kind, String name, String type) {
            this(kind, name);
            this.type = type;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{kind='").append(kind).append('\'');
            sb.append(", name='").append(name).append('\'');
            if (type != null) {
                sb.append(", type='").append(type).append('\'');
            }
            if (accessModifier != null) {
                sb.append(", accessModifier='").append(accessModifier).append('\'');
            }
            if (inherits != null) {
                sb.append(", inherits='").append(inherits).append('\'');
            }
            if (parameters != null && !parameters.isEmpty()) {
                sb.append(", parameters=").append(parameters);
            }
            sb.append('}');
            return sb.toString();
        }
    }

    private static class ParameterInfo {
        int index;
        String name;
        String type;

        ParameterInfo(int index, String name, String type) {
            this.index = index;
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return "{index=" + index + ", name='" + name + '\'' + ", type='" + type + '\'' + '}';
        }
    }
}

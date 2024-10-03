import java.io.IOException;
import java.io.PrintStream;

/**
 * @Author: Shaan Jassal
 * Implements a syntax analyser for a language using a recursive descent parser
 * Extends AbstractSyntaxAnalyser
 */

public class SyntaxAnalyser extends AbstractSyntaxAnalyser {
    private String fileName;

    /** 
     * Attempts to create a new LexicalAnalyser using the provided fileName
     * 
     * @param fileName The name of the file to be analysed
     * @throws RuntimeException If unable to load the lexical analyzer
     */
    public SyntaxAnalyser(String fileName) {
        this.fileName = fileName;
        try {
            lex = new LexicalAnalyser(fileName);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load lexical analyzer", e);
        }
    }

    /** 
     * Generates an error message using the expected token and the actual token found
     * @param expected The expected token description
     * @param actual The actual token found
     * @return The error message
     */
    public String generateErrorMsg(String expected, Token actual) {
        return "Syntax error at line " + actual.lineNumber + " in " + this.fileName +
               ": Expected " + expected + " but found '" + actual.text + "'.";
    }

    /**
     * Accepts a terminal symbol if it matches with the current
     * Advances to the next token
     *
     * @param symbol The expected terminal symbol
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    @Override
    public void acceptTerminal(int symbol) throws IOException, CompilationException {
        if (nextToken.symbol == symbol) {
            myGenerate.insertTerminal(nextToken);
            nextToken = lex.getNextToken();
        } else {
            myGenerate.reportError(nextToken, generateErrorMsg("'" + Token.getName(symbol) + "'", nextToken));
        }
    }

    /**
     * Parses and processes the statement part
     * <statement part> ::= begin <statement list> end
     * 
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    @Override
    public void _statementPart_() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<statement part>");
    
        try {
            acceptTerminal(Token.beginSymbol); 
            statementList();
            acceptTerminal(Token.endSymbol);
    
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<statement list> between 'begin' and 'end'", nextToken), e);
        } finally {
            myGenerate.finishNonterminal("<statement part>");
        }
    }


    /**
     * Parses and processes the statement list
     * <statement list> ::= <statement> | <statement list> ; <statement>

     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    public void statementList() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<statement list>");

        try {
            statement();

            // Iterates through statements separated by semicolons
            while (nextToken.symbol == Token.semicolonSymbol) {
                acceptTerminal(Token.semicolonSymbol);
                statementList(); // recursion
            }
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<statement list>", nextToken), e);
        } finally {
            myGenerate.finishNonterminal("<statement list>");
        }
    }

    /**
     * Parses and processes a statement
     * <assignment statement> | <if statement> | <while statement> | <procedure statement> |
     * <until statement> | <for statement>
     * 
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    private void statement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<statement>");
        try {
            switch (nextToken.symbol) {
                case Token.identifier:
                    assignmentStatement();
                    break;
                case Token.ifSymbol:
                    ifStatement();
                    break;
                case Token.whileSymbol:
                    whileStatement();
                    break;
                case Token.callSymbol:
                    procedureStatement();
                    break;
                case Token.untilSymbol:
                    untilStatement();
                    break;
                case Token.forSymbol:
                    forStatement();
                    break;                
            }
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg(" <assignment>, <if>, <while>, <procedure>, <until>, or <for>", nextToken), e);
        } finally {
            myGenerate.finishNonterminal("<statement>");
        }
    }

    /**
     * Parses and processes an assignment statement
     * <assignment statement> ::= identifier := <expression> | identifier := stringConstant

     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    public void assignmentStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<assignment statement>");
        acceptTerminal(Token.identifier);
        acceptTerminal(Token.becomesSymbol); // accepts assignment symbol

        if (nextToken.symbol == Token.stringConstant) {
            acceptTerminal(Token.stringConstant);
        }
        else {
            try {
                expression(); // parses the expression
            } catch (CompilationException e) {
                throw new CompilationException(generateErrorMsg("<expression>", nextToken), e);
            }   
        }
        myGenerate.finishNonterminal("<assignment statement>");
    }

    /**
     * Parses and processes an if statement
     * <if statement> ::= if <condition> then <statement list> end if |
     * if <condition> then <statement list> else <statement list> end if

     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    public void ifStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<if statement>");
        acceptTerminal(Token.ifSymbol);
    
        try {
            condition(); 
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<condition>", nextToken), e);
        }
    
        acceptTerminal(Token.thenSymbol);
        try {
            statementList(); // statement list for the 'if' branch
            
            // checks for and parses the 'else' branch
            if (nextToken.symbol == Token.elseSymbol) {
                acceptTerminal(Token.elseSymbol);
                statementList();
            }
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<statement list>", nextToken), e);
        }
    
        acceptTerminal(Token.endSymbol);
        acceptTerminal(Token.ifSymbol);
    
        myGenerate.finishNonterminal("<if statement>");
    }
    
    /**
     * Parses and processes a while statement
     * <while statement> ::= while <condition> loop <statement list> end loop
     * 
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    private void whileStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<while statement>");
        acceptTerminal(Token.whileSymbol);
        try {
            condition();
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<condition>", nextToken), e);
        }
        acceptTerminal(Token.loopSymbol);

        try {
            statementList();
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<statement list>", nextToken), e);
        }

        acceptTerminal(Token.endSymbol);
        acceptTerminal(Token.loopSymbol);

        myGenerate.finishNonterminal("<while statement>");
    }

    /**
     * Parses and processes a procedure statement
     * <procedure statement> ::= call identifier ( <argument list> )
     * 
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    private void procedureStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<procedure statement>");
        acceptTerminal(Token.callSymbol);
        acceptTerminal(Token.identifier);
        
        acceptTerminal(Token.leftParenthesis);
        try {
            argumentList();
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<argument list>", nextToken), e);
        }
        acceptTerminal(Token.rightParenthesis);
        myGenerate.finishNonterminal("<procedure statement>");
    }

    /**
     * Parses and processes an until statement
     * <until statement> ::= do <statement list> until <condition>
     * 
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    private void untilStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<until statement>");

        acceptTerminal(Token.doSymbol);

        try {
            statementList();
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<statement list>", nextToken), e);
        }
        acceptTerminal(Token.untilSymbol);

        try {
            condition();
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<condition>", nextToken), e);
        }
        myGenerate.finishNonterminal("until statement>");
    }

    /**
     * Parses and processes a for statement
     * <for statement> ::= for ( <assignment statement> ; <condition> ; <assignment
     * statement> ) do <statement list> end loop

     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    private void forStatement() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<for statement>");
        acceptTerminal(Token.forSymbol);
        acceptTerminal(Token.leftParenthesis);
    
        try {
            assignmentStatement();
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<assignment statement>", nextToken), e);
        }
        acceptTerminal(Token.semicolonSymbol);
    
        try {
            condition();
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<condition>", nextToken), e);
        }
    
        acceptTerminal(Token.semicolonSymbol);
    
        try {
            assignmentStatement();
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<assignment statement>", nextToken), e);
        }
    
        acceptTerminal(Token.rightParenthesis);
        acceptTerminal(Token.doSymbol);
    
        try {
            statementList();
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<statement list>", nextToken), e);
        }
    
        acceptTerminal(Token.endSymbol);
        acceptTerminal(Token.loopSymbol);
        myGenerate.finishNonterminal("<for statement>");
    }

    /**
     * Parses and processes the argument list
     * <argument list> ::= identifier |
     * <argument list> , identifier

     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    private void argumentList() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<argument list>");
    
        try {
            acceptTerminal(Token.identifier);
            
            // handles multiple occurrences of ','
            while (nextToken.symbol == Token.commaSymbol) {
                acceptTerminal(Token.commaSymbol);
                argumentList(); // recursion
            }
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<argument list>", nextToken), e);
        } finally {
            myGenerate.finishNonterminal("<argument list>");
        }
    }

    /**
     * Parses and processes a condition statement
     * <condition> ::= identifier <conditional operator> identifier |
     * identifier <conditional operator> numberConstant |
     * identifier <conditional operator> stringConstant
 
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    private void condition() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<condition>");
    
        try {
            acceptTerminal(Token.identifier);
    
            conditionalOperator();
    
            switch (nextToken.symbol) {
                case Token.identifier:
                    acceptTerminal(Token.identifier);
                    break;
                case Token.numberConstant:
                    acceptTerminal(Token.numberConstant);
                    break;
                case Token.stringConstant:
                    acceptTerminal(Token.stringConstant);
                    break;
            }
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<condition>", nextToken), e);
        } finally {
            myGenerate.finishNonterminal("<condition>");
        }
    }

    /**
     * Parses and processes a conditional operator statement
     * <conditional operator> ::= > | >= | = | /= | < | <=
     * 
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    public void conditionalOperator() throws IOException, CompilationException{
        myGenerate.commenceNonterminal("<conditional operator>");

        switch(nextToken.symbol){
            case Token.greaterThanSymbol:
                acceptTerminal(Token.greaterThanSymbol);
                break;
            case Token.greaterEqualSymbol:
                acceptTerminal(Token.greaterEqualSymbol);
                break;
            case Token.equalSymbol:
                acceptTerminal(Token.equalSymbol);
                break;
            case Token.notEqualSymbol:
                acceptTerminal(Token.notEqualSymbol);
                break;
            case Token.lessThanSymbol:
                acceptTerminal(Token.lessThanSymbol);
                break;
            case Token.lessEqualSymbol:
                acceptTerminal(Token.lessEqualSymbol);
                break;
            default:
                myGenerate.reportError(nextToken, generateErrorMsg(" > , >= , = , /= , < or <= ", nextToken));
        }
        myGenerate.finishNonterminal("<conditional operator>");
    }

    /**
     * Checks if the given token represents a plus or minus symbol
     *
     * @param token The token to be checked
     * @return True if the token is a plus or minus symbol, false otherwise
     */
    public boolean plusOrMinus(Token token) {
        int symbol = token.symbol;
        return symbol == Token.plusSymbol || symbol == Token.minusSymbol;
    }

    /**
     * Parses and processes an expression statement
     * <expression> ::= <term> | <expression> + <term> | <expression> - <term>
     * 
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    private void expression() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("expression");

        try {
            term();
			// Checks for additional terms with plus or minus operators
            while (plusOrMinus(nextToken)) {
                acceptTerminal(nextToken.symbol);
                term();
            }

        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("term", nextToken), e);
        } finally {
            myGenerate.finishNonterminal("expression");
        }
    }

    /**
     * Checks if the given token represents multiplication or division
     *
     * @param token The token to check
     * @return True if the token symbol is either multiplication or div symbol, false otherwise.
     */
    private boolean multiplyOrDivide(Token token) {
        int symbol = token.symbol;
        return symbol == Token.divideSymbol || symbol == Token.timesSymbol;
    }


    /**
     * Parses and processes a term statement
     * <term> ::= <factor> | <term> * <factor> | <term> / <factor>
     * 
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    private void term() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<term>");

        try {
            factor();
			
            while (multiplyOrDivide(nextToken)) {
                acceptTerminal(nextToken.symbol);
                term(); // recursion
            }
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("<term>", nextToken), e);
        }
        myGenerate.finishNonterminal("<term>");
    }

    /**
     * Parses and processes a factor statement
     * <factor> ::= identifier | numberConstant | ( <expression> )
     * 
     * @throws IOException If an I/O error occurs
     * @throws CompilationException If a compilation error occurs
     */
    private void factor() throws IOException, CompilationException {
        myGenerate.commenceNonterminal("<factor>");
    
        try {
            switch (nextToken.symbol) {
                case Token.identifier:
                    acceptTerminal(Token.identifier);
                    break;
                case Token.numberConstant:
                    acceptTerminal(Token.numberConstant);
                    break;
                case Token.leftParenthesis:
                    acceptTerminal(Token.leftParenthesis);
                    expression();
                    acceptTerminal(Token.rightParenthesis);
                    break;
                default:
                    myGenerate.reportError(nextToken, generateErrorMsg("identifier, number constant, '(' or ')", nextToken));
                    break;
            }
        } catch (CompilationException e) {
            throw new CompilationException(generateErrorMsg("identifier, number constant, or <expression>", nextToken), e);
        } finally {
            myGenerate.finishNonterminal("<factor>");
        }
    }
}
/**
 * @author Shaan Jassal
 * The Generate class extends AbstractGenerate and provides an implementation for reporting errors during parsing
 */
public class Generate extends AbstractGenerate {
    private String stackTrace;

    public Generate() {}

    public Generate(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    /**
     * Reports an error during parsing using the given token and explanatory message
     *
     * @param token The token where the error occurred.
     * @param explanatoryMessage A message providing information about the error.
     * @throws CompilationException Thrown to indicate a parsing error.
     */
    @Override
    public void reportError(Token token, String explanatoryMessage) throws CompilationException {
        throw new CompilationException(explanatoryMessage);

        //CompilationException cause = new CompilationException(explanatoryMessage);
        //throw new CompilationException(stackTrace, cause);
    }
}
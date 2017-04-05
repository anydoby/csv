package assignment.csv;

/**
 * This class contains utilities usually imported from guava or apache commons, but for the sake of the demo are
 * included here to reduce the number of dependencies.
 * 
 * @author sergey
 *
 */
public class Utils
{

    /**
     * @param arg the argument to check for <code>null</code>
     * @param message error message
     * @param messageParams optional message parameters
     * @return the value if it is not <code>null</code> or throws an {@link IllegalArgumentException}
     * @throws IllegalArgumentException if a value is <code>null</code>
     */
    public static <Argument> Argument checkNotNull(Argument arg, String message, Object... messageParams)
            throws IllegalArgumentException
    {
        if (arg == null)
        {
            throw new IllegalArgumentException(String.format(message, messageParams));
        }
        return arg;
    }

    /**
     * Re-throws checked exception as if it were unchecked. The profit is that the exception does not have to be caught
     * by the caller which makes code more readable and also the "thrower" does not incur excessive stack frame decoding
     * (faster throws).
     * <p>
     * 
     * @see "http://www.eishay.com/2011/11/throw-undeclared-checked-exception-in.html"
     * 
     * 
     * @param e
     * @return does not happen
     */
    public static RuntimeException unchecked(final Throwable e)
    {
        throwAny(e);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(final Throwable e) throws E
    {
        throw (E) e;
    }

}

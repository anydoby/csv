package assignment.csv;

import static org.junit.Assert.assertSame;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests utils.
 * 
 * @author sergey
 *
 */
public class UtilsTest
{

    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Test
    public void testCheckNotNull() throws Exception
    {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Message: param");

        Utils.checkNotNull(null, "Message: %s", "param");
    }

    @Test
    public void testCheckNotNull_Happy() throws Exception
    {
        Object arg = new Object();
        Object object = Utils.checkNotNull(arg, "Message: %s", "param");
        assertSame(arg, object);
    }

}

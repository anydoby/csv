package assignment.csv;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author sergey
 *
 */
public class SomeCoolCsvReaderTest
{

    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Test
    public void testSomeCoolCsvReader_NullFile() throws Exception
    {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Input file cannot be null");
        new SomeCoolCsvReader(null, null);
    }

    @Test
    public void testSomeCoolCsvReader_NullClass() throws Exception
    {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("POJO class cannot be null. Please provide the model class.");
        new SomeCoolCsvReader(new File("dummy"), null);
    }

    @Test
    public void testSomeCoolCsvReader_NoMetaAnnotations() throws Exception
    {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Unable to find @FileMetaData in the hierarchy of class java.lang.String");
        new SomeCoolCsvReader(new File("dummy"), String.class);
    }

    @Test
    public void testSomeCoolCsvReader_EmptySeparator() throws Exception
    {
        ex.expect(IllegalArgumentException.class);

        @FileMetaData(separator = "")
        class NoSeparator
        {}
        ex.expectMessage(
                "The determined field delimiter (found in class assignment.csv.SomeCoolCsvReaderTest$1NoSeparator) is empty. Please use something sensible, like a comma.");
        new SomeCoolCsvReader(new File("dummy"), NoSeparator.class);
    }

}

package assignment.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import assignment.csv.SomeCoolCsvReader.CsvColumn;
import assignment.csv.SomeCoolCsvReader.NoOpColumn;

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
        ex.expectMessage(
                "The determined field delimiter (found in class assignment.csv.SomeCoolCsvReaderTest$1NoSeparator) is empty. Please use something sensible, like a comma.");

        @FileMetaData(separator = "")
        class NoSeparator
        {}
        new SomeCoolCsvReader(new File("dummy"), NoSeparator.class);
    }

    @Test
    public void testSomeCoolCsvReader_SeparatorTooLong() throws Exception
    {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Delimiter must be a single character");

        @FileMetaData(separator = ",,")
        class NoSeparator
        {}
        new SomeCoolCsvReader(new File("dummy"), NoSeparator.class);
    }

    @Test
    public void testSomeCoolCsvReader_DuplicateIndex() throws Exception
    {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage(
                "Fields [assignment.csv.SomeCoolCsvReaderTest$1DupIndex.s, assignment.csv.SomeCoolCsvReaderTest$1DupIndex.z] share the same index 1");

        @FileMetaData
        class DupIndex
        {

            @CSVColumn(indx = 1)
            String s;

            @CSVColumn(indx = 1)
            String z;

        }
        new SomeCoolCsvReader(new File("dummy"), DupIndex.class);
    }

    @Test
    public void testSomeCoolCsvReader_ZeroIndex() throws Exception
    {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage(
                "Fields [assignment.csv.SomeCoolCsvReaderTest$1ZeroIndex.s] have an index of 0, minimum value is 1");

        @FileMetaData
        class ZeroIndex
        {

            @CSVColumn(indx = 0)
            String s;

            @CSVColumn(indx = 1)
            String z;

        }
        new SomeCoolCsvReader(new File("dummy"), ZeroIndex.class);
    }

    @FileMetaData
    static class NotAllIndexesUsed
    {

        @CSVColumn(indx = 1)
        String s;

        @CSVColumn(indx = 3)
        String z;

    }

    /**
     * Checks that not all indexes (column positions) have to be specified if the user chooses not to de-serialize all
     * of the data. Checks that the generated accessors are able to modify the pojo that has accessible (package visible
     * to the {@link SomeCoolCsvReader}) fields without setters.
     * 
     * @throws Exception
     */
    @Test
    public void testSomeCoolCsvReader_SparseColumnsArray() throws Exception
    {
        NotAllIndexesUsed victim = new NotAllIndexesUsed();
        SomeCoolCsvReader reader = new SomeCoolCsvReader(new File("dummy"), NotAllIndexesUsed.class);

        assertTrue(reader.columns[0] instanceof CsvColumn);
        assertEquals(1, reader.columns[0].getIndex());
        reader.columns[0].set(victim, "firstField");
        assertEquals("firstField", victim.s);

        assertTrue(reader.columns[1] instanceof NoOpColumn);
        assertEquals(2, reader.columns[1].getIndex());

        assertTrue(reader.columns[2] instanceof CsvColumn);
        assertEquals(3, reader.columns[2].getIndex());
        reader.columns[2].set(victim, "secondField");
        assertEquals("secondField", victim.z);

        // a no-op column does not do much, so we only check that it did not change the target object's internal state
        reader.columns[1].set(victim, "someRubbish");
        assertEquals("firstField", victim.s);
        assertEquals("secondField", victim.z);
    }

    @FileMetaData
    static class PublicSetters
    {

        @CSVColumn(indx = 1)
        private String s;
        private boolean setterWasCalled;

        public String getS()
        {
            return s;
        }

        public void setS(String s)
        {
            setterWasCalled = true;
            this.s = s;
        }

    }

    @Test
    public void testPublicSetters()
    {

        PublicSetters victim = new PublicSetters();
        SomeCoolCsvReader reader = new SomeCoolCsvReader(new File("dummy"), PublicSetters.class);

        reader.columns[0].set(victim, "someCoolValue");
        assertEquals("someCoolValue", victim.getS());
        assertTrue("A setter must have been called.", victim.setterWasCalled);

        // check that nulls are fine as arguments
        reader.columns[0].set(victim, null);
        assertNull(victim.getS());
    }

    @FileMetaData
    private static class PrivateSetters
    {

        @CSVColumn(indx = 1)
        private String s;
        private boolean setterWasCalled;

        private String getS()
        {
            return s;
        }

        private void setS(String s)
        {
            setterWasCalled = true;
            this.s = s;
        }

    }

    @Test
    public void testPrivateClassWithSetters()
    {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage(
                "Unable to find a public setter method for the annotated field private java.lang.String assignment.csv.SomeCoolCsvReaderTest$PrivateSetters.s");
        new SomeCoolCsvReader(new File("dummy"), PrivateSetters.class);
    }

    @Test
    public void testClassWithNoDefaultConstructor()
    {
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage(
                "Unable to find a no-args public constructor for class assignment.csv.SomeCoolCsvReaderTest$1NoDefaultConstructor");

        @FileMetaData
        class NoDefaultConstructor
        {

            @CSVColumn(indx = 1)
            String s;

        }
        new SomeCoolCsvReader(new File("dummy"), NoDefaultConstructor.class);
    }

    @FileMetaData
    public static class Person
    {

        @CSVColumn(indx = 1)
        private String firstName;

        @CSVColumn(indx = 3)
        private String secondName;

        public String getFirstName()
        {
            return firstName;
        }

        public void setFirstName(String firstName)
        {
            this.firstName = firstName;
        }

        public String getSecondName()
        {
            return secondName;
        }

        public void setSecondName(String secondName)
        {
            this.secondName = secondName;
        }

    }

    @Test
    public void testNewPojo() throws Exception
    {
        SomeCoolCsvReader<Person> reader =
                reader("John, jr, Doe , student\nJane, sr, Smith, student\n\n", Person.class);

        List<Person> objects = reader.getObjects();

        assertEquals(2, objects.size());
        {
            Person person = objects.get(0);
            assertNotNull(person);
            assertEquals("John", person.getFirstName());
            assertEquals("Doe", person.getSecondName());
        }
        {
            Person person = objects.get(1);
            assertNotNull(person);
            assertEquals("Jane", person.getFirstName());
            assertEquals("Smith", person.getSecondName());
        }
    }

    @Test
    public void testNotAllColumnsPresent() throws IOException
    {
        SomeCoolCsvReader<Person> reader = reader("John, jr\r\n\nJane, sr\n\n", Person.class);

        List<Person> objects = reader.getObjects();

        assertEquals(2, objects.size());
        {
            Person person = objects.get(0);
            assertNotNull(person);
            assertEquals("John", person.getFirstName());
            assertNull(person.getSecondName());
        }
        {
            Person person = objects.get(1);
            assertNotNull(person);
            assertEquals("Jane", person.getFirstName());
            assertNull(person.getSecondName());
        }

    }

    @Test
    public void testNotSquareMatrix() throws IOException
    {
        SomeCoolCsvReader<Person> reader = reader("John, jr\r\n\nJane, ,Smith,,hello\n\n", Person.class);

        List<Person> objects = reader.getObjects();

        assertEquals(2, objects.size());
        {
            Person person = objects.get(0);
            assertNotNull(person);
            assertEquals("John", person.getFirstName());
            assertNull(person.getSecondName());
        }
        {
            Person person = objects.get(1);
            assertNotNull(person);
            assertEquals("Jane", person.getFirstName());
            assertEquals("Smith", person.getSecondName());
        }

    }

    @Test
    public void testEmptyFile() throws IOException
    {
        SomeCoolCsvReader<Person> reader = reader("", Person.class);
        List<Person> objects = reader.getObjects();
        assertTrue(objects.isEmpty());
    }

    /*
     * creates test files on the fly using the specified contents and returs a parser
     */
    private <T> SomeCoolCsvReader<T> reader(String contents, Class<T> pojo) throws IOException
    {
        File temp = File.createTempFile("test", ".csv");
        temp.deleteOnExit();
        Files.write(temp.toPath(), contents.getBytes(), StandardOpenOption.CREATE);

        return new SomeCoolCsvReader<>(temp, pojo);
    }

}

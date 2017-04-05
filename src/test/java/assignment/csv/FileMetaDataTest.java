package assignment.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Ensures runtime resolution of {@link FileMetaData} and {@link CSVColumn} works
 * 
 * @author sergey
 *
 */
public class FileMetaDataTest
{

    @FileMetaData
    private class TestPojo
    {

        @CSVColumn(indx = 1)
        private String prop1;

    }

    private class TestPojoSubclass extends TestPojo
    {

        @CSVColumn(indx = 2)
        private String prop2;

    }

    @Test
    public void testRetention() throws NoSuchFieldException, SecurityException
    {
        FileMetaData annotation = TestPojo.class.getAnnotation(FileMetaData.class);
        assertNotNull(annotation);
        assertEquals(",", annotation.separator());

        CSVColumn csvColumn = TestPojo.class.getDeclaredField("prop1").getAnnotation(CSVColumn.class);
        assertNotNull(csvColumn);
        assertEquals(1, csvColumn.indx());
    }

    @Test
    public void testSubclassRetention() throws NoSuchFieldException, SecurityException
    {
        FileMetaData annotation = TestPojoSubclass.class.getAnnotation(FileMetaData.class);
        assertNotNull(annotation);
        {
            CSVColumn csvColumn = TestPojoSubclass.class.getDeclaredField("prop2").getAnnotation(CSVColumn.class);
            assertNotNull(csvColumn);
            assertEquals(2, csvColumn.indx());
        }
        {
            CSVColumn csvColumn =
                    TestPojoSubclass.class.getSuperclass().getDeclaredField("prop1").getAnnotation(CSVColumn.class);
            assertNotNull(csvColumn);
            assertEquals(1, csvColumn.indx());
        }
    }

}

package assignment.csv;

import static assignment.csv.Utils.checkNotNull;
import static assignment.csv.Utils.unchecked;
import static java.lang.String.format;
import static java.util.Arrays.stream;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * This reader is capable of reading CSV, TSV and other delimited files as long as the class used for initialization
 * conforms to the Javabeans standards (has a no-args constructor and has public get/set mutators). The initialization
 * class must be annotated with the {@link FileMetaData} and the readable fields must be annotated with the
 * {@link CSVColumn} each.
 * 
 * <p>
 * <b>Note that instances of this class are not thread safe.</b>
 * 
 * @author sergey
 * @param <Pojo> the type of objects produced by the instance of this reader
 *
 */
public class SomeCoolCsvReader<Pojo>
{

    private File file;
    private String delimiter;

    /**
     * Creates new instance of the reader.
     * 
     * @param file the input file with delimited data
     * @param pojoClass the class used to initialize the reader
     */
    public SomeCoolCsvReader(File file, Class<Pojo> pojoClass)
    {
        this.file = checkNotNull(file, "Input file cannot be null");
        checkNotNull(pojoClass, "POJO class cannot be null. Please provide the model class.");
        String delimiter = determineDelimiter(pojoClass);
        this.delimiter =
                checkNotNull(delimiter, "Unable to find @FileMetaData in the hierarchy of %s", pojoClass).trim();
        if (delimiter.isEmpty())
        {
            throw new IllegalArgumentException(
                    format("The determined field delimiter (found in %s) is empty. Please use something sensible, like a comma.",
                            pojoClass));
        }

        findReadableFields(pojoClass);
    }

    private void findReadableFields(Class<Pojo> pojoClass)
    {
        stream(pojoClass.getDeclaredFields()).filter(f -> f.getAnnotation(CSVColumn.class) != null).map(Column::new);
    }

    /**
     * Represents a column in the file. Instances of this class can be used to set properties of the model object. One
     * column can set one property.
     * 
     * @author sergey
     *
     */
    static class Column<Pojo> implements Comparable<Column>
    {

        int index;
        private MethodHandle setter;

        Column(Field field)
        {
            Class< ? > declaringClass = field.getDeclaringClass();
            try
            {
                setter = MethodHandles.lookup().findSetter(declaringClass, field.getName(), String.class);
            }
            catch (NoSuchFieldException | IllegalAccessException e)
            {
                throw new IllegalArgumentException("Unable to find a setter method for the annotated field " + field,
                        e);
            }
        }

        @Override
        public int compareTo(Column o)
        {
            return Integer.compare(index, o.index);
        }

        public void set(Pojo target, String value)
        {
            try
            {
                setter.invoke(target, value);
            }
            catch (Throwable e)
            {
                // calling a humble setter should not really get us into trouble, but in any case let's just re-throw it
                // to the caller.
                unchecked(e);
            }
        }

    }

    private String determineDelimiter(Class< ? > pojoClass)
    {
        if (pojoClass == null)
        {
            return null;
        }
        FileMetaData fileMetaData = pojoClass.getAnnotation(FileMetaData.class);
        if (fileMetaData == null)
        {
            return determineDelimiter(pojoClass.getSuperclass());
        }
        return fileMetaData.separator();
    }

}

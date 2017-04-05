package assignment.csv;

import static assignment.csv.Utils.checkNotNull;
import static assignment.csv.Utils.unchecked;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This reader is capable of reading CSV, TSV and other delimited files as long as the class used for initialization
 * conforms to the Javabeans standards (has a no-args constructor and has public get/set mutators or if mutators are not
 * present the annotated fields are public). The initialization class (the POJO) must be annotated with the
 * {@link FileMetaData} and the readable fields must be annotated with the {@link CSVColumn} each. Inherited fields are
 * also supported. Beware that no two fields with the same index are allowed, indexes less than 1 are prohibited.
 * 
 * <p>
 * <b>Note that instances of this class are thread safe. A distinct iterator is created every time {@link #iterator()}
 * is called.</b>
 * 
 * @param <Pojo> the type of objects produced by the instance of this reader
 *
 * @author sergey
 */
public class SomeCoolCsvReader<Pojo> implements Iterable<Pojo>
{

    static final Logger logger = LoggerFactory.getLogger(SomeCoolCsvReader.class);

    private final File file;
    private final char delimiter;
    final Column[] columns;

    private MethodHandle constructor;

    /**
     * Creates new instance of the reader.
     * 
     * @param file the input file with delimited data
     * @param pojoClass the class used to initialize the reader
     * @throws IllegalArgumentException in case when either parameter is <code>null</code>, delimiter could not be
     *             determined or delimiter is an empty string or a string containing more than one character, any
     *             annotated field does not have a setter method or there are multiple fields sharing the same index in
     *             the data line, or there is a field with index of zero (0).
     */
    public SomeCoolCsvReader(File file, Class<Pojo> pojoClass)
    {
        this.file = checkNotNull(file, "Input file cannot be null");
        checkNotNull(pojoClass, "POJO class cannot be null. Please provide the model class.");

        Optional<String> delimiter = determineDelimiter(pojoClass);
        String delimiterString = delimiter.orElseThrow(() -> new IllegalArgumentException(
                format("Unable to find @FileMetaData in the hierarchy of %s", pojoClass))).trim();
        if (delimiterString.isEmpty())
        {
            throw new IllegalArgumentException(
                    format("The determined field delimiter (found in %s) is empty. Please use something sensible, like a comma.",
                            pojoClass));
        }
        if (delimiterString.length() > 1)
        {
            throw new IllegalArgumentException("Delimiter must be a single character");
        }
        this.delimiter = delimiterString.charAt(0);
        this.columns = createCsvColumns(pojoClass);

        try
        {
            // find the default constructor
            constructor = MethodHandles.lookup().findConstructor(pojoClass, MethodType.methodType(void.class));
        }
        catch (NoSuchMethodException | IllegalAccessException e)
        {
            logger.error("Unable to find a no-args public constructor.", e);
            throw new IllegalArgumentException(format("Unable to find a no-args public constructor for %s", pojoClass));
        }
    }

    private static Column[] createCsvColumns(Class< ? > pojoClass)
    {
        Stream<CsvColumn> fields = findReadableFields(pojoClass);
        Map<Integer, List<CsvColumn>> possibleDuplicates = fields.collect(Collectors.groupingBy(CsvColumn::getIndex));
        if (possibleDuplicates.containsKey(0))
        {
            throw new IllegalArgumentException(
                    format("Fields %s have an index of 0, minimum value is 1", possibleDuplicates.get(0)));
        }
        int maxIndex = Integer.MIN_VALUE;
        for (Entry<Integer, List<CsvColumn>> entry : possibleDuplicates.entrySet())
        {
            if (entry.getValue().size() > 1)
            {
                throw new IllegalArgumentException(
                        format("Fields %s share the same index %d", entry.getValue(), entry.getKey()));
            }
            maxIndex = max(maxIndex, entry.getKey());
        }
        Column[] columns = new Column[maxIndex];
        for (int i = 0; i < columns.length; i++ )
        {
            List<CsvColumn> list = possibleDuplicates.get(i + 1);
            if (list == null)
            {
                // this column in the csv input will be skipped
                columns[i] = new NoOpColumn(i + 1);
            }
            else
            {
                columns[i] = list.get(0);
            }
        }
        return columns;
    }

    /**
     * Returns a list of objects parsed from the file provided in the constructor. Note that {@link SomeCoolCsvReader}
     * is also usable as an {@link Iterable} if the number of POJOs is too large to fit in memory.
     * 
     * @return a list with the file contents transformed to the objects of the desired type
     */
    public List<Pojo> getObjects()
    {
        List<Pojo> result = new ArrayList<>();
        iterator().forEachRemaining(result::add);
        return result;
    }

    private static Stream<CsvColumn> findReadableFields(Class< ? > pojoClass)
    {
        Field[] fields = pojoClass.getDeclaredFields();
        // ignore fields without annotations
        Stream<CsvColumn> s = stream(fields).filter(f -> f.getAnnotation(CSVColumn.class) != null).map(CsvColumn::new);
        if (pojoClass.getSuperclass() != null)
        {
            // look for readable fields in the superclass
            return concat(s, findReadableFields(pojoClass.getSuperclass()));
        }
        return s;
    }

    /**
     * Represents a column in the file. Instances of this class can be used to set properties of the model object. One
     * column can set one property.
     * 
     * @author sergey
     *
     */
    interface Column
    {

        /**
         * Returns the column index
         * 
         * @return the index
         */
        int getIndex();

        /**
         * Sets the value of a column to a pojo
         * 
         * @param target the target pojo
         * @param value the value to set
         */
        default void set(Object target, String value)
        {}

    }

    /**
     * A stub for field whose index is not defined in a pojo, for cases when you may want to only use fields 1 and 3,
     * but skip field 2, for example. This column will not set any values.
     * 
     * @author sergey
     *
     */
    static class NoOpColumn implements Column
    {

        private int index;

        public NoOpColumn(int index)
        {
            this.index = index;
        }

        @Override
        public int getIndex()
        {
            return index;
        }

    }

    /**
     * The real setter, it will set the value of a csv field to the POJO using the setter method designated for the
     * field.
     * 
     * @author sergey
     *
     */
    static class CsvColumn extends NoOpColumn
    {

        private MethodHandle setter;

        private Field field;

        private CsvColumn(Field field)
        {
            super(field.getAnnotation(CSVColumn.class).indx());
            this.field = field;
            Class< ? > declaringClass = field.getDeclaringClass();

            Method method = setterMethodFor(field);
            try
            {
                /*
                 * This is by far the fastest way of mutating an object's value, in essence it is the same as calling
                 * the setter or assigning a member field directly in the code. I could have used the javassist or cglib
                 * proxy here, however since java 8 it is not necessary for simple assignments and will perform better.
                 */
                if (method != null)
                {
                    setter = MethodHandles.lookup().unreflect(method);
                }
                else
                {
                    setter = MethodHandles.lookup().findSetter(declaringClass, field.getName(), String.class);
                }
            }
            catch (NoSuchFieldException | IllegalAccessException e)
            {
                logger.error("Unable to find neither a public setter nor public field", e);
                throw new IllegalArgumentException(
                        "Unable to find a public setter method for the annotated field " + field, e);
            }
        }

        /*
         * Determines the public setter method for a field. Returns null if no accessor is found
         */
        private Method setterMethodFor(Field field)
        {
            char[] array = field.getName().toCharArray();
            array[0] = Character.toUpperCase(array[0]);
            String methodName = new StringBuilder().append("set").append(array).toString();
            try
            {
                return field.getDeclaringClass().getMethod(methodName, String.class);
            }
            catch (NoSuchMethodException | SecurityException e)
            {
                logger.debug("Unable to find public setter method for field {}", field);
            }
            return null;
        }

        public void set(Object target, String value)
        {
            try
            {
                setter.invoke(target, value);
            }
            catch (Throwable e)
            {
                logger.error("Calling a setter resulted in an error.", e);
                // calling a humble setter should not really get us into trouble, but in any case let's just re-throw it
                // to the caller.
                unchecked(e);
            }
        }

        @Override
        public String toString()
        {
            return field.getDeclaringClass().getTypeName() + "." + field.getName();
        }

    }

    /*
     * recursively search for the FileMetaData annotation until something is found
     */
    private static Optional<String> determineDelimiter(Class< ? > pojoClass)
    {
        FileMetaData fileMetaData = pojoClass.getAnnotation(FileMetaData.class);
        if (fileMetaData == null)
        {
            if (pojoClass.getSuperclass() != null)
            {
                return determineDelimiter(pojoClass.getSuperclass());
            }
            else
            {
                return Optional.empty();
            }
        }
        return Optional.of(fileMetaData.separator());
    }

    Pojo newPojo(String line)
    {
        Pojo result;
        try
        {
            result = (Pojo) constructor.invoke();
        }
        catch (Throwable e)
        {
            // I cannot imagine this happening other than as OutOfMemory error
            logger.error("Unable to instantiate object", e);
            throw unchecked(e);
        }
        /*
         * I chose not to re-implement the excellent https://commons.apache.org/proper/commons-csv/ and for this demo I
         * do not support escaping of delimiters within value. For a production use I would definitely implement a
         * proper lexer or better yet write a grammar with antlr and generate the lexer/parser.
         */
        int tokenIndex = 0;
        int lastTokenStart = 0;
        for (int i = 0; i < line.length(); i++ )
        {
            char charAt = line.charAt(i);
            if (isDelimiter(charAt))
            {
                emitValue(tokenIndex++ , lastTokenStart, i, line, result);
                lastTokenStart = i + 1;
            }
        }
        emitValue(tokenIndex, lastTokenStart, line.length(), line, result);

        return result;
    }

    private void emitValue(int column, int valueOffset, int valueEndOffset, String line, Pojo result)
    {
        if (column < columns.length)
        {
            String value = line.substring(valueOffset, valueEndOffset).trim();
            if (value.isEmpty())
            {
                /*
                 * this is not specified in the assignment and normally we would have some magic string defining a null
                 * value
                 */
                value = null;
            }
            columns[column].set(result, value);
        }
        else
        {
            logger.debug("Column {} is not defined for class {}", column + 1, result.getClass());
        }
    }

    private boolean isDelimiter(char charAt)
    {
        return delimiter == charAt;
    }

    @Override
    public Iterator<Pojo> iterator()
    {
        try
        {
            /*
             * The default charset is not ideal, but the task does not require anything sophisticated I hope, in real
             * life this would be configurable of course.
             * 
             * Also I assume that the client will read the stream to the end otherwise the file handle will be left open
             * forever. In a real life scenario if I have to let resources leak (through an iterator with an open db
             * connection, for example) I subclass the java.lang.ref.PhantomReference and place the resource to be
             * closed in it, register the reference with the java.lang.ref.ReferenceQueue; and upon entering
             * inaccessible state I close the resource. For the sake of the demo I guess this should suffice.
             */
            return Files.lines(file.toPath(), defaultCharset()).filter(l -> !l.trim().isEmpty()).map(
                    this::newPojo).iterator();
        }
        catch (IOException e)
        {
            /*
             * Although the client will probably handle the exception its good to log it anyway since if it is not
             * handled, at least we'll have something in the log
             */
            logger.error("An IO error occurred while reading data from the file.", e);
            throw unchecked(e);
        }
    }

}

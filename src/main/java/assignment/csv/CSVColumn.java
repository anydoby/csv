package assignment.csv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The column definition. Mark your POJOs' fields with this annotation in order to specify the position of your field in
 * the delimited file. Positions are 1 based (like in BASIC).
 * 
 * @author sergey
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface CSVColumn
{

    /**
     * Returns the index of the property within the string of delimited data. 1 based.
     * 
     * @return column index
     */
    int indx();

}

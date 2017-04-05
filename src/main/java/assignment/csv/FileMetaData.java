package assignment.csv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark your POJOs with this annotation to define the token separator. Default is comma.
 * 
 * @author sergey
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface FileMetaData
{

    /**
     * The separator character. Default is <code>,</code>
     * 
     * @return the separator
     */
    String separator() default ",";

}

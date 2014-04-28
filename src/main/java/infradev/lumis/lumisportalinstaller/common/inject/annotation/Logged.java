package infradev.lumis.lumisportalinstaller.common.inject.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark what will be logged.
 * 
 * @author Alexandre Ribeiro de Souza
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Logged {

}

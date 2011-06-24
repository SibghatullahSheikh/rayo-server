package com.tropo.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy=FileFormatValidator.class)
public @interface ValidFileFormat {

	String message() default Messages.INVALID_FILE_FORMAT;
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
}

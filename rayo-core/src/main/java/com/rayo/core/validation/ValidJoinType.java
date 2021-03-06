package com.rayo.core.validation;

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
@Constraint(validatedBy=JoinTypeValidator.class)
public @interface ValidJoinType {

	String message() default Messages.INVALID_JOIN_TYPE;
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
}

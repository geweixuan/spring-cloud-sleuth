package org.springframework.cloud.sleuth.instrument.web;

import java.util.concurrent.Callable;

import lombok.extern.apachecommons.CommonsLog;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.cloud.sleuth.Trace;
import org.springframework.cloud.sleuth.TraceContextHolder;
import org.springframework.cloud.sleuth.instrument.TraceCallable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestOperations;

/**
 * Aspect that adds correlation id to
 * <p/>
 * <ul>
 * <li>{@link RestController} annotated classes with public {@link Callable} methods</li>
 * <li>{@link Controller} annotated classes with public {@link Callable} methods</li>
 * </ul>
 * <p/>
 * For controllers an around aspect is created that wraps the {@link Callable#call()}
 * method execution in {@link TraceCallable}
 * <p/>
 *
 * @see RestController
 * @see Controller
 * @see RestOperations
 * @see TraceCallable
 * @see Trace
 *
 * @author Tomasz Nurkewicz, 4financeIT
 * @author Marcin Grzejszczak, 4financeIT
 * @author Michal Chmielarz, 4financeIT
 * @author Spencer Gibb
 */
@Aspect
@CommonsLog
public class TraceWebAspect {

	private final Trace trace;

	public TraceWebAspect(Trace trace) {
		this.trace = trace;
	}

	@Pointcut("@target(org.springframework.web.bind.annotation.RestController)")
	private void anyRestControllerAnnotated() {
	}

	@Pointcut("@target(org.springframework.stereotype.Controller)")
	private void anyControllerAnnotated() {
	}

	@Pointcut("execution(public java.util.concurrent.Callable *(..))")
	private void anyPublicMethodReturningCallable() {
	}

	@Pointcut("(anyRestControllerAnnotated() || anyControllerAnnotated()) && anyPublicMethodReturningCallable()")
	private void anyControllerOrRestControllerWithPublicAsyncMethod() {
	}

	@Around("anyControllerOrRestControllerWithPublicAsyncMethod()")
	@SuppressWarnings("unchecked")
	public Object wrapWithCorrelationId(ProceedingJoinPoint pjp) throws Throwable {
		Callable<Object> callable = (Callable<Object>) pjp.proceed();
		if (TraceContextHolder.isTracing()) {
			log.debug("Wrapping callable with span ["
					+ TraceContextHolder.getCurrentSpan() + "]");

			return new TraceCallable<Object>(this.trace, callable);
		}
		else {
			return callable;
		}
	}

}

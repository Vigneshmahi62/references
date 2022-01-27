package com.precision.mdm.data.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Vignesh
 *
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

	/**
	 * 
	 * @param joinPoint {@link ProceedingJoinPoint}
	 * @throws Throwable if any error occurs when executing the
	 *                   {@link ProceedingJoinPoint}
	 */
	@Around("Pointcuts.timerRequested()")
	public Object timerStart(final ProceedingJoinPoint joinPoint) throws Throwable {
		long start = System.currentTimeMillis();
		Object obj = joinPoint.proceed();
		long stop = System.currentTimeMillis();
		long timeTaken = (stop - start) / 1000;
		log.info("Time Taken by the method " + joinPoint.getSignature().getName() + " is "
				+ timeTaken + " Seconds");
		return obj;
	}

}

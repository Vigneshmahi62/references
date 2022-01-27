package com.precision.mdm.data.aspects;

import org.aspectj.lang.annotation.Pointcut;

/**
 * This class is for all public/global Pointcuts used in aspects
 * 
 * @author Vignesh
 *
 */
public abstract class Pointcuts {

	@Pointcut("getMappingMethods() || postMappingMethods() || putMappingMethods() || deleteMappingMethods()")
	public void restRequestMethods() {
	}

	@Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
	private void getMappingMethods() {
	}

	@Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
	private void postMappingMethods() {
	}

	@Pointcut("@annotation(org.springframework.web.bind.annotation.PutMapping)")
	private void putMappingMethods() {
	}

	@Pointcut("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
	private void deleteMappingMethods() {
	}

	@Pointcut("@annotation(com.precision.mdm.data.annotations.Timer)")
	public void timerRequested() {
	}
}

/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */ 

package org.objectstyle.cayenne.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;


/**
 * Invocation represents a dynamic method invocation bound to a specific target.
 * The target is kept with a WeakReference and can therefore be reclaimed by the
 * Garbage Collector.
 * 
 * @author Holger Hoffstätte
 * @author Dirk Olmes
 */
public class Invocation extends Object {
	private static final Logger log = Logger.getLogger(Invocation.class);
 
	private WeakReference _target;
	private Method _method;
	private Class[] _parameterTypes;

	/**
	 * Prevent use of empty default constructor
	 */
	private Invocation() {
	}

	/**
	 * Constructor for an Invocation without arguments in the target's method.
	 * 
	 * @see #Invocation(Object, String, Class[])
	 */
	public Invocation(Object target, String methodName)
		throws NoSuchMethodException {
		this(target, methodName, (Class[])null);
	}

	/**
	 * Constructor for an Invocation with a single argument in the target's
	 * method.
	 * 
	 * @see #Invocation(Object, String, Class[])
	 */
	public Invocation(Object target, String methodName, Class parameterType)
		throws NoSuchMethodException {
		this(target, methodName, new Class[]{parameterType});
	}

	/**
	 * Constructor for an Invocation with arbitrary arguments in the target's
	 * method.
	 * 
	 * @param target
	 * @param methodName
	 * @param parameterTypes
	 * @throws NoSuchMethodException if <code>methodName</code> could not be
	 * found in the target
	 * @throws IllegalArgumentException if target or methodName are
	 * <code>null</code>, or parameterTypes is empty or contains
	 * <code>null</code> elements
	 */
	public Invocation(Object target, String methodName, Class[] parameterTypes)
		throws NoSuchMethodException {
		super();

		if (target == null) {
			throw new IllegalArgumentException("target argument must not be null");
		}

		if (methodName == null) {
			throw new IllegalArgumentException("method name must not be null");
		}	

		if (parameterTypes != null) {
			if (parameterTypes.length > 0) {
				for (int i = 0; i < parameterTypes.length; i++) {
					if (parameterTypes[i] == null) {
						throw new IllegalArgumentException("parameter type[" + i + "] must not be null");
					}
				}
			}
			else {
				throw new IllegalArgumentException("parameter types must not be empty");
			}
		}

		_method = target.getClass().getMethod(methodName, parameterTypes);
		_parameterTypes = parameterTypes;
		_target = new WeakReference(target);
	}

	/**
	 * Invoke the target's method without any arguments.
	 * 
	 * @see #fire(Object[])
	 */
	public boolean fire() {
		return this.fire((Object[])null);
	}

	/**
	 * Invoke the target's method with a single argument.
	 * 
	 * @param argument an object passed to the target's method
	 * @see #fire(Object[])
	 */
	public boolean fire(Object argument) {
		return this.fire(new Object[]{argument});
	}

	/**
	 * Invoke the target's method with an arbitrary number of arguments.
	 * The number of arguments must be consistent with the arguments given at
	 * construction time of this Invocation.
	 * 
	 * @param arguments an array of objects passed to the target's method
	 * @return <code>true</code> if invocation of the method succeeded,
	 * otherwise <code>false</code>
	 * @throws IllegalArgumentException if the passed arguments are inconsistent
	 * with the arguments passed to this instance's constructor
	 * @see #fire(Object[])
	 */
	public boolean fire(Object[] arguments) {
		boolean success = false;

		if (_parameterTypes == null) {
			if (arguments != null) {
				throw new IllegalArgumentException("arguments unexpectedly != null");
			}
		}
		else {
			if (arguments == null) {
				throw new IllegalArgumentException("arguments must not be null");
			}
			else {
				if (_parameterTypes.length != arguments.length) {
					throw new IllegalArgumentException("inconsistent number of arguments: expected"
														+ _parameterTypes.length
														+ ", got "
														+ arguments.length);
				}
			}
		}

		Object currentTarget = _target.get();

		if (currentTarget != null) {
			try {	
				_method.invoke(currentTarget, arguments);
				success = true;
			}
			catch (Exception ex) {
				log.error("exception while firing '" + _method.getName() + "'", ex);
			}
		}

		return success;
	}

	/**
	 * @see Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if ((obj != null) && (obj.getClass().equals(this.getClass()))) {
			Invocation otherInvocation = (Invocation)obj;
			if (_method.equals(otherInvocation.getMethod())) {
				Object otherTarget = otherInvocation.getTarget();
				Object target = _target.get();

				if ((target == null) && (otherTarget == null)) {
					return true;
				}

				if ((target == null) && (otherTarget != null)) {
					return false;
				}

				if (target != null) {
					return target.equals(otherTarget);
				}
			}

			return false;
		}
		else {
			return super.equals(obj);
		}
	}

	/**
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		int hash = 42, hashMultiplier = 59;
		hash = hash * hashMultiplier + _method.hashCode();

		if (_target.get() != null) {
			hash = hash * hashMultiplier + _target.get().hashCode();
		}

		return hash;
	}

	/**
	 * @return the method to be invoked on the target
	 */
	public Method getMethod() {
		return _method;
	}

	/**
	 * @return the target object of this Invocation 
	 */
	public Object getTarget() {
		return _target.get();
	}

	/**
	 * @return an array of Classes describing the target method's parameters
	 */
	public Class[] getParameterTypes() {
		return _parameterTypes;
	}

}

/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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
package org.objectstyle.cayenne.validation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Contains a set of static methods for the most common validation tasks.
 * 
 * @author Fabricio Voznika
 * @since 1.1
 */
public class Validator {

    public static boolean checkNotNull(
        Object bean,
        Object value,
        String attribute,
        ValidationResult result) {
        if (value == null) {
            result.addFailure(
                bean,
                attribute,
                createMessage(attribute, " is a required field."));
            return false;
        }
        return true;
    }

    public static boolean checkNotEmpty(
        Object bean,
        String value,
        String attribute,
        ValidationResult result) {
        if (value == null || value.length() == 0) {
            result.addFailure(
                bean,
                attribute,
                createMessage(attribute, " is a required field."));
            return false;
        }
        return true;
    }

    public static boolean checkNotEmpty(
        Object bean,
        Collection value,
        String attribute,
        ValidationResult result) {
        if (value == null) {
            result.addFailure(
                bean,
                attribute,
                createMessage(attribute, " is a required field."));
            return false;
        }
        if (value.isEmpty()) {
            result.addFailure(
                bean,
                attribute,
                createMessage(attribute, " cannot be empty."));
            return false;
        }
        return true;
    }

    public static boolean checkMandatory(
        Object bean,
        String attribute,
        ValidationResult result) {
        return checkMandatory(bean, beanGet(bean, attribute), attribute, result);
    }

    public static boolean checkMandatory(
        Object bean,
        Object value,
        String attribute,
        ValidationResult result) {
        if (value instanceof String) {
            return Validator.checkNotEmpty(bean, (String) value, attribute, result);
        }
        if (value instanceof Collection) {
            return Validator.checkNotEmpty(bean, (Collection) value, attribute, result);
        }
        return Validator.checkNotNull(bean, value, attribute, result);
    }


    public static String createMessage(String attribute, String message) {
        StringBuffer buffer = new StringBuffer(message.length() + attribute.length() + 5);
        buffer.append(toNiceAttributeName(attribute, ' ', false));
        buffer.append(message);
        return buffer.toString();
    }

    private static String toNiceAttributeName(
        String attributeName,
        char separator,
        boolean caseChange) {
        StringBuffer ret = new StringBuffer(attributeName.length() + 10);
        ret.append(capitalizeFirstLetter(attributeName));
        for (int i = ret.length() - 1;
            i >= 1;
            i--) { // Don't want to add separator before the first char.
            char cur = ret.charAt(i);
            if (Character.isUpperCase(cur)) {
                if (caseChange) {
                    ret.setCharAt(i, Character.toLowerCase(cur));
                }
                ret.insert(i, separator);
            }
        }
        return ret.toString();
    }

    private static String capitalizeFirstLetter(String source) {
        if (source.length() == 0) {
            return source;
        }
        char c = source.charAt(0);
        if (Character.isUpperCase(c)) {
            return source;
        }
        char[] arr = source.toCharArray();
        arr[0] = Character.toUpperCase(c);
        return new String(arr);
    }

    private static Object beanGet(Object bean, String attribute) {
        attribute = capitalizeFirstLetter(attribute);
        try {
            Method m;
            Class clazz = bean.getClass();
            try {
                m = clazz.getMethod("get" + attribute, null);
            }
            catch (NoSuchMethodException e) {
                m = clazz.getMethod("is" + attribute, null);
            }
            return m.invoke(bean, null);

        }
        catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            }
            throw new RuntimeException(target);

        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);

        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
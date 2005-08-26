/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.map;

import java.lang.reflect.Method;

import org.objectstyle.cayenne.ValueHolder;

/**
 * A descriptor for a property with indirect access via a ValueHolder. Defines normal
 * setters and getters but also two additional optional accessors for the ValueHolder
 * object that stores property value.
 * <p>
 * ValueHolder property name is derived from a property name using a naming convention.
 * E.g. if an object that has a property <em>someProperty</em> and wants Cayenne to take
 * care of the ValueHolders initialization, it should implement accessors using the
 * following naming convention: <em>public void setSomePropertyHolder(ValueHolder)</em>
 * and <em>public ValueHolder getSomePropertyHolder()</em>.
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ValueHolderProperty extends Property {

    /**
     * A property name suffix for the property that is implemented with a ValueHolder.
     */
    public static final String HOLDER_NAME_SUFFIX = "Holder";

    protected Method valueHolderReadMethod;
    protected Method valueHolderWriteMethod;

    public ValueHolderProperty(String propertyName, Class beanClass) {
        this(propertyName, beanClass, null);
    }

    public ValueHolderProperty(String propertyName, Class beanClass, Class valueClass) {

        super(propertyName, beanClass, valueClass);

        String base = capitalize(propertyName + HOLDER_NAME_SUFFIX);
        valueHolderReadMethod = findGetter("get" + base, beanClass);
        valueHolderWriteMethod = findMatchingSetter(
                "set" + base,
                beanClass,
                ValueHolder.class);
    }

}

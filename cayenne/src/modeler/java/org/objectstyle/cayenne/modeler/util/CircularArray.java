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

package org.objectstyle.cayenne.modeler.util;
/**
 * A circular array is an array of fixed size as objects are added it will push objects off of the end to
 * allow space for new objects to be added.  This is useful for things like a fixed history size for a navigation tool.
 * 
 * @author Garry Watkins
 * @since 1.2
 */
public class CircularArray extends Object {
    private Object array[] = null;
    private int head = 0;
    private int tail = 0;
    private int count = 0;
    private int capacity = 0;
    
    /**
     * Creates an array of capacity size.
     * 
     * @param capacity - size of the new array
     */
    public CircularArray(int capacity){
        if (capacity <= 0){
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        array = new Object[capacity];
        this.capacity = capacity;
    }
    
    /**
     * Clears out the contents of the array.
     */
    public void clear(){
        array = new Object[capacity];
        head = 0;
        tail = 0;
        count = 0;
    }
    
    /**
     * Returns true if the array has no elements;
     */
    public boolean isEmpty(){
        return count == 0;
    }
    
    /**
     * Adds a new object to the array.  If the array is full it will push the oldest
     * item out of the array.
     * 
     * @param obj - the object to be added
     */
    public void add(Object obj){
        // we have wrapped and we have to move the head pointer
        if (count == capacity && tail == head){
            head = (head + 1) % capacity;
        }

        array[tail] = obj;

        tail = (tail + 1) % capacity;
        
        count++;
        if (count > capacity) count = capacity;
    }

    /**
     * Returns the number of objects stored in the array.
     */
    public int capacity(){
        return capacity;
    }
    
    /*
     * Converts the logical index into a physical index.
     */
    private int convert(int index){
        return (index + head) % capacity;
    }
    
    /*
     * Makes sure that the index is within range.
     */
    private void rangeCheck(int index) {
        if (index >= capacity || index < 0){
            throw new IndexOutOfBoundsException("Index: " + index+ ", Size: " + capacity);
        }
    }

    /**
     * Returns true if the array contains the specified object.
     * 
     * @param obj the object to be checked
     */
    public boolean contains(Object obj){
        return indexOf(obj) > 0;
    }
    
    /**
     * Gets the object at the specified index.
     * 
     * @param index the index of the object to be retrieved
     */
    public Object get(int index){
        rangeCheck(index);
        if (count == 0){
            return null;
        }
        return array[convert(index)];
    }
    
    /**
     * Returns the index of the specified object
     * 
     * @param obj the object that is being searched for
     */
    public int indexOf(Object obj){
        for (int i=0; i < capacity; i++){
            int index = convert(i);
            if (array[index] == obj){
                return i;
            }
        }
        // not found
        return -1;
    }

    /**
     * Resizes the array to the specified new size.  If the new capacity is smaller than
     * the current object count in the array, it will keep the newCapacity most recent objects.
     * 
     * @param newCapacity the new capacity of the array
     */
    public void resize(int newCapacity){
        int i = 0;
        int offset = 0;
        if (newCapacity < count){
            // making it smaller so we want to readjust the first object
            // to be copied into the new array
            i = count - newCapacity;
            offset = count - newCapacity;
        }
        Object newArray[] = new Object[newCapacity];
        for (; i<count; i++){
             newArray[i - offset] = array[convert(i)];
        }
        head = 0;
        tail = 0;
        capacity = newCapacity;
        
        // adjust the count if it is more than the new capacity
        if (capacity < count) count = capacity;
        array = newArray;   
    }
    
    /**
     * Returns the number of objects stored in the array.
     */
    public int size(){
        return count;
    }
    
    /**
     * Converts the array to an Object array.
     */
    public Object[] toArray(){
        Object o[] = new Object[count];
        for (int i=0; i<count; i++){
             o[i] = array[convert(i)];
        }
        return o;
    }
    
    public static void main(String[] args) {
        // The following are some unit tests for this class.  I don't know how to use JUnit, so I did these in a main class.
        // Maybe someday I will get some time to learn JUnit.
        CircularArray q = new CircularArray(3);
        
        String a = "A";
        String b = "B";
        String c = "C";
        String d = "D";
        String e = "E";
        
        q.add(a);
        int i = q.indexOf(a);
        if (i != 0){
            System.out.println("indexOf(a) should be zero instead got [" + String.valueOf(i) + "]");
        }
        String s = (String) q.get(0);
        if (s != a){
            System.out.println("get(0) should be 'a' instead got [" + s + "]");
        }
        i = q.size();
        if (i != 1){
            System.out.println("size() should be 1 instead got [" + String.valueOf(i) + "]");
        }
        
        q.add(b);
        i = q.indexOf(b);
        if (i != 1){
            System.out.println("indexOf(b) should be 1 instead got [" + String.valueOf(i) + "]");
        }
        s = (String) q.get(0);
        if (s != a){
            System.out.println("get(0) should be 'a' instead got [" + s + "]");
        }
        s = (String) q.get(1);
        if (s != b){
            System.out.println("get(1) should be 'b' instead got [" + s + "]");
        }

        i = q.size();
        if (i != 2){
            System.out.println("size() should be 2 instead got [" + String.valueOf(i) + "]");
        }
        
        q.add(c);
        i = q.indexOf(c);
        if (i != 2){
            System.out.println("indexOf(c) should be 2 instead got [" + String.valueOf(i) + "]");
        }
        s = (String) q.get(0);
        if (s != a){
            System.out.println("get(0) should be 'a' instead got [" + s + "]");
        }
        s = (String) q.get(1);
        if (s != b){
            System.out.println("get(1) should be 'b' instead got [" + s + "]");
        }
        s = (String) q.get(2);
        if (s != c){
            System.out.println("get(1) should be 'c' instead got [" + s + "]");
        }
        
        i = q.size();
        if (i != 3){
            System.out.println("size() should be 3 instead got [" + String.valueOf(i) + "]");
        }
        
        q.add(d);
        i = q.size();
        if (i != 3){
            System.out.println("size() should be 3 instead got [" + String.valueOf(i) + "]");
        }
                
        q.add(e);
        i = q.size();
        if (i != 3){
            System.out.println("size() should be 3 instead got [" + String.valueOf(i) + "]");
        }
        
        if (q.contains(a)){
            System.out.println("A should not be in the q");
        }
        
        i = q.indexOf(c);
        if (i != 0){
            System.out.println("indexOf(c) should be zero instead got [" + String.valueOf(i) + "]");
        }
        s = (String) q.get(0);
        if (s != c){
            System.out.println("get(0) should be 'c' instead got [" + s + "]");
        }
        
        i = q.indexOf(d);
        if (i != 1){
            System.out.println("indexOf(d) should be 1 instead got [" + String.valueOf(i) + "]");
        }
        s = (String) q.get(1);
        if (s != d){
            System.out.println("get(1) should be 'd' instead got [" + s + "]");
        }
        
        i = q.indexOf(e);
        if (i != 2){
            System.out.println("indexOf(e) should be 2 instead got [" + String.valueOf(i) + "]");
        }
        s = (String) q.get(2);
        if (s != e){
            System.out.println("get(2) should be 'e' instead got [" + s + "]");
        }
        
        q.resize(5);
        i = q.capacity();
        if (i != 5){
            System.out.println("size() should be 5 after resizing to 5 instead got [" + String.valueOf(i) + "]");
        }

        // should be the same after resizing
        i = q.size();
        if (i != 3){
            System.out.println("size() should be 3 instead got [" + String.valueOf(i) + "]");
        }

        i = q.indexOf(e);
        if (i != 2){
            System.out.println("indexOf(e) should be 2 instead got [" + String.valueOf(i) + "]");
        }
        s = (String) q.get(2);
        if (s != e){
            System.out.println("get(2) should be 'e' instead got [" + s + "]");
        }
        
        q.resize(2);
        i = q.capacity();
        if (i != 2){
            System.out.println("size() should be 2 after resizing to 2 instead got [" + String.valueOf(i) + "]");
        }
    }
}

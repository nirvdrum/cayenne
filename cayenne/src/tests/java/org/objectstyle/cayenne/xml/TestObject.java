package org.objectstyle.cayenne.xml;

import java.util.*;

/**
 * @author Andrei Adamchik
 */
public class TestObject implements XMLSerializable {

    protected String name = "";
    protected int age;
    protected boolean open;
    protected List children = new ArrayList();

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
    
    
    public void addChild(String child) {
        children.add(child);
    }
    
    public void removeChild(String child) {
        children.remove(child);
    }
    
    public void setChildren(List children) {
        this.children = children;
    }
    
    public List getChildren() {
        return children;
        //return Collections.unmodifiableList(children);
    }
    
    public boolean equals(Object o) {
        if (null == o || !(o instanceof TestObject)) {
            return false;
        }
        
        TestObject test = (TestObject) o;
        
        return ((test.getAge() == age) && (name.equals(test.getName())) && (test.isOpen() == open));
    }

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.setRoot("Test", this.getClass().getName());
        encoder.encodeProperty("name", name);
        encoder.encodeProperty("age", new Integer(age));
        encoder.encodeProperty("open", new Boolean(open));
        encoder.encodeProperty("children", children);
    }

    public void decodeFromXML(XMLDecoder decoder) {
        
        if (null != decoder.decodeInteger("age")) {
            age = decoder.decodeInteger("age").intValue();
        }
        
        if (null != decoder.decodeBoolean("open")) {
            open = decoder.decodeBoolean("open").booleanValue();
        }
        
        name = decoder.decodeString("name");
        children = (List) decoder.decodeObject("children");
    }
}
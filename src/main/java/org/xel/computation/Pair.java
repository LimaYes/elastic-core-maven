package org.xel.computation;

public class Pair<K, V> {

    private K element0;
    private V element1;

    public Pair(K element0, V element1) {
        this.element0 = element0;
        this.element1 = element1;
    }

    public void setElement0(K element0) {
        this.element0 = element0;
    }

    public void setElement1(V element1) {
        this.element1 = element1;
    }

    @Override
    public boolean equals(Object obj)
    {

        // checking if both the object references are
        // referring to the same object.
        if(this == obj)
            return true;

        // it checks if the argument is of the
        // type Geek by comparing the classes
        // of the passed argument and this object.
        // if(!(obj instanceof Geek)) return false; ---> avoid.
        if(obj == null || obj.getClass()!= this.getClass())
            return false;

        // type casting of the argument.
        try {
            Pair<K, V> geek = (Pair<K, V>) obj;

            // comparing the state of argument with
            // the state of 'this' Object.
            return (geek.element0.equals(this.element0) && geek.element1.equals(this.element1));
        }catch(Exception e){
            return false;
        }
    }

    @Override
    public int hashCode()
    {

        // We are returning the Geek_id
        // as a hashcode value.
        // we can also return some
        // other calculated value or may
        // be memory address of the
        // Object on which it is invoked.
        // it depends on how you implement
        // hashCode() method.
        int code = this.element0.hashCode() ^ this.element1.hashCode();
        return code;
    }

    public K getElement0() {
        return element0;
    }

    public V getElement1() {
        return element1;
    }

}
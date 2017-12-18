package com.example.cjutzi.myservice;
public class UIModel{
    String name;
    int value; /* 0 -&gt; checkbox disable, 1 -&gt; checkbox enable */
    String distance;
    String type;

    UIModel(String name, String type, String distance, int value){
        this.name       = name;
        this.type       = type;
        this.distance   = distance;
        this.value      = value;
    }
    public int getValue()       { return this.value;    }
    public String getName()     { return this.name;   }
    public String getType()     { return this.type; }
    public String getDistance() { return this.distance; }

}
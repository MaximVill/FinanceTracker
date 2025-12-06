package org.example.financetracker.model;

public class Category {
    private Long id;
    private String name;
    private String type;

    // == ГЕТТЕРЫ ==
    public Long getId() {return id;}
    public String getName() {return name;}
    public String getType() {return type;}

    // == СЕТТЕРЫ ==
    public void setId(Long id) {this.id = id;}
    public void setName(String name) {this.name = name;}
    public void setType(String type) {this.type = type;}
}

package com.waitlist.dto;

import java.util.List;

public class KanbanColumnDTO {
    private Long id;
    private String name;
    private int orderIndex;
    private List<KanbanCardDTO> cards;

    private String primaryColor;
    private String secondaryColor;
    private String backgroundColor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public List<KanbanCardDTO> getCards() { return cards; }
    public void setCards(List<KanbanCardDTO> cards) { this.cards = cards; }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
}

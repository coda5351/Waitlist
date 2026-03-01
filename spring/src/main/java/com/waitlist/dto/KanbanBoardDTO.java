package com.waitlist.dto;

import java.util.List;

import java.time.LocalDateTime;

public class KanbanBoardDTO {
    private Long id;
    private Long accountId;
    private String name;
    private LocalDateTime createdAt;
    private List<KanbanColumnDTO> columns;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public List<KanbanColumnDTO> getColumns() { return columns; }
    public void setColumns(List<KanbanColumnDTO> columns) { this.columns = columns; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

package com.waitlist.dto;

import java.util.List;

public class CreateKanbanBoardRequest {
    private String name;
    private Long accountId;
    private List<CreateKanbanColumnRequest> columns;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public List<CreateKanbanColumnRequest> getColumns() { return columns; }
    public void setColumns(List<CreateKanbanColumnRequest> columns) { this.columns = columns; }
}

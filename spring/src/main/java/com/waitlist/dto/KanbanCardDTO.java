package com.waitlist.dto;

import java.time.LocalDateTime;

public class KanbanCardDTO {
    private Long id;
    private Long boardId;
    private Long columnId;
    private String name;
    private String description;
    private LocalDateTime creationDate;
    private LocalDateTime lastInteractionDate;
    private Integer cardOrder;
    private KanbanCardNoteDTO latestNote;
    private Integer notesCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBoardId() { return boardId; }
    public void setBoardId(Long boardId) { this.boardId = boardId; }
    public Long getColumnId() { return columnId; }
    public void setColumnId(Long columnId) { this.columnId = columnId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }
    public LocalDateTime getLastInteractionDate() { return lastInteractionDate; }
    public void setLastInteractionDate(LocalDateTime lastInteractionDate) { this.lastInteractionDate = lastInteractionDate; }
    public Integer getCardOrder() { return cardOrder; }
    public void setCardOrder(Integer cardOrder) { this.cardOrder = cardOrder; }
    public KanbanCardNoteDTO getLatestNote() { return latestNote; }
    public void setLatestNote(KanbanCardNoteDTO latestNote) { this.latestNote = latestNote; }
    public Integer getNotesCount() { return notesCount; }
    public void setNotesCount(Integer notesCount) { this.notesCount = notesCount; }
}

package com.waitlist.dto;

import java.time.LocalDateTime;

public class KanbanCardNoteDTO {
    private Long id;
    private Long cardId;
    private String content;
    private LocalDateTime createdAt;
    private boolean nextAction;
    private LocalDateTime nextActionConsumedAt;
    // The ID of the user who created the note (nullable)
    private Long createdById;
    private String authorName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public boolean isNextAction() { return nextAction; }
    public void setNextAction(boolean nextAction) { this.nextAction = nextAction; }

    public LocalDateTime getNextActionConsumedAt() { return nextActionConsumedAt; }
    public void setNextActionConsumedAt(LocalDateTime nextActionConsumedAt) { this.nextActionConsumedAt = nextActionConsumedAt; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

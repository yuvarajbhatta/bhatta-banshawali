package com.familytree.dto;

import com.familytree.entity.AnnouncementCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AdminAnnouncementRequestDto {

    @NotNull(message = "Category is required.")
    private AnnouncementCategory category;

    @NotBlank(message = "English title is required.")
    private String titleEn;

    private String titleNe;

    @NotBlank(message = "English body is required.")
    private String bodyEn;

    private String bodyNe;

    private boolean pinned;

    public AnnouncementCategory getCategory() {
        return category;
    }

    public void setCategory(AnnouncementCategory category) {
        this.category = category;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getTitleNe() {
        return titleNe;
    }

    public void setTitleNe(String titleNe) {
        this.titleNe = titleNe;
    }

    public String getBodyEn() {
        return bodyEn;
    }

    public void setBodyEn(String bodyEn) {
        this.bodyEn = bodyEn;
    }

    public String getBodyNe() {
        return bodyNe;
    }

    public void setBodyNe(String bodyNe) {
        this.bodyNe = bodyNe;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}

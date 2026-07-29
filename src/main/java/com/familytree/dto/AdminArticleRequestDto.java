package com.familytree.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AdminArticleRequestDto {

    @NotBlank(message = "Slug is required.")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase letters, numbers, and hyphens only.")
    private String slug;

    @NotBlank(message = "English title is required.")
    private String titleEn;

    private String titleNe;

    @NotBlank(message = "English body is required.")
    private String bodyEn;

    private String bodyNe;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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
}

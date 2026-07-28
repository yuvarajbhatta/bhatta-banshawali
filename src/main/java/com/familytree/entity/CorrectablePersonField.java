package com.familytree.entity;

/**
 * The Person fields a member is allowed to submit a correction for
 * (docs/08 Phase 4 correction-request workflow). An explicit allow-list,
 * not arbitrary field names, so PersonCorrectionService's apply step
 * can map each one to a real, type-checked Person setter rather than
 * using reflection.
 */
public enum CorrectablePersonField {
    FIRST_NAME,
    MIDDLE_NAME,
    LAST_NAME,
    FIRST_NAME_NEPALI,
    MIDDLE_NAME_NEPALI,
    LAST_NAME_NEPALI,
    NICKNAME,
    GENDER,
    BIRTH_DATE,
    DEATH_DATE,
    BIRTH_PLACE,
    CURRENT_ADDRESS,
    NOTES,
    GENERATION_NUMBER
}

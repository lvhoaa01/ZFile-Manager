package com.zfile.manager.model;

public enum SortCriteria {
    NAME_ASC,
    NAME_DESC,
    SIZE_ASC,
    SIZE_DESC,
    DATE_ASC,
    DATE_DESC,
    TYPE;

    public boolean isAscending() {
        return this == NAME_ASC || this == SIZE_ASC || this == DATE_ASC || this == TYPE;
    }
}

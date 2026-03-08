package com.intrigsoft.prathya.core.model;

import java.time.LocalDate;

public class ChangelogEntry {

    private String version;
    private LocalDate date;
    private String note;

    public ChangelogEntry() {}

    public ChangelogEntry(String version, LocalDate date, String note) {
        this.version = version;
        this.date = date;
        this.note = note;
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

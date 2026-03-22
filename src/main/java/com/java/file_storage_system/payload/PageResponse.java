package com.java.file_storage_system.payload;

public class PageResponse <T> {
    private T content;        // Data items
    private int page;                // Current page number (0-indexed)
    private int size;                // Items per page
    private long totalElements;      // Total number of items across all pages
    private int totalPages;          // Total number of pages
    private boolean first;           // Is this the first page?
    private boolean last;            // Is this the last page?
    private boolean empty;           // Is the content empty?

    public PageResponse() {
    }

    public PageResponse(T content, int page, int size, long totalElements, int totalPages, boolean first, boolean last, boolean empty) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
        this.empty = empty;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
}

package com.grammarlab.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing one state (item set) in the canonical LR(0) collection.
 * Maps directly to what ItemSetCardsView and AutomatonGraphView expect.
 */
public class LRItemSetDto {

    /** Numeric state identifier (0-based). */
    private int id;

    /** Display name (e.g., "I0", "I1"). */
    private String name;

    /** All LR(0) items in this state's closure. */
    private List<LRItemDto> items = new ArrayList<>();

    /** GOTO transitions from this state. */
    private List<LRGotoDto> gotos = new ArrayList<>();

    public LRItemSetDto() {}

    public LRItemSetDto(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<LRItemDto> getItems() { return items; }
    public void setItems(List<LRItemDto> items) { this.items = items; }

    public List<LRGotoDto> getGotos() { return gotos; }
    public void setGotos(List<LRGotoDto> gotos) { this.gotos = gotos; }
}

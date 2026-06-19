package com.dave.filestorage.unit;

import com.dave.filestorage.dto.PagedResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagedResultTest {

    @Test
    void totalPages_calculatedCorrectly() {
        PagedResult<String> result = new PagedResult<>(Collections.emptyList(), 0, 10, 25L);
        assertEquals(3, result.getTotalPages());
    }

    @Test
    void hasNext_trueWhenMorePages() {
        PagedResult<String> result = new PagedResult<>(Collections.emptyList(), 0, 10, 25L);
        assertTrue(result.isHasNext());
    }

    @Test
    void hasNext_falseOnLastPage() {
        PagedResult<String> result = new PagedResult<>(Collections.emptyList(), 2, 10, 25L);
        assertFalse(result.isHasNext());
    }

    @Test
    void contentIsPreserved() {
        List<String> items = Arrays.asList("a", "b", "c");
        PagedResult<String> result = new PagedResult<>(items, 0, 3, 3L);
        assertEquals(items, result.getContent());
    }

    @Test
    void zeroSize_zeroTotalPages() {
        PagedResult<String> result = new PagedResult<>(Collections.emptyList(), 0, 0, 0L);
        assertEquals(0, result.getTotalPages());
    }
}

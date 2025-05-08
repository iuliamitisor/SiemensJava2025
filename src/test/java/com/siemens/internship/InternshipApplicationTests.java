package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class InternshipApplicationTests {

	@Autowired
	private ItemService itemService;

	@Autowired
	private ItemRepository itemRepository;

	@BeforeEach
	void setUp() {
		itemRepository.deleteAll();
		itemService.resetProcessedCount();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void shouldSaveAndRetrieveItem() {
		Item item = new Item();
		item.setName("Test Item");
		item.setStatus("UNPROCESSED");

		Item savedItem = itemService.save(item);
		Optional<Item> retrievedItem = itemService.findById(savedItem.getId());

		assertTrue(retrievedItem.isPresent());
		assertEquals("Test Item", retrievedItem.get().getName());
		assertEquals("UNPROCESSED", retrievedItem.get().getStatus());
	}

	@Test
	void shouldFindAllItems() {
		for (int i = 0; i < 3; i++) {
			Item item = new Item();
			item.setName("Item " + i);
			item.setStatus("UNPROCESSED");
			itemService.save(item);
		}

		List<Item> allItems = itemService.findAll();

		assertEquals(3, allItems.size());
	}

	@Test
	void shouldDeleteItem() {
		Item item = new Item();
		item.setId(1L);
		item.setName("Item to delete");
		item.setStatus("UNPROCESSED");
		Item savedItem = itemService.save(item);

		itemService.deleteById(savedItem.getId());

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			itemService.findById(savedItem.getId());
		});

		assertEquals("Item " + savedItem.getId() + " not found", exception.getMessage());
	}

	@Test
	void shouldSuccessfullyProcessItems() throws Exception {
		for (int i = 0; i < 100; i++) {
			Item item = new Item();
			item.setName("Item " + i);
			item.setStatus("UNPROCESSED");
			itemService.save(item);

		}
		List<Item> result = itemService.processItemsAsync().get();

		assertEquals(100, result.size());
		for (Item item : result) {
			assertEquals("PROCESSED", item.getStatus());
		}
		assertEquals(100, itemService.getProcessedCount());
	}

	@Test
	void shouldThrowExceptionWhenItemNotFound() {
		Item item = new Item();
		item.setName("Item to delete");
		item.setStatus("UNPROCESSED");
		Item savedItem = itemService.save(item);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			itemService.findById(savedItem.getId() + 1);
		});

		assertEquals("Item " + (savedItem.getId() + 1) + " not found", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenDeletingNonexistentItem() {
		Item item = new Item();
		item.setName("Item to delete");
		item.setStatus("UNPROCESSED");
		Item savedItem = itemService.save(item);

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			itemService.deleteById(savedItem.getId() + 1);
		});

		assertEquals("Item " + (savedItem.getId() + 1) + " not found", exception.getMessage());
	}
}

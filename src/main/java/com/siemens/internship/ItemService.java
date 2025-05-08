package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: further ensure no synchronization issues occur

/*
    DONE:
     - Changed return type of processItemsAsync() to CompletableFuture<List<Item>> to allow asynchronous processing,
       since Spring cannot handle List<> asynchronously (the method ran on the main thread instead of
       a separate one like expected).
     - Kept processingCount, but as AtomicInteger to make it thread-safe.
     - Made use of CompletableFuture.supplyAsync instead of making multiple runAsync calls in a loop, in order
       to easily collect all items in a List<Item> after processing.
     - Removed manual ExecutorService and allowed usage of @Async task executors.
     - Added an IllegalStateException in findById() method to throw an exception if the item is not found.
     - Similarly added an IllegalStateException in deleteById() method to throw an exception if the item is not found before deletion.
     - Wrote integration tests to test application.
 */

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private AtomicInteger processedCount = new AtomicInteger(0);

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        Optional<Item> item = itemRepository.findById(id);
        if (item.isEmpty()) {
            throw new IllegalStateException("Item " + id + " not found");
        }
        return item;
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new IllegalStateException("Item " + id + " not found");
        }
        itemRepository.deleteById(id);
    }

    public int getProcessedCount() {
        return processedCount.get();
    }

    public void resetProcessedCount() {
        processedCount.set(0);
    }

    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        // Retrieve IDs from database
        // REMARK: though itemRepository is accessed by multiple threads, JpaRepository implementations are thread-safe.
        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Item>> futures = itemIds.stream()
                // Map each ID to a CompletableFuture
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(100); // Simulate processing
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Processing interrupted for item " + id, ie);
                    }

                    // Fetch item or throw exception if not found
                    Item item = itemRepository.findById(id)
                            .orElseThrow(() -> new IllegalStateException("Item " + id + " not found"));
                    // Update status of fetched item
                    item.setStatus("PROCESSED");
                    processedCount.addAndGet(1);

                    // Save updates
                    return itemRepository.save(item);
                }))
                .toList();

        // Merge all into one array of processed items
        CompletableFuture<Void> allDone = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]));

        // Return sequential stream to complete all or propagate exception
        return allDone.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join) // Wait for all futures to complete
                        .toList()
        );
    }
}

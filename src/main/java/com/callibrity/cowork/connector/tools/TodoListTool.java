package com.callibrity.cowork.connector.tools;

import com.callibrity.mocapi.tools.annotation.Tool;
import com.callibrity.mocapi.tools.annotation.ToolService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ToolService
public class TodoListTool {

    private final ConcurrentHashMap<Integer, TodoItem> todos = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @Tool(name = "todo.add", description = "Adds a new item to the to-do list and returns the assigned ID")
    public AddTodoResponse addTodo(
            @Schema(description = "The text of the to-do item to add") String text) {
        int id = nextId.getAndIncrement();
        todos.put(id, new TodoItem(id, text, false));
        return new AddTodoResponse(id, text);
    }

    @Tool(name = "todo.list", description = "Lists all to-do items, optionally filtered by completion status")
    public ListTodosResponse listTodos(
            @Schema(description = "Filter by status: 'all', 'pending', or 'completed'. Defaults to 'all'") String filter) {
        String normalizedFilter = filter == null || filter.isBlank() ? "all" : filter.strip().toLowerCase();
        List<TodoItem> results = todos.values().stream()
                .filter(item -> switch (normalizedFilter) {
                    case "pending" -> !item.completed();
                    case "completed" -> item.completed();
                    default -> true;
                })
                .sorted((a, b) -> Integer.compare(a.id(), b.id()))
                .toList();
        return new ListTodosResponse(results, results.size());
    }

    @Tool(name = "todo.complete", description = "Marks a to-do item as completed")
    public TodoOperationResponse completeTodo(
            @Schema(description = "The ID of the to-do item to mark as completed") int id) {
        TodoItem existing = todos.get(id);
        if (existing == null) {
            return new TodoOperationResponse(false, "No to-do item found with ID " + id);
        }
        todos.put(id, new TodoItem(id, existing.text(), true));
        return new TodoOperationResponse(true, "Marked item " + id + " as completed");
    }

    @Tool(name = "todo.delete", description = "Permanently removes a to-do item from the list")
    public TodoOperationResponse deleteTodo(
            @Schema(description = "The ID of the to-do item to delete") int id) {
        TodoItem removed = todos.remove(id);
        if (removed == null) {
            return new TodoOperationResponse(false, "No to-do item found with ID " + id);
        }
        return new TodoOperationResponse(true, "Deleted item " + id + ": " + removed.text());
    }

    public record TodoItem(
            @Schema(description = "Unique identifier for the to-do item") int id,
            @Schema(description = "Text of the to-do item") String text,
            @Schema(description = "Whether the item has been completed") boolean completed) {
    }

    public record AddTodoResponse(
            @Schema(description = "The ID assigned to the new to-do item") int id,
            @Schema(description = "The text of the added item") String text) {
    }

    public record ListTodosResponse(
            @Schema(description = "The matching to-do items") List<TodoItem> items,
            @Schema(description = "Total number of matching items") int count) {
    }

    public record TodoOperationResponse(
            @Schema(description = "Whether the operation succeeded") boolean success,
            @Schema(description = "Human-readable result message") String message) {
    }
}

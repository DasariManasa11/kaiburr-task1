package com.task;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService svc;

    public TaskController(TaskService svc) {
        this.svc = svc;
    }

    // -------------------------------
    // GET /tasks        → all tasks
    // GET /tasks?id=123 → single task
    // -------------------------------
    @GetMapping
    public ResponseEntity<?> getTasks(@RequestParam(value = "id", required = false) String id) {
        if (id == null) {
            return ResponseEntity.ok(svc.findAll());
        }
        return svc.findById(id)
                .<ResponseEntity<?>>map(task -> ResponseEntity.ok(task))
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("Task not found"));
    }

    // -------------------------------
    // GET /tasks/{id}
    // -------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return svc.findById(id)
                .<ResponseEntity<?>>map(task -> ResponseEntity.ok(task))
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("Task not found"));
    }

    // -------------------------------
    // PUT /tasks
    // Create or update a task
    // -------------------------------
    @PutMapping
    public ResponseEntity<?> putTask(@RequestBody Task task) {
        try {
            Task saved = svc.save(task);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // -------------------------------
    // DELETE /tasks/{id}
    // -------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable String id) {
        if (svc.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found");
        }
        svc.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------
    // GET /tasks/search?name=keyword
    // -------------------------------
    @GetMapping("/search")
    public ResponseEntity<?> searchByName(@RequestParam("name") String name) {
        var list = svc.findByName(name);
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No tasks found matching: " + name);
        }
        return ResponseEntity.ok(list);
    }

    // -------------------------------
    // PUT /tasks/{id}/executions
    // Execute a task command
    // -------------------------------
    @PutMapping("/{id}/executions")
    public ResponseEntity<?> runExecution(@PathVariable String id,
                                          @RequestParam(value = "podName", required = false) String podName) {
        var maybe = svc.findById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found");
        }

        Task task = maybe.get();
        try {
            System.out.println("Running task: " + task.getName() + ", command: " + task.getCommand() + ", pod: " + podName);
            TaskExecution exec = svc.executeInPod(task, podName);
            System.out.println("Execution result: " + exec.getOutput());
            return ResponseEntity.ok(exec);
        } catch (IllegalArgumentException | IllegalStateException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Execution failed: " + e.getMessage());
        }
    }
}

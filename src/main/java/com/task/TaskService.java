package com.task;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class TaskService {

    private final TaskRepository repo;

    public TaskService(TaskRepository repo) {
        this.repo = repo;
    }

    public List<Task> findAll() {
        return repo.findAll();
    }

    public Optional<Task> findById(String id) {
        return repo.findById(id);
    }

    public List<Task> findByName(String name) {
        return repo.findByNameContainingIgnoreCase(name);
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    public Task save(Task task) {
        if (!isCommandSafe(task.getCommand())) {
            throw new IllegalArgumentException("Unsafe command detected!");
        }
        return repo.save(task);
    }

    public TaskExecution executeInPod(Task task, String podName) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;

        if (os.contains("win")) {
            pb = new ProcessBuilder("cmd.exe", "/c", task.getCommand());
        } else {
            pb = new ProcessBuilder("bash", "-c", task.getCommand());
        }

        Process proc = pb.start();
        BufferedReader stdOut = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = stdOut.readLine()) != null) output.append(line).append("\n");
        while ((line = stdErr.readLine()) != null) output.append(line).append("\n");

        proc.waitFor();

        TaskExecution exec = new TaskExecution();
        exec.setStartTime(new Date());
        exec.setEndTime(new Date());
        exec.setOutput(output.toString().trim());

        task.getTaskExecutions().add(exec);
        repo.save(task);

        return exec;
    }




    private boolean isCommandSafe(String cmd) {
        if (cmd == null) return false;
        String lower = cmd.toLowerCase();
        // block dangerous commands
        return !(lower.contains("rm ") ||
                lower.contains("sudo") ||
                lower.contains("shutdown") ||
                lower.contains("reboot") ||
                lower.contains("mkfs") ||
                lower.contains("dd ") ||
                lower.contains(">") ||
                lower.contains("|") ||
                lower.contains("&") ||
                lower.contains(";"));
    }
}

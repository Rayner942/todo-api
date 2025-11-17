package com.example.todoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.validation.Valid; 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Classe principal da aplicação Spring Boot.
 * Contém o modelo (Task), o serviço (TaskService) e o controller (TaskController).
 */
@SpringBootApplication
public class ToDoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToDoApiApplication.class, args);
    }
}

/**
 * --------------------------------
 * 1. MODELO DE DADOS (POJO)
 * --------------------------------
 * Representa uma única Tarefa (Task).
 */
class Task {
    private String id;
    
    @NotBlank(message = "A descrição da tarefa é obrigatória e não pode ser vazia.")
 
    @Size(min = 5, max = 255, message = "A descrição deve ter entre 5 e 255 caracteres.")
    private String description;
    
    private boolean completed;



    public Task() {
        this.id = UUID.randomUUID().toString();
        this.completed = false;
    }

    // Construtor com descrição
    public Task(String description) {
        this(); // Chama o construtor padrão para gerar o ID
        this.description = description;
    }

    // Getters e Setters (Necessários para serialização JSON)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}

/**
 * --------------------------------
 * 2. CAMADA DE SERVIÇO / REPOSITÓRIO (SIMULAÇÃO DE DB)
 * --------------------------------
 * Gerencia a coleção de tarefas. Neste exemplo, usa um mapa em memória.
 */
@Service
class TaskService {
    // Usa um mapa thread-safe para armazenar tarefas em memória (simulando um DB)
    private final ConcurrentMap<String, Task> tasks = new ConcurrentHashMap<>();

    // Inicializa com algumas tarefas de exemplo
    public TaskService() {
        addTask(new Task("Aprender Spring Boot"));
        addTask(new Task("Implementar CRUD da API"));
        addTask(new Task("Documentar os Endpoints"));
   
    }


    /**
     * Retorna todas as tarefas.
     * @return Lista de todas as tarefas.
     */
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * Encontra uma tarefa pelo seu ID.
     * @param id O ID único da tarefa.
     * @return Um Optional contendo a tarefa, ou vazio se não encontrada.
     */
    public Optional<Task> findById(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    /**
     * Adiciona uma nova tarefa e gera um novo ID.
     * @param task A tarefa a ser adicionada (a ser salva).
     * @return A tarefa salva, incluindo o ID gerado.
     */
    public Task addTask(Task task) {
        // Garante que a tarefa tenha um novo ID (se não foi passado no POST)
        if (task.getId() == null) {
            task.setId(UUID.randomUUID().toString());
        }
        tasks.put(task.getId(), task);
        return task;
    }

    /**
     * Atualiza uma tarefa existente.
     * @param id O ID da tarefa a ser atualizada.
     * @param updatedTask Os novos dados da tarefa.
     * @return Um Optional contendo a tarefa atualizada, ou vazio se a tarefa não existir.
     */
    public Optional<Task> updateTask(String id, Task updatedTask) {
        if (tasks.containsKey(id)) {
            Task existingTask = tasks.get(id);
            
            // Atualiza apenas os campos que vieram no body da requisição
            existingTask.setDescription(updatedTask.getDescription());
            existingTask.setCompleted(updatedTask.isCompleted());
            
            tasks.put(id, existingTask); // Sobrescreve
            return Optional.of(existingTask);
        }
        return Optional.empty();
    }

    /**
     * Exclui uma tarefa pelo seu ID.
     * @param id O ID da tarefa a ser excluída.
     * @return true se a tarefa foi removida, false caso contrário.
     */
    public boolean deleteTask(String id) {
        return tasks.remove(id) != null;
    }
}

/**
 * --------------------------------
 * 3. CONTROLLER REST
 * --------------------------------
 * Lida com as requisições HTTP e mapeia para os métodos de serviço.
 * A API é acessível em /api/tasks.
 */
@RestController
@RequestMapping("/api/tasks")
class TaskController {

    private final TaskService taskService;

    // Injeção de Dependência (Spring injeta TaskService automaticamente)
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * GET: Recupera uma lista de todas as tarefas.
     * Mapeamento: GET /api/tasks
     * @return ResponseEntity com a lista de tarefas e status 200 (OK).
     */
    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.findAll();
    }

    /**
     * GET: Recupera uma tarefa específica pelo ID.
     * Mapeamento: GET /api/tasks/{id}
     * @param id O ID da tarefa.
     * @return ResponseEntity com a tarefa e status 200, ou 404 (NOT FOUND).
     */
    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable String id) {
        return taskService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada com ID: " + id));
    }

    /**
     * POST: Adiciona uma nova tarefa.
     * Mapeamento: POST /api/tasks
     * @param task A tarefa a ser criada, vinda do corpo da requisição JSON.
     * @return ResponseEntity com a tarefa criada e status 201 (CREATED).
     */
    @PostMapping
    public ResponseEntity<Task> addTask(@Valid @RequestBody Task task) {
        Task newTask = taskService.addTask(task);
        return new ResponseEntity<>(newTask, HttpStatus.CREATED); // Retorna 201 CREATED
    }

    /**
     * PUT: Atualiza uma tarefa existente.
     * Mapeamento: PUT /api/tasks/{id}
     * @param id O ID da tarefa a ser atualizada.
     * @param taskDetails Os detalhes da tarefa para atualização (corpo JSON).
     * @return ResponseEntity com a tarefa atualizada e status 200 (OK), ou 404 (NOT FOUND).
     */
    @PutMapping("/{id}")
    public Task updateTask(@PathVariable String id, @Valid @RequestBody Task taskDetails) {
        return taskService.updateTask(id, taskDetails)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não foi possível atualizar. Tarefa não encontrada com ID: " + id));
    }

    /**
     * DELETE: Exclui uma tarefa.
     * Mapeamento: DELETE /api/tasks/{id}
     * @param id O ID da tarefa a ser excluída.
     * @return ResponseEntity com status 204 (NO CONTENT).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Retorna 204 No Content
    public void deleteTask(@PathVariable String id) {
        if (!taskService.deleteTask(id)) {
             // Lança 404 se a tarefa não existir para deleção
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Não foi possível deletar. Tarefa não encontrada com ID: " + id);
        }
    }
}
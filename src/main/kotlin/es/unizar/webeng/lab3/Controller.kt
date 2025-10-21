package es.unizar.webeng.lab3

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
class EmployeeController(
    private val repository: EmployeeRepository,
) {
    @Operation(summary = "Get all employees", description = "Returns a list of all employees")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = [Content(mediaType = "application/json")],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
            ),
        ],
    )
    @GetMapping("/employees")
    @Cacheable("employeesAll")
    fun all(): Iterable<Employee> = repository.findAll()

    @Operation(summary = "Create a new employee", description = "Creates a new employee with the provided data")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Employee created successfully",
                content = [Content(mediaType = "application/json")],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid input data",
            ),
        ],
    )
    @PostMapping("/employees")
    @CacheEvict(value = ["employeesAll"], allEntries = true)
    fun newEmployee(
        @RequestBody newEmployee: Employee,
    ): ResponseEntity<Employee> {
        val employee = repository.save(newEmployee)
        val location =
            ServletUriComponentsBuilder
                .fromCurrentServletMapping()
                .path("/employees/{id}")
                .build(employee.id)
        return ResponseEntity.created(location).body(employee)
    }

    @Operation(summary = "Get an employee by ID", description = "Returns the employee with the specified ID")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successful operation",
                content = [Content(mediaType = "application/json")],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Employee not found",
            ),
        ],
    )
    @GetMapping("/employees/{id}")
    @Cacheable("employees", key = "#id")
    fun one(
        @PathVariable id: Long,
    ): Employee = repository.findById(id).orElseThrow { EmployeeNotFoundException(id) }

    @Operation(
        summary = "Update an existing employee",
        description = "Updates the employee with the specified ID or creates a new one if it doesn't exist",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Employee updated successfully",
                content = [Content(mediaType = "application/json")],
            ),
            ApiResponse(
                responseCode = "201",
                description = "Employee created successfully",
                content = [Content(mediaType = "application/json")],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid input data",
            ),
        ],
    )
    @PutMapping("/employees/{id}")
    @CacheEvict(value = ["employeesAll"], allEntries = true)
    fun replaceEmployee(
        @RequestBody newEmployee: Employee,
        @PathVariable id: Long,
    ): ResponseEntity<Employee> {
        val location =
            ServletUriComponentsBuilder
                .fromCurrentServletMapping()
                .path("/employees/{id}")
                .build(id)
                .toASCIIString()
        val (status, body) =
            repository
                .findById(id)
                .map { employee ->
                    employee.name = newEmployee.name
                    employee.role = newEmployee.role
                    repository.save(employee)
                    HttpStatus.OK to employee
                }.orElseGet {
                    newEmployee.id = id
                    repository.save(newEmployee)
                    HttpStatus.CREATED to newEmployee
                }
        return ResponseEntity.status(status).header("Content-Location", location).body(body)
    }

    @Operation(summary = "Delete an employee", description = "Deletes the employee with the specified ID")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Employee deleted successfully",
            ),
            ApiResponse(
                responseCode = "404",
                description = "Employee not found",
            ),
        ],
    )
    @DeleteMapping("/employees/{id}")
    @CacheEvict(value = ["employeesAll"], allEntries = true)
    fun deleteEmployee(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        repository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class EmployeeNotFoundException(
    id: Long,
) : Exception("Could not find employee $id")

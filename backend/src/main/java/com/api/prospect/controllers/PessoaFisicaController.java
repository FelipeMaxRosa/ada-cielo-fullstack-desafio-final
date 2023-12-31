package com.api.prospect.controllers;

import com.api.prospect.dtos.PessoaFisicaDto;
import com.api.prospect.models.PessoaFisicaModel;
import com.api.prospect.repositories.PessoaFisicaRepository;
import com.api.prospect.utils.StringUtils;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/prospect-pessoa-fisica")
public class PessoaFisicaController {
  @Autowired
  private PessoaFisicaRepository pessoaFisicaRepository;

  private final Queue<PessoaFisicaModel> prospectQueue = new LinkedList<>();

  @ApiOperation(value ="Adiciona um novo Prospect de Pessoa Fisica")
  @PostMapping
  public ResponseEntity<Object> addNewProspectPessoaFisica(@RequestBody @Valid PessoaFisicaDto pessoaFisicaDto) {
    if (pessoaFisicaRepository.existsByCpf(pessoaFisicaDto.getCpf())) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Conflito: CPF já está em uso!");
    }

    var pessoaFisicaModel = new PessoaFisicaModel();
    BeanUtils.copyProperties(pessoaFisicaDto, pessoaFisicaModel);

    String formattedCpf = StringUtils.formatToSpecificDigits(pessoaFisicaDto.getCpf(), 11);
    pessoaFisicaModel.setCpf(formattedCpf);

    PessoaFisicaModel savedPessoaFisicaModel = pessoaFisicaRepository.save(pessoaFisicaModel);
    prospectQueue.offer(savedPessoaFisicaModel);

    return ResponseEntity.status(HttpStatus.CREATED).body(savedPessoaFisicaModel);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Object> updateProspectPessoaFisica(
          @PathVariable(value = "id") Long id,
          @RequestBody @Valid PessoaFisicaDto pessoaFisicaDto
  ) {
    Optional<PessoaFisicaModel> pessoaFisicaModelOptional = pessoaFisicaRepository.findById(id);

    if (!pessoaFisicaModelOptional.isPresent()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Prospect Pessoa Física não encontrado.");
    }

    var pessoaFisicaModel = new PessoaFisicaModel();
    BeanUtils.copyProperties(pessoaFisicaDto, pessoaFisicaModel);
    pessoaFisicaModel.setId(pessoaFisicaModelOptional.get().getId());

    String formattedCpf = StringUtils.formatToSpecificDigits(pessoaFisicaDto.getCpf(), 11);
    pessoaFisicaModel.setCpf(formattedCpf);

    PessoaFisicaModel updatedPessoaFisica = pessoaFisicaRepository.save(pessoaFisicaModel);
    prospectQueue.offer(updatedPessoaFisica);

    return ResponseEntity.status(HttpStatus.OK).body(updatedPessoaFisica);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Object> getOneProspectPessoaFisica(@PathVariable(value = "id") Long id) {
    Optional<PessoaFisicaModel> pessoaFisicaModelOptional = pessoaFisicaRepository.findById(id);

    if (!pessoaFisicaModelOptional.isPresent()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Prospect Pessoa Física não encontrado.");
    }

    return ResponseEntity.status(HttpStatus.OK).body(pessoaFisicaModelOptional.get());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Object> deleteProspectPessoaFisica(@PathVariable(value = "id") Long id) {
    Optional<PessoaFisicaModel> pessoaFisicaModelOptional = pessoaFisicaRepository.findById(id);

    if (!pessoaFisicaModelOptional.isPresent()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Prospect Pessoa Física não encontrado.");
    }
    pessoaFisicaRepository.delete(pessoaFisicaModelOptional.get());

    return ResponseEntity.status(HttpStatus.OK).body("Prospect Pessoa Fisica foi deletado com sucesso.");
  }

  @GetMapping
  public ResponseEntity<Object> getAllProspectPessoaFisica() {
    return ResponseEntity.status(HttpStatus.OK).body(pessoaFisicaRepository.findAll());
  }

  @GetMapping("/service-queue")
  public ResponseEntity<Object> getProspectQueuePessoaFisica() {
    return ResponseEntity.status(HttpStatus.OK).body(prospectQueue.toArray());
  }

  @GetMapping("/service-queue/next-prospect")
  public ResponseEntity<Object> getNextProspectOnTheQueuePessoaFisica() {
    PessoaFisicaModel nextProspect = prospectQueue.poll();

    if (nextProspect != null) {
      return ResponseEntity.status(HttpStatus.OK).body(nextProspect);
    }

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("A fila de atendimento de prospects está vazia!");
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    return errors;
  }
}

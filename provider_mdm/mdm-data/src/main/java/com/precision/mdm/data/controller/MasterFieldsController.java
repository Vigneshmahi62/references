package com.precision.mdm.data.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.precision.mdm.data.dto.FieldsDto;
import com.precision.mdm.data.exception.ErrorResponse;
import com.precision.mdm.data.model.MasterFieldsKey;
import com.precision.mdm.data.service.MasterFieldsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Operations pertaining to Master Fields")
public class MasterFieldsController {

	private final MasterFieldsService masterFieldsService;

	@Operation(summary = "Get Available Fields")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Found Fields", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "404", description = "No Fields found", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 404,\"message\": \"No Records found\",\"stackTrace\": null,\"errors\": null}")) }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)) }) })
	@GetMapping(path = "/mdm/fields", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<FieldsDto> getMasterFields() {
		return masterFieldsService.getMasterFields();
	}

	@Operation(summary = "Get Field by Id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Field found against the requested ID", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "404", description = "Not found", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)) }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)) }) })
	@GetMapping(path = "/mdm/fields/{providerType}/{fieldName}/{fieldId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public FieldsDto getMasterFieldsById(@PathVariable("providerType") final String providerType,
			@PathVariable("fieldName") final String fieldName,
			@PathVariable("fieldId") final Integer fieldId) {
		return masterFieldsService
				.getMasterFieldsById(new MasterFieldsKey(providerType, fieldName, fieldId));
	}

	@Operation(summary = "Create Fields", requestBody = @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)), responses = {
			@ApiResponse(responseCode = "200", description = "Fields Created", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 500,\"message\": \"Unknown error occurred\",\"stackTrace\": null,\"errors\": null}")) }) })
	@PostMapping(path = "/mdm/fields", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(code = HttpStatus.CREATED)
	public List<FieldsDto> createMasterFields(
			@org.springframework.web.bind.annotation.RequestBody final List<FieldsDto> fieldsDtos) {
		return masterFieldsService.createMasterFields(fieldsDtos);
	}

	@Operation(summary = "Update Fields", requestBody = @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)), responses = {
			@ApiResponse(responseCode = "200", description = "Fields Updated", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 500,\"message\": \"Unknown error occurred\",\"stackTrace\": null,\"errors\": null}")) }) })
	@PutMapping(path = "/mdm/fields", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(code = HttpStatus.OK)
	public List<FieldsDto> updateMasterFields(
			@org.springframework.web.bind.annotation.RequestBody final List<FieldsDto> fieldsDtos) {
		return masterFieldsService.updateMasterFields(fieldsDtos);
	}

	@Operation(summary = "Delete Field by Id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Field deleted against the requested ID", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "404", description = "Not found", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 404,\"message\": \"Not found\",\"stackTrace\": null,\"errors\": null}")) }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 500,\"message\": \"Unknown error occurred\",\"stackTrace\": null,\"errors\": null}")) }) })
	@DeleteMapping(path = "/mdm/fields/{providerType}/{fieldName}/{fieldId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(code = HttpStatus.OK)
	public String deleteMasterFields(@PathVariable("providerType") final String providerType,
			@PathVariable("fieldName") final String fieldName,
			@PathVariable("fieldId") final Integer fieldId) {
		return masterFieldsService
				.deleteMasterFields(new MasterFieldsKey(providerType, fieldName, fieldId));
	}
}

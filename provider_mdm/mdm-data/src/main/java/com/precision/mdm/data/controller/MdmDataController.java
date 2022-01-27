package com.precision.mdm.data.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.precision.mdm.data.annotations.Timer;
import com.precision.mdm.data.exception.ErrorResponse;
import com.precision.mdm.data.model.MdmDataKey;
import com.precision.mdm.data.service.MdmDataService;

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
@Tag(name = "Operations pertaining to Mdm Data")
public class MdmDataController {

	private final MdmDataService mdmDataService;

	@Operation(summary = "Get Available mdm data")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Found data", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "404", description = "No data found", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 404,\"message\": \"No Records found\",\"stackTrace\": null,\"errors\": null}")) }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)) }) })
	@GetMapping(path = "/mdm/data", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getMdmData() {
		return mdmDataService.getMdmData();
	}

	@Operation(summary = "Get data by MDM Id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Data found against the MDM ID", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "404", description = "Not found", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)) }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)) }) })
	@GetMapping(path = "/mdm/data/{mdmId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Timer
	public String getMdmDataByMdmId(@PathVariable("mdmId") final Integer mdmId) {
		return mdmDataService.getMdmDataByMdmId(mdmId);
	}

	@Operation(summary = "Get Data by Id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Data found against the requested ID", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "404", description = "Not found", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)) }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)) }) })
	@GetMapping(path = "/mdm/data/{providerType}/{mdmId}/{rowType}/{rowId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getMdmDataById(@PathVariable("providerType") final String providerType,
			@PathVariable("mdmId") final Integer mdmId,
			@PathVariable("rowType") final String rowType,
			@PathVariable("rowId") final UUID rowId) {
		return mdmDataService.getMdmDataById(new MdmDataKey(providerType, mdmId, rowType, rowId));
	}

	@Operation(summary = "Create Data", requestBody = @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)), responses = {
			@ApiResponse(responseCode = "200", description = "Data Created", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 500,\"message\": \"Unknown error occurred\",\"stackTrace\": null,\"errors\": null}")) }) })
	@PostMapping(path = "/mdm/data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(code = HttpStatus.CREATED)
	public String createMdmData(
			@org.springframework.web.bind.annotation.RequestBody final String request) {
		return mdmDataService.createMdmData(request);
	}

	@Operation(summary = "Update Data", requestBody = @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)), responses = {
			@ApiResponse(responseCode = "200", description = "Fields Updated", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 500,\"message\": \"Unknown error occurred\",\"stackTrace\": null,\"errors\": null}")) }) })
	@PutMapping(path = "/mdm/data", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(code = HttpStatus.OK)
	public String updateMdmData(
			@org.springframework.web.bind.annotation.RequestBody final String request) {
		return mdmDataService.updateMdmData(request);
	}

	@Operation(summary = "Delete Data by Id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Data deleted against the requested ID", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "404", description = "Not found", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 404,\"message\": \"Not found\",\"stackTrace\": null,\"errors\": null}")) }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 500,\"message\": \"Unknown error occurred\",\"stackTrace\": null,\"errors\": null}")) }) })
	@DeleteMapping(path = "/mdm/data/{providerType}/{mdmId}/{rowType}/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(code = HttpStatus.OK)
	public String deleteMdmData(@PathVariable("providerType") final String providerType,
			@PathVariable("mdmId") final Integer mdmId,
			@PathVariable("rowType") final String rowType,
			@PathVariable("uuid") final String uuid) {
		return mdmDataService
				.deleteMdmData(new MdmDataKey(providerType, mdmId, rowType, UUID.fromString(uuid)));
	}

	@Operation(summary = "Delete Data by MDM Id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Data deleted against the MDM ID", content = {
					@Content(mediaType = "application/json") }),
			@ApiResponse(responseCode = "404", description = "Not found", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 404,\"message\": \"Not found\",\"stackTrace\": null,\"errors\": null}")) }),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
					@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = "{\"status\": 500,\"message\": \"Unknown error occurred\",\"stackTrace\": null,\"errors\": null}")) }) })
	@DeleteMapping(path = "/mdm/data/{mdmId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(code = HttpStatus.OK)
	public List<MdmDataKey> deleteMdmData(@PathVariable("mdmId") final Integer mdmId) {
		return mdmDataService.deleteMdmData(mdmId);
	}
}

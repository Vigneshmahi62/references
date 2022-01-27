package com.compsource.app.data.validation;

import com.compsource.app.data.model.PolicyLimitChangeRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TestPolicyLimitChangeReqValidator {

    private String request;
    private Boolean expectedOutput;
    private ObjectMapper mapper;

    public TestPolicyLimitChangeReqValidator(String request, Boolean expectedOutput) {
        this.request = request;
        this.expectedOutput = expectedOutput;
    }

    @Parameterized.Parameters
    public static Collection requestSamples() {
        return Arrays.asList(new Object[][]{
                {"{\"coiRequestId\":\"65b68288-e02a-4009-b2ea-41e54058e268\"," +
                        "\"accountId\":1122132,\"existingPolicyCoverageLimit\": 100000," +
                        "\"isChangeRequested\":true,\"newlyRequestedPolicyCoverageLimit\":200000," +
                        "\"isNewRecipient\":false,\"recipientId\":\"de31a442-4e0d-4b40-9a3a" +
                        "-47afb02877c1\",\"recipientName\":null,\"recipientEmail\":null}", true},
                {"{\"coiRequestId\":\"bbcc6eda-6b72-4975-8bd7-ea4ba2544bb4\"," +
                        "\"accountId\":4409897,\"existingPolicyCoverageLimit\": 500000," +
                        "\"isChangeRequested\":true," +
                        "\"newlyRequestedPolicyCoverageLimit\":1000000,\"isNewRecipient\":false," +
                        "\"recipientId\":\"f93c42d6-6d33-4086-b8c0-cb27d0f6e900\"," +
                        "\"recipientName\":null,\"recipientEmail\":null}", true},
                {"{\"coiRequestId\":\"8b1d5e87-0598-4b61-9758-8b497aa03830\"," +
                        "\"accountId\":235651,\"existingPolicyCoverageLimit\": 1000000," +
                        "\"isChangeRequested\":true,\"newlyRequestedPolicyCoverageLimit\":500000," +
                        "\"isNewRecipient\":false,\"recipientId\": " +
                        "\"496ddfc1-262b-4e88-87ba-50b1a45b37f8\",\"recipientName\":null," +
                        "\"recipientEmail\":null}", false},
                {"{\"coiRequestId\":\"772fe461-a737-4d85-94ac-219661450b2b\"," +
                        "\"accountId\":985725,\"existingPolicyCoverageLimit\": 50000," +
                        "\"isChangeRequested\":true,\"newlyRequestedPolicyCoverageLimit\":null," +
                        "\"isNewRecipient\":false,\"recipientId\":\"fa0bc76a-af59-4c58-994f" +
                        "-1aca0de55864\",\"recipientName\":null,\"recipientEmail\":null}", false},
                {"{\"coiRequestId\":\"d90bfc22-05dc-4db8-b4f9-2d3a665acd32\"," +
                        "\"accountId\":7824561,\"existingPolicyCoverageLimit\":2000000," +
                        "\"isChangeRequested\":true," +
                        "\"newlyRequestedPolicyCoverageLimit\":2500000,\"isNewRecipient\":true," +
                        "\"recipientId\":\"ed249c31-1f46-4dac-bc14-d4e2479d7f57\"," +
                        "\"recipientName\":\"Amy's Restaurant\",\"recipientEmail\":null}", false},
                {"{\"coiRequestId\":\"d90bfc22-05dc-4db8-b4f9-2d3a665acd32\"," +
                        "\"accountId\":null,\"existingPolicyCoverageLimit\":2000000," +
                        "\"isChangeRequested\":true," +
                        "\"newlyRequestedPolicyCoverageLimit\":2500000,\"isNewRecipient\":true," +
                        "\"recipientId\":\"ed249c31-1f46-4dac-bc14-d4e2479d7f57\"," +
                        "\"recipientName\":\"Amy's Restaurant\",\"recipientEmail\":null}", false},
                {"{\"coiRequestId\":\"65b68288-e02a-4009-b2ea-41e54058e268\"," +
                        "\"accountId\":1122132,\"existingPolicyCoverageLimit\": 100000," +
                        "\"isChangeRequested\":true,\"newlyRequestedPolicyCoverageLimit\":200000," +
                        "\"isNewRecipient\":true,\"recipientId\":\"de31a442-4e0d-4b40-9a3a" +
                        "-47afb02877c1\",\"recipientName\":null,\"recipientEmail\":null}", false},
                {"{\"coiRequestId\":\"bbcc6eda-6b72-4975-8bd7-ea4ba2544bb4\"," +
                        "\"accountId\":-4409897,\"existingPolicyCoverageLimit\": 500000," +
                        "\"isChangeRequested\":true," +
                        "\"newlyRequestedPolicyCoverageLimit\":1000000,\"isNewRecipient\":false," +
                        "\"recipientId\":\"f93c42d6-6d33-4086-b8c0-cb27d0f6e900\"," +
                        "\"recipientName\":null,\"recipientEmail\":null}", false},
                {"{\"coiRequestId\": \"e756d40f-3278-4192-6c72-f3f3fd212fek\",\"accountId\": " +
                        "11272,\"existingPolicyCoverageLimit\": 6252,\"isChangeRequested\": true," +
                        "\"newlyRequestedPolicyCoverageLimit\": 171463,\"isNewRecipient\": true," +
                        "\"recipientId\": \"b28e1dc5-378d-4ba2-7208-7765149863cd\"," +
                        "\"recipientName\": \"Sameeha Gregory\",\"recipientEmail\": " +
                        "\"SameehaGregory@gmail.com\",\"dateRequested\": \"2021-05-13 " +
                        "12:00:00\"}", false},
                {"{\"coiRequestId\": \"27a38f48-a95c-48b1-b890-4fffd511310e\",\"accountId\": " +
                        "11245,\"existingPolicyCoverageLimit\": 100000,\"isChangeRequested\": " +
                        "true,\"newlyRequestedPolicyCoverageLimit\": 200000,\"isNewRecipient\": " +
                        "true,\"recipientId\": \"3dde4653-6388-4500-879a-e42a37c81319\"," +
                        "\"recipientName\": \"braums\",\"recipientEmail\": \"braums@gmail.com\"," +
                        "\"dateRequested\": \"2021-05-13 12:00:00\"}", true}
        });
    }

    @Before
    public void initialize() {
        this.mapper = new ObjectMapper();
    }

    @Test
    public void testRequestValidation() {
        try {
            PolicyLimitChangeRequest policyLimitChangeRequest = mapper.readValue(request,
                    PolicyLimitChangeRequest.class);
            assertEquals(expectedOutput, PolicyLimitChangeReqValidator.validate(policyLimitChangeRequest));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

}

package com.compsource.app.data.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class TestPolicyLimitChangeReqStats {
    private PolicyLimitChangeRequestsStatistics policyLimitChangeRequestsStatistics;
    private ObjectMapper mapper;

    @Before
    public void initialize() {
        this.policyLimitChangeRequestsStatistics = new PolicyLimitChangeRequestsStatistics();
        this.mapper = new ObjectMapper();
    }


    @Test
    public void testStatisticsAdd() throws JsonProcessingException {
        List<String> sampleRequests = Arrays.asList("{\"coiRequestId\":\"65b68288-e02a-4009-b2ea" +
                        "-41e54058e268\",\"accountId\":1122132,\"existingPolicyCoverageLimit\": " +
                        "100000,\"isChangeRequested\":true," +
                        "\"newlyRequestedPolicyCoverageLimit\":200000,\"isNewRecipient\":false," +
                        "\"recipientId\":\"de31a442-4e0d-4b40-9a3a-47afb02877c1\"," +
                        "\"recipientName\":null,\"recipientEmail\":null}",
                "{\"coiRequestId\":\"bbcc6eda-6b72-4975-8bd7-ea4ba2544bb4\"," +
                        "\"accountId\":4409897,\"existingPolicyCoverageLimit\": 500000," +
                        "\"isChangeRequested\":true," +
                        "\"newlyRequestedPolicyCoverageLimit\":1000000,\"isNewRecipient\":false," +
                        "\"recipientId\":\"f93c42d6-6d33-4086-b8c0-cb27d0f6e900\"," +
                        "\"recipientName\":null,\"recipientEmail\":null}",
                "{\"coiRequestId\":\"bbcc6eda-6b72-4975-8bd7-ea4ba2544bb4\"," +
                        "\"accountId\":4409897,\"existingPolicyCoverageLimit\": 60000," +
                        "\"isChangeRequested\":true," +
                        "\"newlyRequestedPolicyCoverageLimit\":500000,\"isNewRecipient\":false," +
                        "\"recipientId\":\"f93c42d6-6d33-4086-b8c0-cb27d0f6e900\"," +
                        "\"recipientName\":null,\"recipientEmail\":null}");
//        List<PolicyLimitChangeRequest> requestObjects = new ArrayList<>();

        for (String request : sampleRequests) {
            PolicyLimitChangeRequest policyLimitChangeRequest = mapper.readValue(request,
                    PolicyLimitChangeRequest.class);
//            requestObjects.add(policyLimitChangeRequest);
            policyLimitChangeRequestsStatistics.add(policyLimitChangeRequest);
        }
//        Integer requestCount = requestObjects.size();
//        Long increasedPolicyLimit = requestObjects.stream()
//                .mapToLong(request -> request.getNewlyRequestedPolicyCoverageLimit() - request.getExistingPolicyCoverageLimit()).sum();

        Integer expectedRequestCount = 3;
        Long expectedIncreasedPolicyLimit = 1040000L;

        assertEquals(policyLimitChangeRequestsStatistics.getTotalCoverageIncreaseRequestsCount(),
                expectedRequestCount);

        assertEquals(policyLimitChangeRequestsStatistics.getTotalCoverageIncreaseAmount(), expectedIncreasedPolicyLimit);

    }


}



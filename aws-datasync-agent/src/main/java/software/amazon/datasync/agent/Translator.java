package software.amazon.datasync.agent;

import software.amazon.awssdk.services.datasync.model.*;

public class Translator {

    Translator() {}

    public static CreateAgentRequest translateToCreateRequest(final ResourceModel model) {
        return CreateAgentRequest.builder()
                .agentName(model.getAgentName())
                .activationKey(model.getActivationKey())
                .securityGroupArns(model.getSecurityGroupArns())
                .subnetArns(model.getSubnetArns())
                .vpcEndpointId(model.getVpcEndpointId())
                .build();

    }

    public static DeleteAgentRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteAgentRequest.builder()
                .agentArn(model.getAgentArn())
                .build();
    }

    public static ListAgentsRequest translateToListRequest(final String nextToken) {
        return ListAgentsRequest.builder()
                .maxResults(100)
                .nextToken(nextToken)
                .build();
    }

    public static UpdateAgentRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdateAgentRequest.builder()
                .name(model.getAgentName())
                .agentArn(model.getAgentArn())
                .build();
    }

    public static DescribeAgentRequest translateToReadRequest(final ResourceModel model) {
        return DescribeAgentRequest.builder()
                .agentArn(model.getAgentArn())
                .build();
    }
}
package software.amazon.datasync.agent;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.datasync.model.*;

import java.util.List;
import java.util.Optional;

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

//    public static CreateAgentRequest translateToCreateRequest(final ResourceModel model) {
//        final CreateAgentRequest.Builder createAgentRequestBuilder =
//                CreateAgentRequest.builder().activationKey(model.getActivationKey());
//
//        checkString(model.getAgentName()).ifPresent(createAgentRequestBuilder::agentName);
//        checkList(model.getSecurityGroupArns()).ifPresent(createAgentRequestBuilder::securityGroupArns);
//        checkList(model.getSubnetArns()).ifPresent(createAgentRequestBuilder::subnetArns);
//        checkString(model.getVpcEndpointId()).ifPresent(createAgentRequestBuilder::vpcEndpointId);
//
//        return createAgentRequestBuilder.build();
//
//    }

    public static DeleteAgentRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteAgentRequest.builder()
                .agentArn(model.getAgentArn())
                .build();
    }

    // I need to check on whether or not nextToken is exactly necessary here
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

    /*
        Returns an Optional instance: empty if the attribute does not exist, otherwise includes the non-null value
     */
    private static Optional<String> checkString(final String s) {
        if (StringUtils.isNullOrEmpty(s)) {
            return Optional.empty();
        } else {
            return Optional.of(s);
        }
    }

    /*
    Returns an Optional instance: empty if the attribute does not exist, otherwise includes the non-null value
    */
    private static Optional<List<String>> checkList(final List<String> a) {
        if (isNullOrEmpty(a)) {
            return Optional.empty();
        } else {
            return Optional.of(a);
        }
    }

    private static boolean isNullOrEmpty(List<String> a) {
        return a == null || a.isEmpty();
    }


}
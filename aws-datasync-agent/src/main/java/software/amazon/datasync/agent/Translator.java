package software.amazon.datasync.agent;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.datasync.model.CreateAgentRequest;
import software.amazon.awssdk.services.datasync.model.DeleteAgentRequest;
import software.amazon.awssdk.services.datasync.model.ListAgentsRequest;
import software.amazon.awssdk.services.datasync.model.UpdateAgentRequest;
import software.amazon.awssdk.services.datasync.model.DescribeAgentRequest;
import software.amazon.awssdk.services.datasync.model.TagListEntry;

public class Translator {

    Translator() {}

    public static CreateAgentRequest translateToCreateRequest(final ResourceModel model) {
        return CreateAgentRequest.builder()
                .agentName(model.getAgentName())
                .activationKey(model.getActivationKey())
                .securityGroupArns(model.getSecurityGroupArns())
                .subnetArns(model.getSubnetArns())
                .vpcEndpointId(model.getVpcEndpointId())
                .tags(Translator.translateTags(model.getTags()))
                .build();
    }

    public static DeleteAgentRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteAgentRequest.builder()
                .agentArn(model.getAgentArn())
                .build();
    }

    public static ListAgentsRequest translateToListRequest(final String nextToken) {
        return ListAgentsRequest.builder()
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

    // Convert Tag to TagListEntry
    static Set<TagListEntry> translateTags(final Set<Tag> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.stream()
                .map(tag -> TagListEntry.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }
}
package software.amazon.datasync.agent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.datasync.model.*;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

public class Translator {

    Translator() {}

    public static CreateAgentRequest translateToCreateRequest(final ResourceModel model, final Map<String, String> tags) {
        return CreateAgentRequest.builder()
                .agentName(model.getAgentName())
                .activationKey(model.getActivationKey())
                .securityGroupArns(model.getSecurityGroupArns())
                .subnetArns(model.getSubnetArns())
                .vpcEndpointId(model.getVpcEndpointId())
                .tags(translateMapToTagListEntries(tags))
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

    public static ListTagsForResourceRequest translateToListTagsRequest(final ResourceModel model) {
        return ListTagsForResourceRequest.builder()
                .resourceArn(model.getAgentArn())
                .build();
    }

    public static TagResourceRequest translateToTagResourceRequest(Set<Tag> tagsToAdd, String agentArn) {
        return TagResourceRequest.builder()
                .resourceArn(agentArn)
                .tags(translateTags(tagsToAdd))
                .build();
    }

    public static UntagResourceRequest translateToUntagResourceRequest(Set<String> tagsToRemove, String agentArn) {
        return UntagResourceRequest.builder()
                .resourceArn(agentArn)
                .keys(tagsToRemove)
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

    // Convert TagListEntry to Tag
    static Set<Tag> translateTagListEntries(final List<TagListEntry> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.stream()
                .map(tag -> Tag.builder().key(tag.key()).value(tag.value()).build())
                .collect(Collectors.toSet());
    }

    static Map<String, String> translateTagsToMap(final Set<Tag> tags) {
        if (tags == null)
            return Collections.emptyMap();
        return tags.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue));
    }

    static Set<TagListEntry> translateMapToTagListEntries(final Map<String, String> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.entrySet().stream().map(entry -> {
            return TagListEntry.builder().key(entry.getKey()).value(entry.getValue()).build();
        }).collect(Collectors.toSet());
    }

    static Set<Tag> translateMapToTags(final Map<String, String> tags) {
        if (tags == null)
            return Collections.emptySet();
        return tags.entrySet().stream().map(entry -> {
            return Tag.builder().key(entry.getKey()).value(entry.getValue()).build();
        }).collect(Collectors.toSet());
    }

    public static BaseHandlerException translateDataSyncExceptionToCfnException(DataSyncException e) {
        if (e.isThrottlingException()) {
            return new CfnThrottlingException(e);
        } else {
            return new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }
    }
}

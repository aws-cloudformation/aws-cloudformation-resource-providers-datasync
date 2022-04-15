package software.amazon.datasync.locationfsxlustre;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.*;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Since tags cannot be retrieved or updated through the DataSync Describe and Update
 * API calls, these methods make the appropriate tag-specific API requests.
 */
public class TagRequestMaker {
    private static final String AWS_TAG_PREFIX = "aws:";

    /**
     * Retrieve the tags associated with the given resource.
     *
     * @param proxy
     * @param client
     * @param resourceArn
     * @return the set of tags currently attached to the resource
     */
    public static Set<Tag> listTagsForResource(
            final AmazonWebServicesClientProxy proxy,
            final DataSyncClient client,
            final String resourceArn) {
        final ListTagsForResourceRequest listTagsForResourceRequest = TagTranslator.translateToListTagsRequest(resourceArn);

        ListTagsForResourceResponse tagsResponse;
        try {
            tagsResponse = proxy.injectCredentialsAndInvokeV2(listTagsForResourceRequest, client::listTagsForResource);
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, resourceArn);
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw Translator.translateDataSyncExceptionToCfnException(e);
        }

        if (tagsResponse.tags() != null) {
            return TagTranslator.translateTagListEntries(tagsResponse.tags());
        }
        return new HashSet<Tag>();
    }

    /**
     * Calculate and perform a delta update (additions and removals as needed) to
     * resource tags based on the current and previous tags supplied by the CloudFormation request.
     *
     * @param proxy
     * @param client
     * @param resourceArn
     * @param request
     * @param logger
     */
    public static void updateTagsForResource(
            final AmazonWebServicesClientProxy proxy,
            final DataSyncClient client,
            final String resourceArn,
            final ResourceHandlerRequest<ResourceModel> request,
            final Logger logger) {

        Map<String, String> tagList = request.getDesiredResourceTags();
        if (tagList == null) {
            tagList = new HashMap<String, String>();
        }

        Map<String, String> prevTagList = new HashMap<String, String>();
        if (request.getPreviousResourceTags() != null) {
            prevTagList = request.getPreviousResourceTags();
        }

        final Set<String> keysToRemove = Sets.difference(
                prevTagList.keySet(),
                tagList.keySet()
        );

        if (!keysToRemove.isEmpty()) {
            UntagResourceRequest untagResourceRequest = TagTranslator.translateToUntagResourceRequest(
                    keysToRemove, resourceArn);
            try {
                proxy.injectCredentialsAndInvokeV2(untagResourceRequest, client::untagResource);
                logger.log(String.format("%s %s old tags removed successfully", ResourceModel.TYPE_NAME,
                        resourceArn));
            } catch (InvalidRequestException e) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, resourceArn);
            } catch (InternalException e) {
                throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
            }
        }

        MapDifference<String, String> mapDifference = Maps.difference(tagList, prevTagList);
        final Set<Tag> tagsToAdd = mapDifference.entriesDiffering().entrySet().stream().map(entry -> {
            return Tag.builder().key(entry.getKey()).value(entry.getValue().leftValue()).build();
        }).collect(Collectors.toSet());
        tagsToAdd.addAll(TagTranslator.translateMapToTags(mapDifference.entriesOnlyOnLeft()));

        for (Tag tag : tagsToAdd) {
            if (tag.getKey().trim().toLowerCase().startsWith(AWS_TAG_PREFIX)) {
                throw new CfnInvalidRequestException(tag.getKey() + " is an invalid key. aws: prefixed tag key names cannot be requested.");
            }
        }

        if (request.getPreviousSystemTags() == null && request.getSystemTags() != null) {
            tagsToAdd.addAll(TagTranslator.translateMapToTags(request.getSystemTags()));
        }

        if (!tagsToAdd.isEmpty()) {
            TagResourceRequest tagResourceRequest = TagTranslator.translateToTagResourceRequest(
                    tagsToAdd, resourceArn);
            try {
                proxy.injectCredentialsAndInvokeV2(tagResourceRequest, client::tagResource);
                logger.log(String.format("%s %s tags updated successfully", ResourceModel.TYPE_NAME,
                        resourceArn));
            } catch (InvalidRequestException e) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, resourceArn);
            } catch (InternalException e) {
                throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
            }
        }
    }
}

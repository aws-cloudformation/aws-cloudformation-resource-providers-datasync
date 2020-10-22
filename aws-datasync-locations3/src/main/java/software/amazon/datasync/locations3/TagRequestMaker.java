package software.amazon.datasync.locations3;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.*;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
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

public class TagRequestMaker {

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
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        if (tagsResponse.tags() != null) {
            return TagTranslator.translateTagListEntries(tagsResponse.tags());
        }
        return new HashSet<Tag>();
    }

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

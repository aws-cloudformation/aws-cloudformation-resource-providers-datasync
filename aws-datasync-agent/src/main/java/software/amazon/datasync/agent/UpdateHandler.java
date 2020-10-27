package software.amazon.datasync.agent;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.TagResourceRequest;
import software.amazon.awssdk.services.datasync.model.UntagResourceRequest;
import software.amazon.awssdk.services.datasync.model.UpdateAgentRequest;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel prevModel = request.getPreviousResourceState();
        final ResourceModel currentModel = request.getDesiredResourceState();
        final DataSyncClient client = ClientBuilder.getClient();

        UpdateAgentRequest updateAgentRequest = Translator.translateToUpdateRequest(currentModel);

        try {
            proxy.injectCredentialsAndInvokeV2(updateAgentRequest, client::updateAgent);
            logger.log(String.format("%s %s updated successfully", ResourceModel.TYPE_NAME,
                    currentModel.getAgentArn()));
        } catch (InvalidRequestException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, currentModel.getAgentArn());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(e.getMessage(), e.getCause());
        }

        // Since tags are not maintained by the update request, we must manually calculate
        // a delta of tags to add and remove based on the resource- and stack-level tags
        // provided by the model and CloudFormation
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
            UntagResourceRequest untagResourceRequest = Translator.translateToUntagResourceRequest(
                    keysToRemove, currentModel.getAgentArn());
            try {
                proxy.injectCredentialsAndInvokeV2(untagResourceRequest, client::untagResource);
                logger.log(String.format("%s %s old tags removed successfully", ResourceModel.TYPE_NAME,
                        currentModel.getAgentArn()));
            } catch (InvalidRequestException e) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, currentModel.getAgentArn());
            } catch (InternalException e) {
                throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
            }
        }

        MapDifference<String, String> mapDifference = Maps.difference(tagList, prevTagList);
        final Set<Tag> tagsToAdd = mapDifference.entriesDiffering().entrySet().stream().map(entry -> {
            return Tag.builder().key(entry.getKey()).value(entry.getValue().leftValue()).build();
        }).collect(Collectors.toSet());
        tagsToAdd.addAll(Translator.translateMapToTags(mapDifference.entriesOnlyOnLeft()));

        if (!tagsToAdd.isEmpty()) {
            TagResourceRequest tagResourceRequest = Translator.translateToTagResourceRequest(
                    tagsToAdd, currentModel.getAgentArn());
            try {
                proxy.injectCredentialsAndInvokeV2(tagResourceRequest, client::tagResource);
                logger.log(String.format("%s %s tags updated successfully", ResourceModel.TYPE_NAME,
                        currentModel.getAgentArn()));
            } catch (InvalidRequestException e) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, currentModel.getAgentArn());
            } catch (InternalException e) {
                throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
            }
        }

        return new ReadHandler().handleRequest(proxy, request, callbackContext, logger);
    }

}

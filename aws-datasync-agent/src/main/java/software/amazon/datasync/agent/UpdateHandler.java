package software.amazon.datasync.agent;

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

import java.util.HashSet;
import java.util.Set;

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

        Set<Tag> currentTags = new HashSet<>();
        if (currentModel.getTags() != null) {
            currentTags = currentModel.getTags();
        }

        Set<Tag> existingTags = new HashSet<>();
        if (prevModel != null && prevModel.getTags() != null) {
            existingTags = prevModel.getTags();
        }

        final Set<String> keysToRemove = Sets.difference(
                Translator.extractTagKeys(existingTags),
                Translator.extractTagKeys(currentTags)
        );
        if (!keysToRemove.isEmpty()) {
            UntagResourceRequest untagResourceRequest = Translator.translateToUntagResourceRequest(
                    keysToRemove, currentModel.getAgentArn());
            logger.log(String.format("%s %s old tags removed successfully", ResourceModel.TYPE_NAME,
                    currentModel.getAgentArn()));
            try {
                proxy.injectCredentialsAndInvokeV2(untagResourceRequest, client::untagResource);
            } catch (InvalidRequestException e) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, currentModel.getAgentArn());
            } catch (InternalException e) {
                throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
            }
        }

        final Set<Tag> tagsToAdd = Sets.difference(currentTags, existingTags);
        if (!tagsToAdd.isEmpty()) {
            TagResourceRequest tagResourceRequest = Translator.translateToTagResourceRequest(
                    tagsToAdd, currentModel.getAgentArn());
            logger.log(String.format("%s %s tags updated successfully", ResourceModel.TYPE_NAME,
                    currentModel.getAgentArn()));
            try {
                proxy.injectCredentialsAndInvokeV2(tagResourceRequest, client::tagResource);
            } catch (InvalidRequestException e) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, currentModel.getAgentArn());
            } catch (InternalException e) {
                throw new CfnServiceInternalErrorException(e.getMessage(), e.getCause());
            }
        }

        return new ReadHandler().handleRequest(proxy, request, callbackContext, logger);
    }

}

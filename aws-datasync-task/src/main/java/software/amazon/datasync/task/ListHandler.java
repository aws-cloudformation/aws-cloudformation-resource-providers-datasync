package software.amazon.datasync.task;

import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.awssdk.services.datasync.model.DataSyncException;
import software.amazon.awssdk.services.datasync.model.InternalException;
import software.amazon.awssdk.services.datasync.model.InvalidRequestException;
import software.amazon.awssdk.services.datasync.model.ListTasksRequest;
import software.amazon.awssdk.services.datasync.model.ListTasksResponse;
import software.amazon.awssdk.services.datasync.model.TaskListEntry;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final String nextToken = request.getNextToken();
        final DataSyncClient client = ClientBuilder.getClient();

        final ListTasksRequest listTasksRequest = Translator.translateToListRequest(nextToken);

        final ListTasksResponse response;
        try {
            response = proxy.injectCredentialsAndInvokeV2(listTasksRequest, client::listTasks);
        } catch (InvalidRequestException e) {
            throw new CfnInvalidRequestException(listTasksRequest.toString(), e.getCause());
        } catch (InternalException e) {
            throw new CfnServiceInternalErrorException(listTasksRequest.toString(), e.getCause());
        } catch (DataSyncException e) {
            throw new CfnGeneralServiceException(listTasksRequest.toString(), e.getCause());
        }

        final List<ResourceModel> models = new ArrayList<>();
        for (TaskListEntry task : response.tasks()) {
            ResourceModel model = ResourceModel.builder()
                    .taskArn(task.taskArn())
                    .name(task.name())
                    .status(task.statusAsString())
                    .build();
            models.add(model);
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .nextToken(response.nextToken())
                .build();
    }
}

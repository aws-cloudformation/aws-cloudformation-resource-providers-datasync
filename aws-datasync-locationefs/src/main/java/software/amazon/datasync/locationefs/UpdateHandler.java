package software.amazon.datasync.locationefs;

import software.amazon.cloudformation.proxy.*;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .errorCode(HandlerErrorCode.NotUpdatable)
            .status(OperationStatus.FAILED)
            .build();
    }
}

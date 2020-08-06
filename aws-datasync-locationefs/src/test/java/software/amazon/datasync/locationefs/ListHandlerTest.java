package software.amazon.datasync.locationefs;

import software.amazon.awssdk.services.datasync.model.ListLocationsRequest;
import software.amazon.awssdk.services.datasync.model.ListLocationsResponse;
import software.amazon.awssdk.services.datasync.model.LocationListEntry;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListHandler handler = new ListHandler();

        final List<LocationListEntry> locations = buildDefaultList();

        ListLocationsResponse listLocationsResponse = ListLocationsResponse.builder()
                .locations(locations)
                .nextToken(null)
                .build();

        doReturn(listLocationsResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(ListLocationsRequest.class), any());

        final ResourceModel model1 = buildDefaultModel1();
        final ResourceModel model2 = buildDefaultModel2();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).containsAll(Arrays.asList(model1, model2));
        assertThat(response.getNextToken()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    private static List<LocationListEntry> buildDefaultList() {
        final String locationArn1 = "arn:aws:datasync:us-west-2:123456789012:location/loc-1234567890123456";
        final String locationUri1 = "efs://us-east-1.fs-01234567/";
        final String locationArn2 = "arn:aws:datasync:us-west-2:123456789012:location/loc-0123456789012345";
        final String locationUri2 = "efs://us-east-1.fs-12345678/";
        LocationListEntry location1 = LocationListEntry.builder()
                .locationArn(locationArn1)
                .locationUri(locationUri1)
                .build();
        LocationListEntry location2 = LocationListEntry.builder()
                .locationArn(locationArn2)
                .locationUri(locationUri2)
                .build();
        return Arrays.asList(location1, location2);
    }

    private static ResourceModel buildDefaultModel1() {
        final String locationArn = "arn:aws:datasync:us-west-2:123456789012:location/loc-1234567890123456";
        final String locationUri = "efs://us-east-1.fs-01234567/";
        return ResourceModel.builder()
                .locationArn(locationArn)
                .locationUri(locationUri)
                .build();
    }

    private static ResourceModel buildDefaultModel2() {
        final String locationArn = "arn:aws:datasync:us-west-2:123456789012:location/loc-0123456789012345";
        final String locationUri = "efs://us-east-1.fs-12345678/";
        return ResourceModel.builder()
                .locationArn(locationArn)
                .locationUri(locationUri)
                .build();
    }
}

package software.amazon.datasync.locationfsxopenzfs;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.datasync.DataSyncClient;
import software.amazon.cloudformation.LambdaWrapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientBuilder {

    public static DataSyncClient getClient() {
        return DataSyncClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }

}

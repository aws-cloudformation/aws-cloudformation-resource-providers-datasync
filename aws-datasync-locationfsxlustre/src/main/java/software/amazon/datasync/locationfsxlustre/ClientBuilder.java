package software.amazon.datasync.locationfsxlustre;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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

package software.amazon.datasync.agent;

import lombok.NoArgsConstructor;
import lombok.AccessLevel;
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

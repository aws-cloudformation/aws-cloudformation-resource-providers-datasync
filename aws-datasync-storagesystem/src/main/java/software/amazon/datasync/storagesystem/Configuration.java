package software.amazon.datasync.storagesystem;

import java.util.Map;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-datasync-storagesystem.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        return TagTranslator.translateTagsToMap(resourceModel.getTags());
    }

}

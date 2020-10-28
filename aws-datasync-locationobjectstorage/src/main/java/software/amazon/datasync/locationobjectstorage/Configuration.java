package software.amazon.datasync.locationobjectstorage;

import java.util.Map;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-datasync-locationobjectstorage.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        return TagTranslator.translateTagsToMap(resourceModel.getTags());
    }

}

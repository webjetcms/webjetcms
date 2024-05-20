Feature('webpages.media-groups');

Before(({ I, login }) => {
    login('admin');
});

Scenario('media-groups-zakladne testy', async ({I, DataTables }) => {
    I.amOnPage("/admin/v9/webpages/media-groups/");
    await DataTables.baseTest({
        dataTable: 'mediaGroupsTable',
        perms: 'editor_edit_media_group',
        skipSwitchDomain: true
    });
});
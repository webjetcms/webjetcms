/** Keeps the image dialog's URL logical while loading and inserting the selected working-version bytes. */
export function configureBranchImageUrl(field, previewUrl, logicalUrl) {
    const setup = field.setup;
    field.setup = function(type, element) {
        setup.call(this, type, element);
        if (type === 1) {
            this.setValue(logicalUrl(this.getValue()), true);
            this.setInitValue();
        }
    };

    const onChange = field.onChange;
    field.onChange = function() {
        const value = logicalUrl(this.getValue());
        // The native handler loads the image and its dimensions from the field value.
        // Suppress change events while supplying its display URL, then restore the visible logical URL.
        this.setValue(previewUrl(value), true);
        try {
            return onChange.call(this);
        } finally {
            this.setValue(value, true);
        }
    };

    const commit = field.commit;
    field.commit = function(type, element) {
        this.setValue(logicalUrl(this.getValue()), true);
        commit.call(this, type, element);
        if (type === 1 && element.getAttribute('src')) {
            element.setAttribute('src', previewUrl(element.getAttribute('src')));
        }
    };
}

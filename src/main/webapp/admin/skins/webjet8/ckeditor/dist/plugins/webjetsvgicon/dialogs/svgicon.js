CKEDITOR.dialog.add('svgiconDialog', function(editor) {
    var config = editor.config.webjetsvgicon || {};
    var iconsConfig = config.icons || {};
    var spritePath = config.spritePath || '/sprite.svg';
    var iconWidth = config.iconWidth || 48;
    var iconHeight = config.iconHeight || 48;
    var gridHeight = config.gridHeight || 400;
    var sizes = (config.sizes || 'small,medium,large,xlarge,xxlarge,huge').split(',');
    var colors = (config.colors || 'info,success,warning,danger,orange').split(',');

    // Parse icons configuration and build groups
    var iconNames = [];
    var allGroups = [];
    var iconToGroups = {};

    if (iconsConfig.size > 0) {
        iconNames = Object.keys(iconsConfig);
        iconNames.forEach(function(iconName) {
            var groups = iconsConfig[iconName] || [];
            iconToGroups[iconName] = groups;
            groups.forEach(function(group) {
                if (allGroups.indexOf(group) === -1) {
                    allGroups.push(group);
                }
            });
        });
    }

    // Sort groups alphabetically
    allGroups.sort();

    // Build group selector items with "Show All" as first option
    var groupItems = [[editor.lang.webjetsvgicon.showAll, 'all']];
    allGroups.forEach(function(group) {
        groupItems.push([group, group]);
    });

    // Build size selector items
    var sizeItems = [[editor.lang.webjetsvgicon.defaultSize, '']];
    sizes.forEach(function(size) {
        sizeItems.push([size, size]);
    });

    // Build color selector items
    var colorItems = [[editor.lang.webjetsvgicon.defaultColor, '']];
    colors.forEach(function(color) {
        colorItems.push([color, color]);
    });

    var selectedIconName = '';

    function filterIcons(dialog) {
        var selectedGroup = dialog.getValueOf('icons', 'iconGroup');
        var searchTerm = dialog.getValueOf('icons', 'iconSearch').toLowerCase();
        var gridContainer = dialog.getContentElement('icons', 'iconGrid').getElement();
        var iconItems = gridContainer.find('.wj-icon-item');
        var visibleCount = 0;

        for (var i = 0; i < iconItems.count(); i++) {
            var item = iconItems.getItem(i);
            var iconName = item.getAttribute('data-icon-name');
            var iconGroups = iconToGroups[iconName] || [];

            var groupMatch = selectedGroup === 'all' || iconGroups.indexOf(selectedGroup) !== -1;
            var searchMatch = iconName.toLowerCase().indexOf(searchTerm) !== -1;

            if (groupMatch && searchMatch) {
                item.show();
                visibleCount++;
            } else {
                item.hide();
            }
        }

        var noIconsMsg = gridContainer.findOne('.wj-no-icons-message');
        if (visibleCount === 0) {
            if (!noIconsMsg) {
                var msgElement = new CKEDITOR.dom.element('div');
                msgElement.addClass('wj-no-icons-message');
                msgElement.setHtml(editor.lang.webjetsvgicon.noIconsFound);
                msgElement.setStyles({
                    'text-align': 'center',
                    'padding': '20px',
                    'color': '#666',
                    'font-style': 'italic'
                });
                gridContainer.append(msgElement);
            } else {
                noIconsMsg.show();
            }
        } else if (noIconsMsg) {
            noIconsMsg.hide();
        }
    }

    function loadIconSprite(dialog) {
        var gridContainer = dialog.getContentElement('icons', 'iconGrid').getElement();

        // Show loading message
        gridContainer.setHtml('<div class="wj-loading-message" style="text-align: center; padding: 20px; color: #666;">' +
                             editor.lang.webjetsvgicon.loadingIcons + '</div>');

        CKEDITOR.ajax.loadXml(spritePath, function(xml) {
            if (xml) {
                // Create hidden sprite container
                var spriteContainer = CKEDITOR.document.getById('wj-svg-sprite-container');
                if (!spriteContainer) {
                    spriteContainer = new CKEDITOR.dom.element('div');
                    spriteContainer.setAttributes({
                        'id': 'wj-svg-sprite-container',
                        'style': 'display: none; position: absolute; top: -9999px;'
                    });
                    CKEDITOR.document.getBody().append(spriteContainer);

                    // Extract SVG content and inject it
                    var svgContent = xml.documentElement ? xml.documentElement.outerHTML : (new XMLSerializer()).serializeToString(xml.baseXml.childNodes[0]);
                    spriteContainer.setHtml(svgContent);
                }

                buildIconGrid(dialog, spriteContainer.$);
            } else {
                gridContainer.setHtml('<div class="wj-error-message" style="text-align: center; padding: 20px; color: #d32f2f;">' +
                                     editor.lang.webjetsvgicon.loadError + '</div>');
            }
        });
    }

    function buildIconGrid(dialog, xml) {
        var gridContainer = dialog.getContentElement('icons', 'iconGrid').getElement();
        var symbols = xml.getElementsByTagName('symbol');
        var availableIcons = [];

        // Filter symbols to only include configured icons
        var iconNamesEmpty = iconNames.length === 0;
        for (var i = 0; i < symbols.length; i++) {
            var symbolId = symbols[i].getAttribute('id');
            if (iconNamesEmpty === true) {
                //we dont have predefined list of icons, so accept all from sprite
                iconNames.push(symbolId);
            }
            if (iconNames.indexOf(symbolId) !== -1) {
                availableIcons.push(symbolId);
            }
        }

        var gridHtml = '<div class="wj-icon-grid" style="' +
            'display: grid; ' +
            'grid-template-columns: repeat(auto-fit, minmax(' + (iconWidth + 2 + 16) + 'px, 1fr)); ' +
            'gap: 8px; ' +
            'max-height: ' + gridHeight + 'px; ' +
            'overflow-y: auto; ' +
            'padding: 10px; ' +
            'border: 1px solid #ccc; ' +
            'background: #fff;' +
        '">';

        availableIcons.forEach(function(iconName) {
            gridHtml += '<div class="wj-icon-item" data-icon-name="' + iconName + '" style="' +
                'display: flex; ' +
                'flex-direction: column; ' +
                'align-items: center; ' +
                'padding: 8px; ' +
                'border: 2px solid transparent; ' +
                'border-radius: 4px; ' +
                'cursor: pointer; ' +
                'transition: all 0.2s; ' +
                'width: ' + iconWidth + 'px; ' +
                'height: ' + (iconHeight + 30) + 'px; ' +
                'justify-content: center;' +
                'overflow: hidden;' +
            '">' +
                '<div class="wj-icon-placeholder" style="' +
                    'width: ' + iconWidth + 'px; ' +
                    'height: ' + iconHeight + 'px; ' +
                    'border: 1px dashed #ccc; ' +
                    'display: flex; ' +
                    'align-items: center; ' +
                    'justify-content: center; ' +
                    'margin-bottom: 4px;' +
                '">Loading...</div>' +
                '<div class="wj-icon-name" style="' +
                    'font-size: 10px; ' +
                    'text-align: center; ' +
                    'word-break: break-all; ' +
                    'line-height: 1.2;' +
                '">' + iconName + '</div>' +
            '</div>';
        });

        gridHtml += '</div>';
        gridContainer.setHtml(gridHtml);

        // Attach click handlers and setup intersection observer
        setupIconGrid(dialog);
    }

    function setupIconGrid(dialog) {
        var gridContainer = dialog.getContentElement('icons', 'iconGrid').getElement();
        var iconItems = gridContainer.find('.wj-icon-item');

        // Setup intersection observer for lazy loading
        if (window.IntersectionObserver) {
            var observer = new IntersectionObserver(function(entries) {
                entries.forEach(function(entry) {
                    if (entry.isIntersecting) {
                        loadIconSvg(entry.target);
                        observer.unobserve(entry.target);
                    }
                });
            });

            for (var i = 0; i < iconItems.count(); i++) {
                var item = iconItems.getItem(i);
                observer.observe(item.$);

                // Add click handler
                item.on('click', function() {
                    selectIcon(dialog, this);
                });

                // Add hover effects
                item.on('mouseenter', function() {
                    if (!this.hasClass('wj-selected')) {
                        this.setStyle('background-color', '#f5f5f5');
                    }
                });

                item.on('mouseleave', function() {
                    if (!this.hasClass('wj-selected')) {
                        this.setStyle('background-color', '');
                    }
                });
            }
        } else {
            // Fallback for older browsers - load all icons immediately
            for (var i = 0; i < iconItems.count(); i++) {
                var item = iconItems.getItem(i);
                loadIconSvg(item.$);
                item.on('click', function() {
                    selectIcon(dialog, this);
                });
            }
        }
    }

    function loadIconSvg(itemElement) {
        var iconName = itemElement.getAttribute('data-icon-name');
        var placeholder = itemElement.querySelector('.wj-icon-placeholder');

        if (placeholder && iconName) {
            placeholder.innerHTML = '<svg width="' + iconWidth + '" height="' + iconHeight + '" style="max-width: 100%; max-height: 100%;">' +
                '<use href="#' + iconName + '"></use>' +
                '</svg>';
        }
    }

    function selectIcon(dialog, iconElement) {
        var gridContainer = dialog.getContentElement('icons', 'iconGrid').getElement();
        var allItems = gridContainer.find('.wj-icon-item');

        // Clear previous selection
        for (var i = 0; i < allItems.count(); i++) {
            var item = allItems.getItem(i);
            item.removeClass('wj-selected');
            item.setStyles({
                'border-color': 'transparent',
                'background-color': ''
            });
        }

        // Set new selection
        iconElement.addClass('wj-selected');
        iconElement.setStyles({
            'border-color': '#2196f3',
            'background-color': '#e3f2fd'
        });

        selectedIconName = iconElement.getAttribute('data-icon-name');
    }

    function parseExistingClasses(cssClass) {
        if (!cssClass) return { size: '', color: '', withTextEnd: false, custom: [] };

        var classes = cssClass.split(/\s+/);
        var result = { size: '', color: '', withTextEnd: false, custom: [] };

        classes.forEach(function(cls) {
            if ("svg-icon-selected" === cls) return;
            if (cls === 'icon' || cls === 'icon--with-text-end') {
                if (cls === 'icon--with-text-end') {
                    result.withTextEnd = true;
                }
            } else if (cls.startsWith('icon--')) {
                var value = cls.substring(6); // Remove 'icon--' prefix
                if (sizes.indexOf(value) !== -1) {
                    result.size = value;
                } else if (colors.indexOf(value) !== -1) {
                    result.color = value;
                } else {
                    result.custom.push(cls);
                }
            } else {
                result.custom.push(cls);
            }
        });

        return result;
    }

    function buildCssClasses(size, color, withTextEnd, customClasses) {
        var classes = ['icon'];

        if (size) classes.push('icon--' + size);
        if (color) classes.push('icon--' + color);
        if (withTextEnd) classes.push('icon--with-text-end');

        // Add custom classes, filtering out conflicting size/color classes
        if (customClasses) {
            var customArray = customClasses.split(/\s+/).filter(function(cls) {
                if (!cls) return false;
                if (cls === 'icon' || cls === 'icon--with-text-end') return false;
                if (cls.startsWith('icon--')) {
                    var value = cls.substring(6);
                    return sizes.indexOf(value) === -1 && colors.indexOf(value) === -1;
                }
                return true;
            });
            classes = classes.concat(customArray);
        }

        return classes.join(' ');
    }

    // Dialog elements for Icons tab
    var iconElements = [
        {
            type: 'hbox',
            padding: 0,
            widths: [ '49%', '2%', '49%' ],
            children: [
                {
                    type: 'select',
                    id: 'iconGroup',
                    label: editor.lang.webjetsvgicon.iconGroup,
                    items: groupItems,
                    'default': 'all',
                    onChange: function() {
                        filterIcons(this.getDialog());
                    }
                },
                {
                    type: 'html',
                    html: '&nbsp;'
                },
                {
                    type: 'text',
                    id: 'iconSearch',
                    label: editor.lang.webjetsvgicon.iconSearch,
                    onKeyUp: function() {
                        filterIcons(this.getDialog());
                    }
                }
            ]
        },
        {
            type: 'html',
            id: 'iconGrid',
            html: '<div style="min-height: ' + gridHeight + 'px;"></div>'
        }
    ];

    // Dialog elements for Advanced tab
    var advancedElements = [
        {
            type: 'select',
            id: 'iconSize',
            label: editor.lang.webjetsvgicon.iconSize,
            items: sizeItems
        },
        {
            type: 'select',
            id: 'iconColor',
            label: editor.lang.webjetsvgicon.iconColor,
            items: colorItems
        },
        {
            type: 'checkbox',
            id: 'withTextEnd',
            label: editor.lang.webjetsvgicon.withTextEnd
        },
        {
            type: 'text',
            id: 'customClass',
            label: editor.lang.webjetsvgicon.customClass
        }
    ];

    return {
        title: editor.lang.webjetsvgicon.title,
        minWidth: 800,
        minHeight: 500,
        contents: [
            {
                id: 'icons',
                label: editor.lang.webjetsvgicon.icons,
                elements: iconElements
            },
            {
                id: 'advanced',
                label: editor.lang.webjetsvgicon.advanced,
                elements: advancedElements
            }
        ],
        onShow: function() {
            var dialog = this;
            selectedIconName = '';

            // Clear search field
            this.setValueOf('icons', 'iconSearch', '');

            // Load sprite and build grid
            loadIconSprite(dialog);

            var svgElement = editor.document.find('.svg-icon-selected');

            if (svgElement && svgElement.$.length == 1) {
                // Parse CSS classes
                var cssClass = svgElement.$[0].getAttribute('class') || '';
                var parsedClasses = parseExistingClasses(cssClass);

                this.setValueOf('advanced', 'iconSize', parsedClasses.size);
                this.setValueOf('advanced', 'iconColor', parsedClasses.color);
                this.setValueOf('advanced', 'withTextEnd', parsedClasses.withTextEnd);
                this.setValueOf('advanced', 'customClass', parsedClasses.custom.join(' '));

                var href = svgElement.$[0].children[0].getAttribute("xlink:href") || '';
                if (href && href.indexOf('#') !== -1) {
                    selectedIconName = href.substring(href.indexOf('#') + 1);

                    // Set search to show the icon
                    this.setValueOf('icons', 'iconSearch', selectedIconName);
                }

                // Find appropriate group for the icon
                if (selectedIconName && iconToGroups[selectedIconName]) {
                    var groups = iconToGroups[selectedIconName];
                    if (groups.length > 0) {
                        this.setValueOf('icons', 'iconGroup', groups[0]);
                    }
                }

                //add class wj-selected to the selected icon
                var gridContainer = dialog.getContentElement('icons', 'iconGrid').getElement();
                var iconItems = gridContainer.find('.wj-icon-item');
                for (var i = 0; i < iconItems.count(); i++) {
                    var item = iconItems.getItem(i);
                    var iconName = item.getAttribute('data-icon-name');
                    if (iconName === selectedIconName) {
                        item.addClass('wj-selected');
                        item.setStyles({
                            'border-color': '#2196f3',
                            'background-color': '#e3f2fd'
                        });
                        break;
                    }
                }
            }
        },
        onOk: function() {
            var dialog = this;

            if (!selectedIconName) {
                alert(editor.lang.webjetsvgicon.noIconSelected);
                return false;
            }

            // Get values from Advanced tab
            var iconSize = dialog.getValueOf('advanced', 'iconSize');
            var iconColor = dialog.getValueOf('advanced', 'iconColor');
            var withTextEnd = dialog.getValueOf('advanced', 'withTextEnd');
            var customClass = dialog.getValueOf('advanced', 'customClass');

            var cssClasses = buildCssClasses(iconSize, iconColor, withTextEnd, customClass);

            var svgHtml = '<svg class="' + CKEDITOR.tools.htmlEncode(cssClasses) + '" aria-hidden="true">';
            svgHtml += '<use xlink:href="' + CKEDITOR.tools.htmlEncode(spritePath + '#' + selectedIconName) + '"></use>';
            svgHtml += '</svg>';

            var svgElement = editor.document.find('.svg-icon-selected');
            if (svgElement && svgElement.$.length == 1) {
                svgElement = svgElement.$[0];
                svgElement.outerHTML = svgHtml;
            } else {
                var newElement = CKEDITOR.dom.element.createFromHtml(svgHtml);
                editor.insertElement(newElement);
            }
        }
    };
});
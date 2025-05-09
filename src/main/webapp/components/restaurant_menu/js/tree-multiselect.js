/* jQuery Tree Multiselect v2.1.3 | (c) Patrick Tsai | MIT Licensed */ ! function a(b, c, d) {
    function e(g, h) {
        if (!c[g]) {
            if (!b[g]) {
                var i = "function" == typeof require && require;
                if (!h && i) return i(g, !0);
                if (f) return f(g, !0);
                var j = new Error("Cannot find module '" + g + "'");
                throw j.code = "MODULE_NOT_FOUND", j
            }
            var k = c[g] = {
                exports: {}
            };
            b[g][0].call(k.exports, function(a) {
                var c = b[g][1][a];
                return e(c ? c : a)
            }, k, k.exports, a, b, c, d)
        }
        return c[g].exports
    }
    for (var f = "function" == typeof require && require, g = 0; g < d.length; g++) e(d[g]);
    return e
}({
    1: [function(a, b, c) {
        "use strict";

        function d(a) {
            var b = {
                allowBatchSelection: !0,
                collapsible: !0,
                enableSelectAll: !1,
                selectAllText: "Select All",
                unselectAllText: "Unselect All",
                freeze: !1,
                hideSidePanel: !1,
                onChange: null,
                onlyBatchSelection: !1,
                searchable: !1,
                searchParams: ["value", "text", "description", "section"],
                sectionDelimiter: "/",
                showSectionOnSelected: !0,
                sortable: !1,
                startCollapsed: !1
            };
            return jQuery.extend({}, b, a)
        }
        var e = a("./tree"),
            f = 0,
            g = function(a) {
                var b = this,
                    c = d(a);
                return this.each(function() {
                    var a = jQuery(b);
                    a.attr("multiple", "").css("display", "none");
                    var d = new e(f, a, c);
                    d.initialize(), ++f
                }), this
            };
        b.exports = g
    }, {
        "./tree": 5
    }],
    2: [function(a, b, c) {
        "use strict";
        b.exports = function(a, b, c, d, e, f) {
            this.id = a, this.value = b, this.text = c, this.description = d, this.initialIndex = e, this.section = f
        }
    }, {}],
    3: [function(a, b, c) {
        "use strict";

        function d(a, b, c, d) {
            this.options = a, this.index = {}, this.selectionNodeHash = b, this.selectionNodeHashKeys = Object.keys(b), this.sectionNodeHash = c, this.sectionNodeHashKeys = Object.keys(c), this.setSearchParams(d), this.buildIndex()
        }

        function e(a) {
            if (f.assert(a), a.length < g) return [a];
            for (var b = [], c = 0; c < a.length - g + 1; ++c) b.push(a.substring(c, c + g));
            return b
        }
        var f = a("./utility"),
            g = 3;
        d.prototype.setSearchParams = function(a) {
            f.assert(Array.isArray(a));
            var b = {
                value: !0,
                text: !0,
                description: !0,
                section: !0
            };
            this.searchParams = [];
            for (var c = 0; c < a.length; ++c) b[a[c]] && this.searchParams.push(a[c])
        }, d.prototype.buildIndex = function() {
            var a = this;
            this.options.forEach(function(b) {
                var c = [];
                a.searchParams.forEach(function(a) {
                    c.push(b[a])
                });
                var d = f.array.removeFalseyExceptZero(c).map(function(a) {
                    return a.toLowerCase()
                });
                d.forEach(function(c) {
                    var d = c.split(" ");
                    d.forEach(function(c) {
                        a._addToIndex(c, b.id)
                    })
                })
            })
        }, d.prototype._addToIndex = function(a, b) {
            for (var c = 1; c <= g; ++c)
                for (var d = 0; d < a.length - c + 1; ++d) {
                    var e = a.substring(d, d + c);
                    this.index[e] || (this.index[e] = []);
                    var f = this.index[e].length;
                    0 !== f && this.index[e][f - 1] === b || this.index[e].push(b)
                }
        }, d.prototype.search = function(a) {
            var b = this;
            if (!a) return this.selectionNodeHashKeys.forEach(function(a) {
                b.selectionNodeHash[a].style.display = ""
            }), void this.sectionNodeHashKeys.forEach(function(a) {
                b.sectionNodeHash[a].style.display = ""
            });
            a = a.toLowerCase();
            var c = a.split(" "),
                d = [];
            c.forEach(function(a) {
                var c = e(a);
                c.forEach(function(a) {
                    d.push(b.index[a] || [])
                })
            });
            var g = f.array.intersectMany(d);
            this._handleNodeVisbilities(g)
        }, d.prototype._handleNodeVisbilities = function(a) {
            var b = this,
                c = {},
                d = {};
            a.forEach(function(a) {
                c[a] = !0;
                var e = b.selectionNodeHash[a];
                for (e.style.display = "", e = e.parentNode; !e.className.match(/tree-multiselect/);) {
                    if (e.className.match(/section/)) {
                        var g = f.getKey(e);
                        if (f.assert(g || 0 === g), d[g]) break;
                        d[g] = !0, e.style.display = ""
                    }
                    e = e.parentNode
                }
            }), this.selectionNodeHashKeys.forEach(function(a) {
                c[a] || (b.selectionNodeHash[a].style.display = "none")
            }), this.sectionNodeHashKeys.forEach(function(a) {
                d[a] || (b.sectionNodeHash[a].style.display = "none")
            })
        }, b.exports = d
    }, {
        "./utility": 9
    }],
    4: [function(a, b, c) {
        "use strict";
        ! function(b) {
            b.fn.treeMultiselect = a("./main")
        }(jQuery)
    }, {
        "./main": 1
    }],
    5: [function(a, b, c) {
        "use strict";

        function d(a, b, c) {
            this.id = a, this.$originalSelect = b;
            var d = new g(b, c.hideSidePanel);
            this.$selectionContainer = d.$selectionContainer, this.$selectedContainer = d.$selectedContainer, this.params = c, this.selectOptions = [], this.selectNodes = {}, this.sectionNodes = {}, this.selectedNodes = {}, this.selectedKeys = [], this.keysToAdd = [], this.keysToRemove = []
        }
        var e = a("./option"),
            f = a("./search"),
            g = a("./ui-builder"),
            h = a("./utility");
        d.prototype.initialize = function() {
            var a = this.generateSelections();
            if (this.generateHtmlFromData(a, this.$selectionContainer[0]), this.popupDescriptionHover(), this.params.allowBatchSelection && this.handleSectionCheckboxMarkings(), this.params.collapsible && this.addCollapsibility(), this.params.searchable || this.params.enableSelectAll) {
                var b = h.dom.createNode("div", {
                    class: "auxiliary clearfix"
                });
                this.$selectionContainer.prepend(b, this.$selectionContainer.firstChild), this.params.searchable && this.createSearchBar(b), this.params.enableSelectAll && this.createSelectAllButtons(b)
            }
            this.armRemoveSelectedOnClick(), this.updateSelectedAndOnChange(), this.render(!0)
        }, d.prototype.generateSelections = function() {
            var a = [
                    [], {}
                ],
                b = this.params.sectionDelimiter,
                c = this,
                d = 0,
                f = [];
            return this.$originalSelect.find("> option").each(function() {
                var g = this;
                g.setAttribute("data-key", d);
                var h = g.getAttribute("data-section"),
                    i = h && h.length > 0 ? h.split(b) : [],
                    j = g.value,
                    k = g.text,
                    l = g.getAttribute("data-description"),
                    m = parseInt(g.getAttribute("data-index")),
                    n = new e(d, j, k, l, m, h);
                m ? c.keysToAdd[m] = d : g.hasAttribute("selected") && f.push(d), c.selectOptions[d] = n;
                for (var o = a, p = 0; p < i.length; ++p) o[1][i[p]] || (o[1][i[p]] = [
                    [], {}
                ]), o = o[1][i[p]];
                o[0].push(n), ++d
            }), this.keysToAdd = h.array.uniq(h.array.removeFalseyExceptZero(this.keysToAdd).concat(f)), a
        }, d.prototype.generateHtmlFromData = function(a, b, c) {
            c = c || 0;
            for (var d = 0, e = 0; e < a[0].length; ++e) {
                var f = a[0][e],
                    g = h.dom.createSelection(f, this.id, !this.params.onlyBatchSelection, this.params.freeze);
                this.selectNodes[f.id] = g, b.appendChild(g)
            }
            for (var i = Object.keys(a[1]), j = 0; j < i.length; ++j) {
                var k = i[j],
                    l = d + c,
                    m = h.dom.createSection(k, l, this.params.onlyBatchSelection || this.params.allowBatchSelection, this.params.freeze);
                this.sectionNodes[l] = m, ++d, b.appendChild(m), d += this.generateHtmlFromData(a[1][i[j]], m, c + d)
            }
            return d
        }, d.prototype.popupDescriptionHover = function() {
            this.$selectionContainer.on("mouseenter", "div.item > span.description", function() {
                var a = jQuery(this).parent(),
                    b = a.attr("data-description"),
                    c = document.createElement("div");
                c.className = "temp-description-popup", c.innerHTML = b, c.style.position = "absolute", a.append(c)
            }), this.$selectionContainer.on("mouseleave", "div.item > span.description", function() {
                var a = jQuery(this).parent();
                a.find("div.temp-description-popup").remove()
            })
        }, d.prototype.handleSectionCheckboxMarkings = function() {
            var a = this;
            this.$selectionContainer.on("change", "input.section[type=checkbox]", function() {
                var b = jQuery(this).closest("div.section"),
                    c = b.find("div.item"),
                    d = [];
                c.each(function(a, b) {
                    d.push(h.getKey(b))
                }), this.checked ? a.keysToAdd = h.array.uniq(a.keysToAdd.concat(d)) : a.keysToRemove = h.array.uniq(a.keysToRemove.concat(d)), a.render()
            })
        }, d.prototype.redrawSectionCheckboxes = function(a) {
            a = a || this.$selectionContainer;
            var b = [!0, !0],
                c = a.find("> div.item > input[type=checkbox]");
            c.each(function() {
                this.checked ? b[1] = !1 : b[0] = !1
            });
            var d = this,
                e = a.find("> div.section");
            e.each(function() {
                var a = d.redrawSectionCheckboxes(jQuery(this));
                b[0] = b[0] && a[0], b[1] = b[1] && a[1]
            });
            var f = a.find("> div.title > input[type=checkbox]");
            return f.length && (f = f[0], b[0] ? (f.checked = !0, f.indeterminate = !1) : b[1] ? (f.checked = !1, f.indeterminate = !1) : (f.checked = !1, f.indeterminate = !0)), b
        }, d.prototype.addCollapsibility = function() {
            var a = "-",
                b = "+",
                c = "div.title",
                d = this.$selectionContainer.find(c),
                e = h.dom.createNode("span", {
                    class: "collapse-section"
                });
            this.params.startCollapsed ? (jQuery(e).text(b), d.siblings().toggle()) : jQuery(e).text(a), d.prepend(e), this.$selectionContainer.on("click", c, function(c) {
                if ("INPUT" != c.target.nodeName) {
                    var d = jQuery(this).find("> span.collapse-section"),
                        e = d.text();
                    d.text(e == a ? b : a);
                    var f = d.parent();
                    f.siblings().toggle()
                }
            })
        }, d.prototype.createSearchBar = function(a) {
            var b = new f(this.selectOptions, this.selectNodes, this.sectionNodes, this.params.searchParams),
                c = h.dom.createNode("input", {
                    class: "search",
                    placeholder: "Hľadať..."
                });
            a.appendChild(c), this.$selectionContainer.on("input", "input.search", function() {
                var a = this.value;
                b.search(a)
            })
        }, d.prototype.createSelectAllButtons = function(a) {
            var b = h.dom.createNode("span", {
                    class: "select-all",
                    text: this.params.selectAllText
                }),
                c = h.dom.createNode("span", {
                    class: "unselect-all",
                    text: this.params.unselectAllText
                }),
                d = h.dom.createNode("div", {
                    class: "select-all-container"
                });
            d.appendChild(b), d.appendChild(c), a.appendChild(d);
            var e = this;
            this.$selectionContainer.on("click", "span.select-all", function() {
                for (var a = 0; a < e.selectOptions.length; ++a) e.keysToAdd.push(a);
                e.keysToAdd = h.array.uniq(e.keysToAdd), e.render()
            }), this.$selectionContainer.on("click", "span.unselect-all", function() {
                e.keysToRemove = h.array.uniq(e.keysToRemove.concat(e.selectedKeys)), e.render()
            })
        }, d.prototype.armRemoveSelectedOnClick = function() {
            var a = this;
            this.$selectedContainer.on("click", "span.remove-selected", function() {
                var b = this.parentNode,
                    c = h.getKey(b);
                a.keysToRemove.push(c), a.render()
            })
        }, d.prototype.updateSelectedAndOnChange = function() {
            var a = this;
            if (this.$selectionContainer.on("change", "input.option[type=checkbox]", function() {
                var b = this,
                    c = b.parentNode,
                    d = h.getKey(c);
                h.assert(d || 0 === d), b.checked ? a.keysToAdd.push(d) : a.keysToRemove.push(d), a.render()
            }), this.params.sortable && !this.params.freeze) {
                var b = null,
                    c = null;
                this.$selectedContainer.sortable({
                    start: function(a, c) {
                        b = c.item.index()
                    },
                    stop: function(d, e) {
                        c = e.item.index(), b !== c && (h.array.moveEl(a.selectedKeys, b, c), a.render())
                    }
                })
            }
        }, d.prototype.render = function(a) {
            var b = this;
            this.keysToAdd = h.array.subtract(this.keysToAdd, this.selectedKeys), this.keysToRemove = h.array.intersect(this.keysToRemove, this.selectedKeys);
            for (var c = 0; c < this.keysToRemove.length; ++c) {
                var d = this.selectedNodes[this.keysToRemove[c]];
                d && (d.remove(), this.selectedNodes[this.keysToRemove[c]] = null);
                var e = this.selectNodes[this.keysToRemove[c]];
                e.getElementsByTagName("INPUT")[0].checked = !1
            }
            this.selectedKeys = h.array.subtract(this.selectedKeys, this.keysToRemove);
            for (var f = 0; f < this.keysToAdd.length; ++f) {
                var g = this.keysToAdd[f],
                    i = this.selectOptions[g];
                this.selectedKeys.push(g);
                var j = h.dom.createSelected(i, this.params.freeze, this.params.showSectionOnSelected);
                this.selectedNodes[i.id] = j, this.$selectedContainer.append(j), this.selectNodes[this.keysToAdd[f]].getElementsByTagName("INPUT")[0].checked = !0
            }
            this.selectedKeys = h.array.uniq(this.selectedKeys.concat(this.keysToAdd)), this.redrawSectionCheckboxes();
            for (var k = {}, l = {}, m = 0; m < this.selectedKeys.length; ++m) {
                var n = this.selectOptions[this.selectedKeys[m]].value;
                k[this.selectedKeys[m]] = !0, l[n] = m
            }
            var o = this.$originalSelect.find("option").toArray();
            if (o.sort(function(a, b) {
                var c = l[a.value] || 0,
                    d = l[b.value] || 0;
                return c - d
            }), this.$originalSelect.html(o), this.$originalSelect.find("option").each(function(a, b) {
                k[h.getKey(b)] ? this.selected = !0 : this.selected = !1
            }), this.$originalSelect.change(), !a && this.params.onChange) {
                var p = this.selectedKeys.map(function(a) {
                        return b.selectOptions[a]
                    }),
                    q = this.keysToAdd.map(function(a) {
                        return b.selectOptions[a]
                    }),
                    r = this.keysToRemove.map(function(a) {
                        return b.selectOptions[a]
                    });
                this.params.onChange(p, q, r)
            }
            this.keysToRemove = [], this.keysToAdd = []
        }, b.exports = d
    }, {
        "./option": 2,
        "./search": 3,
        "./ui-builder": 6,
        "./utility": 9
    }],
    6: [function(a, b, c) {
        "use strict";
        b.exports = function(a, b) {
            var c = jQuery('<div class="tree-multiselect"></div>'),
                d = jQuery('<div class="selections col-sm-12 col-md-6"></div>');
            b && d.addClass("no-border"), c.append(d);
            var e = jQuery('<div class="selected col-sm-12 col-md-6"><span>Zoznam vybraných jedál</span></div>');
            b || c.append(e), a.after(c), this.$tree = c, this.$selectionContainer = d, this.$selectedContainer = e
        }
    }, {}],
    7: [function(a, b, c) {
        "use strict";

        function d(a, b) {
            for (var c = {}, d = [], e = 0; e < b.length; ++e) c[b[e]] = !0;
            for (var f = 0; f < a.length; ++f) c[a[f]] || d.push(a[f]);
            return d
        }

        function e(a) {
            for (var b = {}, c = [], d = 0; d < a.length; ++d) b[a[d]] || (b[a[d]] = !0, c.push(a[d]));
            return c
        }

        function f(a) {
            for (var b = [], c = 0; c < a.length; ++c)(a[c] || 0 === a[c]) && b.push(a[c]);
            return b
        }

        function g(a, b, c) {
            var d = a[b];
            a.splice(b, 1), a.splice(c, 0, d)
        }

        function h(a, b) {
            for (var c = [], d = {}, e = 0; e < b.length; ++e) d[b[e]] = !0;
            for (var f = 0; f < a.length; ++f) d[a[f]] && c.push(a[f]);
            return c
        }

        function i(a) {
            var b = [],
                c = [];
            a.forEach(function(a) {
                b.push(0), c.push(a.length - 1)
            });
            for (var d = []; b.length > 0 && b[0] <= c[0]; ++b[0]) {
                for (var e = !1, f = 1; f < a.length; ++f) {
                    for (; a[f][b[f]] < a[0][b[0]] && b[f] <= c[f];) ++b[f];
                    if (b[f] > c[f]) {
                        e = !0;
                        break
                    }
                }
                if (e) break;
                for (var g = !0, h = 1; h < a.length; ++h)
                    if (a[0][b[0]] !== a[h][b[h]]) {
                        g = !1;
                        break
                    }
                g && d.push(a[0][b[0]])
            }
            return d
        }
        b.exports = {
            subtract: d,
            uniq: e,
            removeFalseyExceptZero: f,
            moveEl: g,
            intersect: h,
            intersectMany: i
        }
    }, {}],
    8: [function(a, b, c) {
        "use strict";

        function d(a, b) {
            var c = document.createElement(a);
            if (b) {
                for (var d in b) b.hasOwnProperty(d) && "text" !== d && c.setAttribute(d, b[d]);
                b.text && (c.textContent = b.text)
            }
            return c
        }

        function e(a, b, c, e) {
            var f = {
                    class: "item",
                    "data-key": a.id,
                    "data-value": a.value
                },
                g = !!a.description;
            g && (f["data-description"] = a.description), a.initialIndex && (f["data-index"] = a.initialIndex);
            var h = d("div", f);
            if (g) {
                var i = d("span", {
                    class: "description",
                    text: "?"
                });
                h.appendChild(i)
            }
            if (c) {
                var j = "treemultiselect-" + b + "-" + a.id,
                    k = {
                        class: "option",
                        type: "checkbox",
                        id: j
                    };
                e && (k.disabled = !0);
                var l = d("input", k);
                h.insertBefore(l, h.firstChild);
                var m = {
                        for: j,
                        text: a.text || a.value
                    },
                    n = d("label", m);
                h.appendChild(n);

                // var p = {
                //     class: "checkmark",
                //     text: ''
                //     },
                //     b = d("span", p);
                // h.appendChild(b)
            } else h.innerText = a.text || a.value;
            return h
        }

        function f(a, b, c) {
            var e = d("div", {
                class: "item",
                "data-key": a.id,
                "data-value": a.value,
                text: a.text
            });
            if (!b) {
                var f = d("span", {
                    class: "remove-selected",
                    text: "×"
                });
                e.insertBefore(f, e.firstChild)
            }
            if (c) {
                var g = d("span", {
                    class: "section-name",
                    text: a.section
                });
                e.appendChild(g)
            }
            return e
        }

        function g(a, b, c, e) {
            var f = d("div", {
                    class: "section",
                    "data-key": b
                }),
                g = d("div", {
                    class: "title",
                    text: a
                });
            if (c) {
                var h = {
                    class: "section",
                    type: "checkbox"
                };
                e && (h.disabled = !0);
                var i = d("input", h);
                g.insertBefore(i, g.firstChild)
            }
            return f.appendChild(g), f
        }
        b.exports = {
            createNode: d,
            createSelection: e,
            createSelected: f,
            createSection: g
        }
    }, {}],
    9: [function(a, b, c) {
        "use strict";

        function d(a, b) {
            if (!a) throw new Error(b || "Assertion failed")
        }

        function e(a) {
            return d(a), parseInt(a.getAttribute("data-key"))
        }
        var f = a("./array"),
            g = a("./dom");
        b.exports = {
            assert: d,
            getKey: e,
            array: f,
            dom: g
        }
    }, {
        "./array": 7,
        "./dom": 8
    }]
}, {}, [4]);

if (!('remove' in Element.prototype)) {
    Element.prototype.remove = function() {
        if (this.parentNode) {
            this.parentNode.removeChild(this);
        }
    };
}
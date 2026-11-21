import {diffArrays} from 'diff';
import {node as element} from './dashboard/widget-utils';

/** Returns the line numbers to mark in each version, counting insertions and removals independently. */
export function changedSourceLines(live, work) {
    const marks = {live: new Set(), work: new Set()};
    const lines = value => value == null ? [] : String(value).split(/\r\n?|\n/);
    const changes = diffArrays(lines(live), lines(work), {timeout: 50});
    let liveLine = 0, workLine = 0;
    for (const change of changes || []) {
        for (let i = 0; i < change.count; i++) {
            if (!change.added) liveLine++;
            if (!change.removed) workLine++;
            if (change.added) marks.work.add(workLine);
            if (change.removed) marks.live.add(liveLine);
        }
    }
    return marks;
}

/** Renders stored markup as text. The readable preview is built only when the user opens it. */
export function htmlSource(value, marks, version, previewLabel) {
    const source = String(value);
    const block = element('div', 'pg-branch-merge__source');
    const code = element('pre', 'pg-branch-merge__code');
    const lines = source.split(/\r\n?|\n/);
    for (const [index, line] of lines.entries()) {
        const row = element('span', 'pg-branch-merge__line');
        const number = element('span', 'pg-branch-merge__line-number', index + 1);
        number.setAttribute('aria-hidden', 'true');
        if (marks?.has(index + 1)) {
            row.classList.add(version === 'work' ? 'is-added' : 'is-removed');
            number.append(version === 'work' ? ' +' : ' −');
        }
        const text = element('code');
        let offset = 0;
        for (const tag of line.matchAll(/<\/?[a-z][\w:-]*/gi)) {
            text.append(line.slice(offset, tag.index), element('span', 'pg-branch-merge__tag', tag[0]));
            offset = tag.index + tag[0].length;
        }
        text.append(line.slice(offset));
        row.append(number, text);
        code.append(row);
    }
    const preview = element('details');
    preview.append(element('summary', 'small', previewLabel));
    preview.addEventListener('toggle', () => {
        if (!preview.open || preview.childElementCount > 1) return;
        const doc = new DOMParser().parseFromString(source, 'text/html');
        doc.querySelectorAll('script,style,template').forEach(node => node.remove());
        doc.querySelectorAll('br,p,div,li,h1,h2,h3,h4,h5,h6').forEach(node => node.append(document.createTextNode('\n')));
        preview.append(element('pre', 'pg-branch-merge__text', doc.body.textContent.trim()));
    });
    block.append(preview, code);
    return block;
}

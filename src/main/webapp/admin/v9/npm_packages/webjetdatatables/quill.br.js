import Quill, { Delta } from 'quill';
import Embed from 'quill/blots/embed';

//inspired by: https://github.com/slab/quill/issues/4248#issuecomment-2634898120

export function shiftEnterHandler(quill, range) {
	const currentLeaf = quill.getLeaf(range.index)[0];
	const nextLeaf = quill.getLeaf(range.index + 1)[0];
	quill.insertEmbed(range.index, "sbreak", true, Quill.sources.USER);
	// Insert a second break if:
	// At the end of the editor, OR next leaf has a different parent (<p>)
	if (nextLeaf === null || currentLeaf.parent !== nextLeaf.parent) {
		quill.insertEmbed(range.index, "sbreak", true, Quill.sources.USER);
	}
	// Now that we've inserted a line break, move the cursor forward
	quill.setSelection(range.index + 1, Quill.sources.SILENT);
}

export function lineBreakMatcher() {
	const newDelta = new Delta();
	newDelta.insert({'sbreak': ''});
	return newDelta;
}

export class SoftLineBreakBlot extends Embed {
	static blotName = 'sbreak';
	static tagName = 'br';
}
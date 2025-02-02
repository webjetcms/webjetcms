import 'tui-color-picker/dist/tui-color-picker.css';
import '@webjetcms/tui-image-editor/dist/tui-image-editor.css';
import Editor from '@webjetcms/tui-image-editor';

//import 'tui-image-editor/node_modules/tui-color-picker/dist/tui-color-picker.css';
//import 'tui-image-editor/apps/image-editor/dist/tui-image-editor.css';
//import Editor from 'tui-image-editor/apps/image-editor/dist/tui-image-editor';

export class ImageEditor extends Editor {
    //preklad: <iwcm:text key="components.gallery.tui-image-editor.Remove_White"/>
    //konstanta: <%=Constants.getInt("rootGroupId")%>

    #editorState = 0;
    #file = null;

    /* chunk config */
    #bytesPerChunk = 1310720;
    #blobSizeBytes = null;
    #start = 0;
    #end = this.#bytesPerChunk;
    #chunkRank = 0;
    #totalChunkCount = null;
    #convertSize = null;
    #progress = 0;

    constructor(...args) {
        super(...args);
        $('.tui-image-editor-header').remove();
        $('.tui-image-editor-main').css({'top': '0px'});
        this.on('undoStackChanged', length => {
            this.#editorState = length;
        });
    }

    /**
     * Metoda na ulozenie upraveneho obrazka
     * * Url parameter je required
     * ? Mala by byt tato metoda medzi public API ?
     * @param {string} url url pre ulozenie obrazka
     */
    onPostEditedImage(url) {
        if (url == undefined || typeof url != 'string') throw('function onPostEditedImage: first parameter "url" have to be string and is required');
        // zistim ci boli robene zmeny v editore na obrazku
        if (this.#editorState === 0) {
            //alert('nespravili ste žiadne zmeny v editore obrázkov');
            return;
        }

        //console.log("Saving image, url=", url);

        const imageBlob = this.getImageBlob();
        const formatedBlobData = new FormData();

        this.#blobSizeBytes = Number(imageBlob.size);
        this.#totalChunkCount = `${!Number.isInteger(imageBlob.size / this.#bytesPerChunk) ? Math.floor(imageBlob.size / this.#bytesPerChunk) + 1 : Math.floor(imageBlob.size / this.#bytesPerChunk)}`;
        this.#convertSize = 100 / this.#totalChunkCount;
        formatedBlobData.append('uploadType', 'image');
        formatedBlobData.append('file', imageBlob);
        formatedBlobData.append('destinationFolder', adminUpload.getDestinationFolder());
        formatedBlobData.append('writeDirectlyToDestination', true);
        formatedBlobData.append('overwriteMode', 'overwrite');
        formatedBlobData.append('name', this.getImageName());
        formatedBlobData.append('encoding', "base64");

        this.#file = new File([imageBlob], this.getImageName(), { type: 'image/jpeg' });
        this.#file.upload = { progess: 0 };
        $( document ).trigger('initAddedFileFromImageOutside', [this.#file]);
        this.postAgain(url, formatedBlobData, imageBlob);
    }

    /**
     * Metoda ktora sa spusta az kym sa neodosle cely obrazok
     * @param {string} url url pre post
     * @param {FormData} form formatovany blob obrazok
     * @param {object} imageBlob blob object
     */
    postAgain(url, form, imageBlob) {
        const chunk = imageBlob.slice(this.#start, this.#end);
        form.set('dztotalfilesize', imageBlob.size);
        form.set('dzchunksize', `${this.#bytesPerChunk}`);
        form.set('dztotalchunkcount', `${!Number.isInteger(imageBlob.size / this.#bytesPerChunk) ? Math.floor(imageBlob.size / this.#bytesPerChunk) + 1 : Math.floor(imageBlob.size / this.#bytesPerChunk)}`);
        form.set('dzchunkindex', this.#chunkRank)
        form.set('file', chunk);
        this.onPostImage(url, form).then((response, status) => {
            if (status == 'success') {
                this.#start = this.#end;
                this.#end = this.#start + this.#bytesPerChunk;
                this.#chunkRank += 1;
                this.#progress += this.#convertSize;
                this.#file.upload = { progress: this.#progress }
                //console.log("progress 2=", this.#file.upload);
                $( document ).trigger('initUploadProgressFromOutside', this.#file);
                if (this.#start < this.#blobSizeBytes) {
                    this.postAgain(url, form, imageBlob)
                } else {
                    if (typeof galleryTable != "undefined") galleryTable.ajax.reload();
                    WJ.dispatchEvent("WJ.imageEditor.upload.success", {});
                }
            } else {
                WJ.notifyError('Nepodarilo sa to', 'Skúste to neskor');
                $( document ).trigger('initErrorMessageFromOutside', [this.#file, response]);
            }
        })
    }

    /**
     * Post pre chunked uploading
     * @param {FormData} form FormData pre chunked upload
     * @param {string} url url na post
     */
    onPostImage(url, form) {
        return $.ajax({
            type: 'POST',
            url: url,
            data: form,
            error: err => {
                console.log(err);
            },
            processData: false,
            contentType: false,
        });
    }

    /* Metoda na formatovanie upraveneho obrazka z formatu base64 na Blob object */
    getImageBlob() {
        const b64Image = this.toDataURL();
        const byteCharacters = b64Image.substring("data:image/png;base64,".length) ;  //jeeff: uz to je base64 zbytocne by sme to spravili duplicitne btoa(b64Image);
        const byteNumbers = new Array(byteCharacters.length);
        for (let x = 0; x < byteCharacters.length; x++) {
            byteNumbers[x] = byteCharacters.charCodeAt(x);
        }
        const byteArray = new Uint8Array(byteNumbers);
        const blob = new Blob([byteArray], { type: 'image/png' });
        return blob;
    }

    resetData() {
        this.#editorState = 0;
        this.#blobSizeBytes = null;
        this.#start = 0;
        this.#end = this.#bytesPerChunk;
        this.#chunkRank = 0;
        this.#totalChunkCount = null;
        this.#convertSize = null;
        this.#progress = 0;

        this.clearUndoStack();
    }
}